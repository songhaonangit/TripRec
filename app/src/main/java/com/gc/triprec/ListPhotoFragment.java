package com.gc.triprec;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Comparator;

public class ListPhotoFragment extends ListFragment {

    private static final String TAG = "ListPhotoFragment";

    @Override
    public void playback(int m_position) {

    }

    @Override
    public void searchFiles(Context context) {
        File appDir = new File(context.getExternalFilesDir(null), "photo");
        if (!appDir.exists()) {
            return;
        }

        m_files = appDir.listFiles();
        for (File file : m_files) {
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
