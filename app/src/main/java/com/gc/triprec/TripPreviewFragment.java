package com.gc.triprec;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.lang.ref.WeakReference;

public class TripPreviewFragment extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback , View.OnClickListener{

    private Button m_btnPhoto;

    private Button m_btnVideo;

    private Button m_btnSetting;

    private AutoFitTextureView m_textureView;

    private OrientationEventListener m_orientationListener;

    private TripCamera m_camera;

    private TripRecSettings m_settings;

    private String m_nextVideoAbsolutePath = null;

    /**
     * Tolerance when comparing aspect ratios.
     */
    private static final double ASPECT_RATIO_TOLERANCE = 0.005;

    /**
     * Request code for camera permissions.
     */
    private static final int REQUEST_CAMERA_PERMISSIONS = 1;


    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * Permissions required to take a picture.
     */
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    private static final String TAG = "TripPreviewFragment";

    public static TripPreviewFragment newInstance() {
        return new TripPreviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.camview, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        m_textureView = view.findViewById(R.id.autofit_view);
        m_btnPhoto = view.findViewById(R.id.picture);
        m_btnPhoto.setOnClickListener(this);
        m_btnVideo = view.findViewById(R.id.video);
        m_btnVideo.setOnClickListener(this);
        m_btnSetting = view.findViewById(R.id.settings);
        m_btnSetting.setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        // Setup a new OrientationEventListener.  This is used to handle rotation events like a
        // 180 degree rotation that do not normally trigger a call to onCreate to do view re-layout
        // or otherwise cause the preview TextureView's size to change.
        m_orientationListener = new OrientationEventListener(getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (m_textureView != null && m_textureView.isAvailable()) {
                    Log.i(TAG, "onOrientationChanged " + String.valueOf(orientation));
                    /* Todo configure transform */
                    configureTransform(m_textureView.getWidth(), m_textureView.getHeight());
                }
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
        TripPreviewActivity parent = (TripPreviewActivity) getActivity();
        m_camera = parent.getCamera();
        m_settings = parent.getSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume");
        if (null == m_camera)
            return;

        /* Todo request permissions */
        if (!hasAllPermissionsGranted()) {
            requestCameraPermissions();
            Log.e(TAG, "no permission");
            return;
        }

        m_camera.open(m_callback);

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we should
        // configure the preview bounds here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (m_textureView.isAvailable()) {
            /* Todo config transform */
            Log.i(TAG, "onResume m_textureView.isAvailable()");
            configureTransform(m_textureView.getWidth(), m_textureView.getHeight());
        } else {
            Log.i(TAG, "onResume m_textureView. not isAvailable()");
            m_textureView.setSurfaceTextureListener(m_surfaceTextureListener);
            if ((null != m_orientationListener) && m_orientationListener.canDetectOrientation()) {
                m_orientationListener.enable();
            }
        }
    }

    @Override
    public void onPause() {
        if (null == m_camera) {
            super.onPause();
            return;
        }
        if (null != m_orientationListener) {
            m_orientationListener.disable();
        }
        /* Todo close camera */
        m_camera.close();
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private final TextureView.SurfaceTextureListener m_surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable");
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged");
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "onSurfaceTextureDestroyed");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    private TripCamera.TripCameraCallback m_callback = new TripCamera.TripCameraCallback() {
        @Override
        public void onStateOpened() {
            if (m_textureView.isAvailable() && (null != m_camera)) {

                SurfaceTexture texture = m_textureView.getSurfaceTexture();
                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(m_camera.getPreviewSize().getWidth(), m_camera.getPreviewSize().getHeight());

                // This is the output Surface we need to start preview.
                Surface surface = new Surface(texture);
                Log.i(TAG,"onStateOpened 0");
                m_camera.preview(surface);
                Log.i(TAG,"onStateOpened 1");
            }
        }

        @Override
        public void onStateDisconnected() {

        }

        @Override
        public void onStateError() {

        }
    };

