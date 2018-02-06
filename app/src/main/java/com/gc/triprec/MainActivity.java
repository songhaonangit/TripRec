package com.gc.triprec;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CameraView m_camera;
    private File m_file;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        m_file = new File(getVideoFilePath(this));
        m_camera = findViewById(R.id.camera);
        m_camera.addCameraListener(m_cameraListener);
        findViewById(R.id.picture).setOnClickListener(this);
        findViewById(R.id.video).setOnClickListener(this);
        findViewById(R.id.settings).setOnClickListener(this);
        findViewById(R.id.info).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_camera.start();
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
            Log.i(TAG, "onPictureTaken" + String.valueOf(jpeg.length));
            saveImage(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);
            Log.i(TAG, "onVideoTaken");
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

    public String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.picture:
                m_camera.captureSnapshot();
                break;

            case R.id.video:

                break;

            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;

            case R.id.info:

                break;

            default:
                break;
        }
    }

    private void saveImage(Bitmap bmp) {
        File appDir = new File(getExternalFilesDir(null), "photo");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
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

}
