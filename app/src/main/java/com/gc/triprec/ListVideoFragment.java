package com.gc.triprec;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.Comparator;

public class ListVideoFragment extends ListFragment {

    private static final String TAG = "ListPhotoFragment";

    @Override
    public void playback(int position) {
        Intent intent = new Intent(getActivity().getApplicationContext(), PlaybackVideoActivity.class);
        intent.putExtra("video", Uri.fromFile(m_filelist.get(position)));
        startActivity(intent);
    }

    @Override
    public void searchFiles(Context context) {
        File appDir = new File(context.getExternalFilesDir(null), "video");
        if (!appDir.exists()) {
            return;
        }

        File[] files = appDir.listFiles();
        for (File file : files) {
            Log.i(TAG, "video : " + file.getName());
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