    /**
     * Tells whether all the necessary permissions are granted to this app.
     *
     * @return True if all the required permissions are granted.
     */
    private boolean hasAllPermissionsGranted() {
        for (String permission : CAMERA_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets whether you should show UI with rationale for requesting the permissions.
     *
     * @return True if the UI should be shown.
     */
    private boolean shouldShowRationale() {
        for (String permission : CAMERA_PERMISSIONS) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions necessary to use camera and save pictures.
     */
    private void requestCameraPermissions() {
        if (shouldShowRationale()) {
            PermissionConfirmationDialog.newInstance().show(getChildFragmentManager(), "dialog");
        } else {
            FragmentCompat.requestPermissions(this, CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            if (grantResults.length == CAMERA_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        Log.i(TAG, "configureTransform w: " + String.valueOf(viewWidth) + " h: " + String.valueOf(viewHeight));
        if ((null == m_textureView) || (null == activity) || (null == m_camera))
            return;

        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int totalRotation = m_camera.sensorToDeviceRotation(deviceRotation);
        boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;

        Size previewSize = m_camera.optimalSize(SurfaceTexture.class, swappedDimensions, viewWidth, viewHeight, displaySize);
        Log.i(TAG, "configureTransform previewSize: " + String.valueOf(previewSize));

        if (swappedDimensions) {
            m_textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        } else {
            m_textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        }

        int rotation = m_camera.deviceToSensorRotation(deviceRotation);


        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        // Initially, output stream images from the Camera2 API will be rotated to the native
        // device orientation from the sensor's orientation, and the TextureView will default to
        // scaling these buffers to fill it's view bounds.  If the aspect ratios and relative
        // orientations are correct, this is fine.
        //
        // However, if the device orientation has been rotated relative to its native
        // orientation so that the TextureView's dimensions are swapped relative to the
        // native device orientation, we must do the following to ensure the output stream
        // images are not incorrectly scaled by the TextureView:
        //   - Undo the scale-to-fill from the output buffer's dimensions (i.e. its dimensions
        //     in the native device orientation) to the TextureView's dimension.
        //   - Apply a scale-to-fill from the output buffer's rotated dimensions
        //     (i.e. its dimensions in the current device orientation) to the TextureView's
        //     dimensions.
        //   - Apply the rotation from the native device orientation to the current device
        //     rotation.
        if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);

        }
        matrix.postRotate(rotation, centerX, centerY);

        m_textureView.setTransform(matrix);

        if (m_camera.getPreviewSize() == null || !checkAspectsEqual(previewSize, m_camera.getPreviewSize())) {
            m_camera.setPreviewSize(previewSize);
            if (!m_camera.isClosed()) {
                /* Todo create CameraPreviewSession*/
                SurfaceTexture texture = m_textureView.getSurfaceTexture();
                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(m_camera.getPreviewSize().getWidth(), m_camera.getPreviewSize().getHeight());

                // This is the output Surface we need to start preview.
                Surface surface = new Surface(texture);

                m_camera.preview(surface);
            }
        }
    }

    /**
     * Return true if the two given {@link Size}s have the same aspect ratio.
     *
     * @param a first {@link Size} to compare.
     * @param b second {@link Size} to compare.
     * @return true if the sizes have the same aspect ratio, otherwise false.
     */
    private static boolean checkAspectsEqual(Size a, Size b) {
        double aAspect = a.getWidth() / (double) a.getHeight();
        double bAspect = b.getWidth() / (double) b.getHeight();
        return Math.abs(aAspect - bAspect) <= ASPECT_RATIO_TOLERANCE;
    }


    public String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    private static final int MsgRecordStart = 1;
    private static final int MsgRecordStop = 2;
    private static final int MsgSettings = 3;
    private PreviewHandler m_handler = new PreviewHandler(this);
    private static class PreviewHandler extends Handler {
        private final WeakReference<TripPreviewFragment> m_fragment;

        PreviewHandler(TripPreviewFragment fragment) {
            m_fragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TripPreviewFragment fragment = m_fragment.get();
            if (null == fragment.m_camera)
                return;
            SurfaceTexture texture = fragment.m_textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(fragment.m_camera.getPreviewSize().getWidth(), fragment.m_camera.getPreviewSize().getHeight());

            switch (msg.what) {
                case MsgRecordStart:
                    Activity activity = fragment.getActivity();
                    if (null == activity)
                        return;
                    fragment.m_nextVideoAbsolutePath = fragment.getVideoFilePath(activity);
                    Log.d(TAG, "start recording Video saved: " + fragment.m_nextVideoAbsolutePath);

                    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                    fragment.m_camera.startRecordingVideo(new Surface(texture), fragment.m_nextVideoAbsolutePath, rotation);

                    break;

                case MsgRecordStop:
                    if (fragment.m_camera.isRecording()) {
                        Log.i(TAG, "stop recording");
                        fragment.m_camera.stopRecordingVideo();
                        fragment.m_nextVideoAbsolutePath = null;
                        fragment.m_camera.preview(new Surface(texture));
                    }
                    break;

                case MsgSettings:
                    fragment.m_camera.close();
                    Intent intent = new Intent();
                    intent.setClass(fragment.getActivity().getApplicationContext(), SettingsActivity.class);
                    fragment.getActivity().startActivity(intent);
                    break;

                default:
                    break;
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.picture:

                break;

            case R.id.video:
                if (m_camera.isRecording()) {
                    m_handler.sendEmptyMessage(MsgRecordStop);
                } else {
                    m_handler.sendEmptyMessage(MsgRecordStart);
                }
                break;

            case R.id.info:
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;

            case R.id.settings:
                m_handler.sendEmptyMessage(MsgSettings);
                break;

            default:

                break;
        }
    }

    public static class PermissionConfirmationDialog extends DialogFragment {

        public static CameraFragment.PermissionConfirmationDialog newInstance() {
            return new CameraFragment.PermissionConfirmationDialog();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, CAMERA_PERMISSIONS,
                                    REQUEST_CAMERA_PERMISSIONS);
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    }).create();
        }
    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

}