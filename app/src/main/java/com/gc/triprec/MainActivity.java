package com.gc.triprec;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private CameraView m_camera;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        m_camera = findViewById(R.id.camera);
        m_camera.addCameraListener(m_cameraListener);
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
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
        }

        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);
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
}
