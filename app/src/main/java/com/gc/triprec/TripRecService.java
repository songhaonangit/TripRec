package com.gc.triprec;

import android.app.Service;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TripRecService extends Service {
    private TripCamera m_camera = null;
    private TripRecSettings m_settings = null;
    private static final String TAG = "TripRecService";

    public TripRecService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        m_settings = new TripRecSettings(this);
        if (null != m_camera) {
            return;
        }
        Log.i(TAG, "onCreate 1");
        m_camera = new TripCamera((CameraManager) getSystemService(CAMERA_SERVICE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        // TODO: Return the communication channel to the service.
        return m_binderService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private final BinderService m_binderService = new BinderService();

    public class BinderService extends Binder {
        public TripRecService getService() { return TripRecService.this; }
    }

    public TripCamera getCamera() {
        return m_camera;
    }

    public TripRecSettings getSettings() {
        return m_settings;
    }
}
