package com.demo.demogalleries;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(new MyAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, PhotosInCategoryActivity.class);
                intent.putExtra(PhotosInCategoryActivity.CATEGORY_NAME, mCategories[position]);
                startActivity(intent);
            }
        });
    }

    String[] mCategories = CategoriesUtils.getCategories();
    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCategories.length;
        }

        @Override
        public Object getItem(int position) {
            return mCategories[position];
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
                convertView = getLayoutInflater().inflate(R.layout.listview_categories, null);

                holder = new ViewHolder();
                holder.mTv = (TextView) convertView.findViewById(R.id.tv_name);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTv.setText(mCategories[position]);
            return convertView;
        }

    }
    private static class ViewHolder {
        TextView mTv;
    }

}
