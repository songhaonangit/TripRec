package com.gc.triprec;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CameraView m_camera;
    private boolean m_isTakePhoto = false;
    private boolean m_isTakeVideo = false;
    private ImageButton m_btnVideo;
    private TripRecSettings m_settings;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
        Log.i(TAG, "onCreate");
        m_camera = findViewById(R.id.camera);
        m_camera.addCameraListener(m_cameraListener);
        findViewById(R.id.picture).setOnClickListener(this);
        findViewById(R.id.playback).setOnClickListener(this);
        m_btnVideo = findViewById(R.id.video);
        m_btnVideo.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        m_settings = new TripRecSettings(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_camera.start();
        if (!m_settings.getRecordEnable()) {
            m_isTakeVideo = false;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_black_24dp, null));
            } else {
                m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_black_24dp));
            }
        } else {
            m_isTakeVideo = true;
            takeVideo();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_off_black_24dp, null));
            } else {
                m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_off_black_24dp));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_camera.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_camera.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;

            case R.id.action_info:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.intro_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;
            default:
                break;
        }
        return true;
    }

    private CameraListener m_cameraListener = new CameraListener() {
        @Override
        public void onCameraOpened(CameraOptions options) {
            super.onCameraOpened(options);
            Log.i(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            Log.i(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
            m_isTakePhoto = false;
            Log.i(TAG, "onPictureTaken" + String.valueOf(jpeg.length));
            savePhoto(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);

            if (null == m_settings) {
                return;
            }

            if (m_isTakeVideo) {
                takeVideo();
                m_isTakeVideo = true;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !m_camera.isStarted()) {
            m_camera.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.picture:
                if (m_isTakePhoto) {
                    return;
                }
                m_isTakePhoto = true;
                m_camera.captureSnapshot();
                break;

            case R.id.video:

                if (m_isTakePhoto) {
                    return;
                }

                if (m_isTakeVideo) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_black_24dp, null));
                    } else {
                        m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_black_24dp));
                    }
                    m_isTakeVideo = false;
                    m_camera.stopCapturingVideo();
                } else {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_off_black_24dp, null));
                    } else {
                        m_btnVideo.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_off_black_24dp));
                    }
                    m_isTakeVideo = true;
                    takeVideo();
                }
                break;

            case R.id.playback:
                startActivity(new Intent(MainActivity.this, PlaylistActivity.class));
                break;
            default:
                break;
        }
    }

    public File getVideoFilePath() {
        File appDir = new File(getExternalFilesDir(null), "video");
        if (!appDir.exists()) {
            appDir.mkdir();
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String filename = simpleDateFormat.format(date) + ".mp4";
        File file = new File(appDir, filename);

        return file;
    }

    private void savePhoto(Bitmap bmp) {
        File appDir = new File(getExternalFilesDir(null), "photo");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String filename = simpleDateFormat.format(date) + ".jpg";

        File file = new File(appDir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeOldestFile() {
        File appDir = new File(getExternalFilesDir(null), "video");
        if (!appDir.exists()) {
            return ;
        }

        File[] files = appDir.listFiles();
        File old = files[0];
        for (File file : files) {
            if (old.lastModified() < file.lastModified()) {
                old = file;
                break;
            }
        }

        old.deleteOnExit();
    }

    private int getFileTotalCount() {
        File appDir = new File(getExternalFilesDir(null), "video");
        if (!appDir.exists()) {
            return 0;
        }

        File[] files = appDir.listFiles();

        return files.length;
    }

    private void takeVideo() {
        if (null == m_settings)
            return;

        if (m_settings.getOverrideEnable()) {
            if (getFileTotalCount() < m_settings.getVideofilesCount()) {
                removeOldestFile();
            }
        }
        m_camera.startCapturingVideo(getVideoFilePath(), m_settings.getRecordTime() * 1000);
    }
}
