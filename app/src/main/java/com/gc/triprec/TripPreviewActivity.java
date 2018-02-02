package com.gc.triprec;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

public class TripPreviewActivity extends AppCompatActivity implements ServiceConnection {

    private TripCamera m_camera = null;
    private TripRecService m_service;
    private boolean m_bound = false;
    private static final String TAG = "TripPreviewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_preview);
        Log.i(TAG, "onCreate");
        if (null == savedInstanceState) {
            Log.i(TAG, "onCreate 1");
//            getFragmentManager().beginTransaction()
//                    .replace(R.id.container, TripPreviewFragment.newInstance())
//                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        bindTripRecService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        unbindTripRecService();
    }

    private void bindTripRecService() {
        if (m_bound) {
            return;
        }

        bindService(new Intent(TripPreviewActivity.this,
                TripRecService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void unbindTripRecService() {
        if (m_bound) {
            unbindService(this);
            m_bound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "service connected");
        m_bound = true;
        TripRecService.BinderService binder = (TripRecService.BinderService)service;
        m_service = binder.getService();
        if (null == m_camera) {
            Log.i(TAG, "service connected 1");
            m_camera = m_service.getCamera();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, TripPreviewFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "service disconnected");
        m_bound = false;
    }

    public TripCamera getCamera() {
        if (null == m_service) {
            return null;
        }
        return m_service.getCamera();
    }

}
