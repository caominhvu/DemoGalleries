package com.demo.demogalleries;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.demogalleries.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by caominhvu on 5/18/16.
 */
public class PhotosInCategoryActivity extends AppCompatActivity {
    public static final String CATEGORY_NAME = "CATEGORY_NAME";
    String mCategoryName = null;
    ListView mListView;
    MyAdapter mAdapter;
    ProgressDialog mProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCategoryName = getIntent().getStringExtra(CATEGORY_NAME);
        setTitle(mCategoryName);
        setContentView(R.layout.activity_main);

        mAdapter = new MyAdapter();
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String title = ((JSONObject) mJObjPhoto.get(position)).getString("name");
                    String author = ((JSONObject)mJObjPhoto.get(position)).getJSONObject("user").getString("username");
                    String photoId = ((JSONObject)mJObjPhoto.get(position)).getString("id");

                    Intent intent = new Intent(PhotosInCategoryActivity.this, PhotoActivity.class);
                    intent.putExtra(PhotoActivity.TITLE, title);
                    intent.putExtra(PhotoActivity.AUTHOR, author);
                    intent.putExtra(PhotoActivity.ID, photoId);
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        mProgress = new ProgressDialog(PhotosInCategoryActivity.this);
        mProgress.setMessage("Loading...");
        mProgress.show();
        new HttpClientReq(PhotosInCategoryActivity.this, new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                if(mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                if(data != null) {
                    try {
                        mJObjPhoto = new JSONObject((String) data).getJSONArray("photos");
                        mAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).getPhotosOfCategory(mCategoryName);

        if(!Utils.isNetworkConnected(PhotosInCategoryActivity.this)) {
            Toast.makeText(PhotosInCategoryActivity.this, "Please enable network connection!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    JSONArray mJObjPhoto = null;
    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mJObjPhoto != null? mJObjPhoto.length() : -1;
        }

        @Override
        public Object getItem(int position) {
            try {
                return mJObjPhoto.get(position);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.listview_photos_info, null);

                holder = new ViewHolder();
                holder.mTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.mName = (TextView) convertView.findViewById(R.id.tv_name);
                holder.mRating = (TextView) convertView.findViewById(R.id.tv_rate);
                holder.mIcon = (ImageView) convertView.findViewById(R.id.img_ic);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            try {
                holder.mTitle.setText(((JSONObject)mJObjPhoto.get(position)).getString("name"));
                holder.mName.setText(((JSONObject)mJObjPhoto.get(position)).getJSONObject("user").getString("username"));
                holder.mRating.setText("Rating: " + ((JSONObject)mJObjPhoto.get(position)).getString("rating"));
                holder.mIcon.setImageDrawable(getDrawable(R.drawable.ic_logo));
                ImageManager.getInstance(PhotosInCategoryActivity.this).fetch(holder.mIcon, null, ((JSONObject) mJObjPhoto.get(position)).getString("image_url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView mIcon;
        TextView mTitle;
        TextView mName;
        TextView mRating;
    }

}
