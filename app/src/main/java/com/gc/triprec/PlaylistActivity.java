package com.gc.triprec;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItem;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentStatePagerItemAdapter;

public class PlaylistActivity extends FragmentActivity {

    private static final String TAG = "PlaylistActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        createListPages();
    }

    private void createListPages() {
        Log.i(TAG, "createListPages");
        ViewGroup tab = findViewById(R.id.tab);
        tab.addView(LayoutInflater.from(this).inflate(R.layout.pagetab, tab, false));
        FragmentPagerItems pages = new FragmentPagerItems(this);
        pages.add(FragmentPagerItem.of("video", ListVideoFragment.class));
        pages.add(FragmentPagerItem.of("photo", ListPhotoFragment.class));
        FragmentStatePagerItemAdapter adapter = new FragmentStatePagerItemAdapter(getSupportFragmentManager(), pages);
        ListViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        SmartTabLayout viewpagerTab = (SmartTabLayout) findViewById(R.id.viewpagertab);
        viewpagerTab.setViewPager(viewPager);
    }

}
