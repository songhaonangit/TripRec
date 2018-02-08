package com.gc.triprec;

import android.content.Context;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup.LayoutParams;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlaybackPhotoActivity extends AppCompatActivity {
    private static final String TAG = "PlaybackPhotoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback_photo);

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new PhotoPagerAdapter(this));
        int position = getIntent().getIntExtra("photo", 0);
        viewPager.setCurrentItem(position);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private class PhotoPagerAdapter extends PagerAdapter {
        private List<File> m_filelist = new ArrayList<>();
        private static final String TAG = "PagerAdapter";

        public PhotoPagerAdapter(Context context) {
            super();
            searchFiles(context);
        }

        @Override
        public int getCount() {
            return m_filelist.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(container.getContext());
            Uri uri = Uri.fromFile(m_filelist.get(position));
            photoView.setImageURI(uri);
            container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            return photoView;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        public void searchFiles(Context context) {
            File appDir = new File(context.getExternalFilesDir(null), "photo");
            if (!appDir.exists()) {
                return;
            }

            File[] files = appDir.listFiles();
            for (File file : files) {
                Log.i(TAG, "photo : " + file.getName());
                m_filelist.add(file);
            }


            Comparator c = new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    if(file1.lastModified() < file2.lastModified())
                        return 1;
                    else
                        return -1;
                }
            };

            m_filelist.sort(c);
        }
    }
}
