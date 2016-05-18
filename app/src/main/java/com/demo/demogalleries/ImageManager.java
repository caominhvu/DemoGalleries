package com.demo.demogalleries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.demo.demogalleries.utils.IOUtils;
import com.demo.demogalleries.utils.Utils;

/**
 * @author Cao Minh Vu
 * 
 * @category This class handle image for whole application
 */
public class ImageManager {
	private static final String TAG = "ImageManager";
	private int mHDCacheSize;
	private File mHDCachePath;

	private Map<String, String> mHDCache;
	private ConcurrentHashMap<String, SoftReference<Bitmap>> mSoftCache;

	private ContextWrapper mContext;

	private volatile static ImageManager uniqueInstance;

	/**
	 * @param context
	 * @return Instance of ImageManager, using Singleton pattern
	 */
	public static ImageManager getInstance(Context context) {
		if (uniqueInstance == null) {
			synchronized (ImageManager.class) {
				if (uniqueInstance == null) {
					uniqueInstance = new ImageManager(context);
				}
			}
		}
		return uniqueInstance;
	}

	public ImageManager(Context base) {
		mContext = new ContextWrapper(base);
		mHDCachePath = mContext.getCacheDir();
		mHDCacheSize = 20;

		mHDCache = Collections
				.synchronizedMap(new LinkedHashMap<String, String>(
						mHDCacheSize, 0.75f, true) {
					private static final long serialVersionUID = 1L;

					@Override
					protected boolean removeEldestEntry(
							Entry<String, String> eldest) {
						File f = new File(eldest.getValue());
						f.delete();
						return super.removeEldestEntry(eldest);
					}
				});

		mSoftCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
				mHDCacheSize / 2);
	}

	/**
	 * @param url
	 * @return bitmap get from soft cache or hard cache
	 */
	private Bitmap getBMFromCache(String url) {
		// Get from Soft cache
		SoftReference<Bitmap> bmpRef = mSoftCache.get(url);
		if (bmpRef != null) {
			Bitmap bitmap = bmpRef.get();
			if (bitmap != null) {
				return bitmap;
			} else {
				// Garbage collector deleted this for more memory
				mSoftCache.remove(url);
			}
		}

		// Get from hard cache
		if (mHDCache.containsKey(url)) {
			File file = new File(mHDCache.get(url));
			if (file.exists()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				try {
					options.outWidth = -1;
					options.outHeight = -1;
					options.inSampleSize = Utils.computeSampleSize(
							new FileInputStream(file), options.outWidth,
							options.outHeight);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return null;
				}

				Bitmap b = null;
				try {
					b = BitmapFactory.decodeFile(file.getAbsolutePath(),
							options);
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				}
				if (b != null) {
					// Using this one, so re-new it
					// So that system wont delete it as eldest
					mHDCache.remove(url);
					mHDCache.put(url, file.getAbsolutePath());
					mSoftCache.put(url, new SoftReference<Bitmap>(b));
					return b;
				}
			} else {
				// System deleted this file
				mHDCache.remove(url);
			}
		}
		return null;
	}

	private boolean isInCache(String url) {
		return mSoftCache.contains(url) || mHDCache.containsKey(url);
	}

	private void addToSoftCache(String url, Bitmap bmp) {
		mSoftCache.put(url, new SoftReference<Bitmap>(bmp));
	}

	private void addToHDCache(String url, InputStream input) {
		File file = new File(mHDCachePath, url.substring(url.lastIndexOf('/')));
		OutputStream output = null;
		try {
			output = new FileOutputStream(file);
			IOUtils.copy(input, output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				if (output != null)
					output.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

		}
		mHDCache.put(url, file.getAbsolutePath());
	}

	public void fetch(ImageView imgView, ProgressBar progresBar, String... urls) {
		if (urls.length <= 0) {
			Log.i(TAG, "No URL to download");
			return;
		}
		if (isInCache(urls[0])) {
			Bitmap bm;
			bm = getBMFromCache(urls[0]);
			if (bm != null) {
				imgView.setVisibility(View.VISIBLE);
				imgView.setImageBitmap(bm);
			}
		} else {
			if (mPool.containsKey(imgView)) {
				if (!urls[0].equals(imgView.getTag())) {
					mPool.get(imgView).cancel(true);
					imgView.setTag(urls[0]);
					new ImageDownloader(imgView, progresBar).execute();
				}
			} else {
				imgView.setTag(urls[0]);
				ImageDownloader imgDownloader = new ImageDownloader(imgView,
						progresBar);
				mPool.put(imgView, imgDownloader);
				imgDownloader.execute();
			}
		}
		
		new Thread(new DummyRunable(urls)).start();
	}

	Map<ImageView, ImageDownloader> mPool = new LinkedHashMap<ImageView, ImageDownloader>();

	// Algorithms to download image
	private class DummyRunable implements Runnable {

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		/**
		 * 
		 */
		private String[] mUrls;
		public DummyRunable(String...Urls) {
			mUrls = Urls;
		}
		
		@Override
		public void run() {
			for (int i = 1; i < mUrls.length; i++) {
				if(!isInCache(mUrls[i]))
					loadImgFromUrl(mUrls[i]);
			}
		}
	}
	
	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int bytes = read();
					if (bytes < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	private Bitmap loadImgFromUrl(String url) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpUriRequest request = new HttpGet(url);
		Bitmap b = null;
		try {
			Log.i(TAG, request.getURI().toString());
			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine() != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					Log.e(TAG, "HttpClient Executive Error: " + statusCode);
					return null;
				}
			}
			BufferedHttpEntity entity = new BufferedHttpEntity(
					response.getEntity());
			if (entity != null) {
				try {
					b = BitmapFactory.decodeStream(new FlushedInputStream(
							entity.getContent()));
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				} finally {
					addToHDCache(url, entity.getContent());
					entity.consumeContent();
				}
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return b;
	}

	/**
	 * @author Cao Minh Vu
	 * 
	 */
	private class ImageDownloader extends AsyncTask<Void, Void, Bitmap> {
		private WeakReference<ImageView> mImgViewRef;
		private WeakReference<ProgressBar> mProgressBarRef = null;
		private String mUrl;

		public ImageDownloader(ImageView imgView, ProgressBar progress) {
			mUrl = (String) imgView.getTag();
			mImgViewRef = new WeakReference<ImageView>(imgView);
			if(progress != null) {
				mProgressBarRef = new WeakReference<ProgressBar>(progress);
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if(mProgressBarRef != null) {
				mProgressBarRef.get().setVisibility(View.VISIBLE);
			}
			mImgViewRef.get().setVisibility(View.INVISIBLE);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Bitmap doInBackground(Void... params) {
			return loadImgFromUrl((String) mImgViewRef.get().getTag());
		}

		@Override
		protected void onPostExecute(Bitmap bm) {
			super.onPostExecute(bm);
			if (isCancelled()) {
				bm = null;
			}
			if (mUrl.equals(mImgViewRef.get().getTag())) {
				ImageView imgView = mImgViewRef.get();
				if (bm != null && mImgViewRef != null) {
					if(mProgressBarRef != null) {
						mProgressBarRef.get().setVisibility(View.GONE);
					}
					imgView.setVisibility(View.VISIBLE);
					imgView.setImageBitmap(bm);
				}
				mPool.remove(imgView);
			}
		}
	}
	
	private Drawable getBitmap(String...urls) {
		StateListDrawable drawables = new StateListDrawable();
		drawables.addState(new int[] {android.R.attr.state_active}, new BitmapDrawable(getBMFromCache((urls[0]))));
		drawables.addState(new int[] {android.R.attr.state_checked}, null);
		return drawables;
	}
	
	private class StateListBitmap {
		private BitmapDrawable bm;
		private String[] urls;
	}
}
