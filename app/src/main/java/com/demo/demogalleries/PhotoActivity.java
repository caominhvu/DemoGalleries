package com.demo.demogalleries;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.demogalleries.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by caominhvu on 5/18/16.
 */
public class PhotoActivity extends AppCompatActivity {
    public static final String TITLE = "TITLE";
    public static final String AUTHOR = "AUTHOR";
    public static final String ID = "ID";

    ProgressDialog mProgress;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        setTitle(getIntent().getStringExtra(TITLE));
        ((TextView) findViewById(R.id.tv_title)).setText(getIntent().getStringExtra(TITLE));
        ((TextView) findViewById(R.id.tv_name)).setText(getIntent().getStringExtra(AUTHOR));
        final ImageView imageView = (ImageView) findViewById(R.id.img_photo);

        mProgress = new ProgressDialog(PhotoActivity.this);
        mProgress.setMessage("Loading...");
        mProgress.show();

        new HttpClientReq(PhotoActivity.this, new Observer() {
            @Override
            public void update(Observable observable, final Object data) {
                if(mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                if (data != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String url = ((JSONObject) (new JSONObject((String) data)).getJSONObject("photo").getJSONArray("images").get(0)).getString("url");
                                ImageManager.getInstance(PhotoActivity.this).fetch(imageView, null, url);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }).getPhotoDetail(getIntent().getStringExtra(ID));
        if(!Utils.isNetworkConnected(PhotoActivity.this)) {
            Toast.makeText(PhotoActivity.this, "Please enable network connection!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }
}
