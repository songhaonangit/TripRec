package com.gc.triprec;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;

public class ListPhotoFragment extends ListFragment {

    private static final String TAG = "ListPhotoFragment";

    @Override
    public void playback(int m_position) {

        Log.i(TAG, "ListPhotoFragment  playback: " + m_position);
        Intent intent = new Intent(getActivity().getApplicationContext(), PlaybackPhotoActivity.class);
        intent.putExtra("photo", m_position);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
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

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            m_filelist.sort(c);
        } else {
            Object[] a = m_filelist.toArray();
            Arrays.sort(a, (Comparator) c);
            ListIterator<File> i = m_filelist.listIterator();
            for (Object e : a) {
                i.next();
                i.set((File) e);
            }
        }

    }
}
