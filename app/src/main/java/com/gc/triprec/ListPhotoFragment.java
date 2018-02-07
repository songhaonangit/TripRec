package com.gc.triprec;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListPhotoFragment extends Fragment implements PlaylistAdapter.AdapterCallback {
    private SwipyRefreshLayout m_swipeRefreshLayout;
    private SwipeMenuListView m_listView;
    private PlaylistAdapter m_adapter;
    private int m_position = 0;
    private List<String> m_photolist = new ArrayList<>();

    private static final String TAG = "PhotoListFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_filelist, container, false);
        m_swipeRefreshLayout = view.findViewById(R.id.swipy_layout);
        m_swipeRefreshLayout.setOnRefreshListener(m_refreshListener);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated");
        m_adapter = new PlaylistAdapter(m_photolist, this);
        m_listView = view.findViewById(R.id.swipfilelist_view);
        m_listView.setAdapter(m_adapter);

        // step 1. create a MenuCreator
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "del" item
                SwipeMenuItem delItem = new SwipeMenuItem(
                        getActivity());
                // set item background
                delItem.setBackground(new ColorDrawable(Color.RED));
                // set item width
                delItem.setWidth(dp2px(90));
                // set item title
                delItem.setTitle("delete");
                // set item title fontsize
                delItem.setTitleSize(18);
                // set item title font color
                delItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(delItem);
            }
        };
        m_listView.setMenuCreator(creator);
        m_listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
        m_listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                m_position  = position;
                String item = m_photolist.get(position);
                switch (index) {
                    case 0:
                        // delete
                        Log.i(TAG, "del " + item);
                        break;
                }
                return false;
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
        searchFiles(context);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private SwipyRefreshLayout.OnRefreshListener m_refreshListener = new SwipyRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {

        }
    };

    @Override
    public void startPlayback(String item) {

    }

    private void searchFiles(Context context) {
        File appDir = new File(context.getExternalFilesDir(null), "photo");
        if (!appDir.exists()) {
            return;
        }

        File[] files = appDir.listFiles();
        for (File file : files) {
            Log.i(TAG, "photo : " + file.getName());
        }
    }
}