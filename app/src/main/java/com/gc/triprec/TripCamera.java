package com.gc.triprec;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TripCamera {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;

    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }


    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * Camera state: Device is closed.
     */
    private static final int STATE_CLOSED = 0;

    /**
     * Camera state: Device is opened, but is not capturing.
     */
    private static final int STATE_OPENED = 1;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 2;

    /**
     * Camera state: Waiting for 3A convergence before capturing a photo.
     */
    private static final int STATE_WAITING_FOR_3A_CONVERGENCE = 3;


    /**
     * Camera state: Waiting for 3A convergence before capturing a photo.
     */
    private static final int STATE_RECORDING = 4;

    /**
     * The state of the camera device.
     */
    private int m_state = STATE_CLOSED;

    private Integer m_sensorOrientation;

    private CameraManager m_manager;

    private CameraCharacteristics m_cameraCharacteristics;

    private CameraDevice m_cameraDevice;

    private CameraCaptureSession m_cameraCaptureSession;
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder m_previewRequestBuilder;

    private String m_cameraId;

    private Size m_previewSize;

    private MediaRecorder m_mediaRecorder;

    /**
     * A lock protecting camera state.
     */
    private final Object m_cameraStateLock = new Object();

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore m_cameraOpenCloseLock = new Semaphore(1);

    private HandlerThread m_backgroundThread;

    private Handler m_backgroundHandler;

    private static final String TAG = "TripCamera";

    private final CameraDevice.StateCallback m_stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "onOpened");

            synchronized (m_cameraStateLock) {
                m_state = STATE_OPENED;
                m_cameraOpenCloseLock.release();
                m_cameraDevice = camera;
                Log.i(TAG, "onOpened 1 ==============");
                //Todo: Start the preview session if the TextureView has been set up already.
                if ((null != m_previewSize) && (null != m_callback)) {
                    Log.i(TAG, "m_previewSize: " + String.valueOf(m_previewSize));
                    m_callback.onStateOpened();
                }
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (null != m_callback)
                m_callback.onStateDisconnected();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (null != m_callback)
                m_callback.onStateError();
        }
    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events for the preview and
     * pre-capture sequence.
     */
    private CameraCaptureSession.CaptureCallback m_preCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (m_state) {
                case STATE_PREVIEW:
                    // We have nothing to do when the camera preview is running normally.
                    break;

                case STATE_WAITING_FOR_3A_CONVERGENCE:

                    break;

                default:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.i(TAG, "onCaptureProgressed");
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
//            Log.i(TAG, "onCaptureCompleted");
            process(result);
        }
    };

    public TripCamera(CameraManager manager) {
        m_manager = manager;
    }

    public void open(@NonNull TripCameraCallback callback) {
        m_callback = callback;

        if (null == m_callback) {
            Log.i(TAG, "callback null");
            return;
        }

        Log.i(TAG, "open");
        m_mediaRecorder = new MediaRecorder();

        startBackgroundThread();

        /* Todo setup camera outputs */
        setUpCameraOutputs();

       /* Todo open camera */
        openCamera();
    }


    public void close() {
        Log.i(TAG, "close");
        try {
            m_cameraOpenCloseLock.acquire();
            closeCamera();
            if (null != m_mediaRecorder) {
                m_mediaRecorder.release();
                m_mediaRecorder = null;
            }

            stopBackgroundThread();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            m_cameraOpenCloseLock.release();
        }
    }

    public void preview(Surface surface) {
        try {
            if (surface == null) {
                return;
            }

            // We set up a CaptureRequest.Builder with the output Surface.
            m_previewRequestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            m_previewRequestBuilder.addTarget(surface);

            m_cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    synchronized (m_cameraStateLock) {
                        if (null == m_cameraDevice) {
                            return;
                        }


                        try {
                            /* Todo: setup 3A controls locked */

                            session.setRepeatingRequest(m_previewRequestBuilder.build(), m_preCaptureCallback, m_backgroundHandler);
                            m_state = STATE_PREVIEW;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                            return;
                        }
                        m_cameraCaptureSession = session;
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "onConfigureFailed");
                }
            }, m_backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void takePhoto() {

    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Update the camera preview. {@link #preview(Surface)} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == m_cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(m_previewRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            m_cameraCaptureSession.setRepeatingRequest(m_previewRequestBuilder.build(), null, m_backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private TripCameraCallback m_callback;
    public interface TripCameraCallback {
        void onStateOpened();
        void onStateDisconnected();
        void onStateError();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {

        //Todo: Wait for any previously running session to finish.
        try {
            Log.d(TAG, "tryAcquire");
            if (!m_cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MICROSECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId;
            Handler backgroundHandler;
            synchronized (m_cameraStateLock) {
                cameraId = m_cameraId;
                backgroundHandler = m_backgroundHandler;
            }
            m_manager.openCamera(cameraId, m_stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
            Log.i(TAG, "closeCamera 0");
            synchronized (m_cameraStateLock) {
                m_state = STATE_CLOSED;
                Log.i(TAG, "closeCamera 1");
                if (null != m_cameraCaptureSession) {
                    m_cameraCaptureSession.close();
                    m_cameraCaptureSession = null;
                    Log.i(TAG, "closeCamera 2");
                }

                if (null != m_cameraDevice) {
                    m_cameraDevice.close();
                    m_cameraDevice = null;
                }
            }
      }

    private boolean setUpCameraOutputs() {

        try {
            for (String cameraId : m_manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = m_manager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK)
                    continue;

                Log.i(TAG, "1 camid: " + cameraId);
                synchronized (m_cameraStateLock) {
                        /*  Todo:Set up ImageReaders for JPEG and RAW outputs.  Place these in a reference
                            counted wrapper to ensure they are only closed when all background tasks
                            using them are finished.
                         */

                    m_cameraId = cameraId;
                    m_cameraCharacteristics = characteristics;
                }
            }
            return true;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        Log.i(TAG, "startBackgroundThread");
        m_backgroundThread = new HandlerThread("CameraBackground");
        m_backgroundThread.start();
        synchronized (m_cameraStateLock) {
            m_backgroundHandler = new Handler(m_backgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        Log.i(TAG, "stopBackgroundThread");
        if (null == m_backgroundThread)
            return;
        m_backgroundThread.quitSafely();
        try {
            m_backgroundThread.join();
            m_backgroundThread = null;
            synchronized (m_cameraStateLock) {
                m_backgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     *                          orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    public int sensorToDeviceRotation(int deviceOrientation) {
        int sensorOrientation = m_cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        // Reverse device orientation for front-facing cameras
        if (m_cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public int deviceToSensorRotation(int deviceRotation) {
        return (m_cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) ?
                (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                (360 - ORIENTATIONS.get(deviceRotation)) % 360;
    }


    /**
     * Comparator based on area of the given {@link Size} objects.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CameraFragment.CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CameraFragment.CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public <T> Size optimalSize(Class<T> kclass, boolean swappedDimensions, int viewWidth, int viewHeight,
                                Point displaySize) {


        StreamConfigurationMap map = m_cameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                new CameraFragment.CompareSizesByArea());

        int rotatedViewWidth = viewWidth;
        int rotatedViewHeight = viewHeight;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;
        if (swappedDimensions) {
            rotatedViewWidth = viewHeight;
            rotatedViewHeight = viewWidth;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }
        // Preview should not be larger than display size and 1080p.
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }
        // Find the best preview size for these view dimensions and configured JPEG size.
        Size previewSize = chooseOptimalSize(map.getOutputSizes(kclass),
                rotatedViewWidth, rotatedViewHeight, maxPreviewWidth, maxPreviewHeight,
                largestJpeg);
        Log.i(TAG, "previewSize: " + String.valueOf(m_previewSize));
        return previewSize;
    }

    public Size getPreviewSize() {
        return m_previewSize;
    }

    public void setPreviewSize(Size previewSize) {
        m_previewSize = previewSize;
        Log.i(TAG, "2 m_previewSize: " + String.valueOf(m_previewSize));
    }


    private void closePreviewSession() {
        if (m_cameraCaptureSession != null) {
            m_cameraCaptureSession.close();
            m_cameraCaptureSession = null;
        }
    }


    public boolean isClosed() {
        return STATE_CLOSED == m_state;
    }

    public boolean isRecording() {
        return STATE_RECORDING == m_state;
    }

    public void startRecordingVideo(@NonNull Surface surface, @NonNull String dir, int rotation) {
        if (null == m_cameraDevice)
            return;

        closePreviewSession();

        try {
            /* Todo: setup media recorder */
            setUpMediaRecorder(dir, rotation);

            m_previewRequestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(surface);
            m_previewRequestBuilder.addTarget(surface);

            Surface recorderSurface = m_mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            m_previewRequestBuilder.addTarget(recorderSurface);

            m_cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    m_cameraCaptureSession = session;
                    m_state = STATE_RECORDING;
                    updatePreview();
                    m_mediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, m_backgroundHandler);

        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }


    }

    public void stopRecordingVideo() {
        /* Todo recording state change */
        m_state = STATE_PREVIEW;
        m_mediaRecorder.stop();
        m_mediaRecorder.reset();

        /* start preview */
    }

    private void setUpMediaRecorder(String filePath, int rotation) throws IOException {
        m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        m_mediaRecorder.setOutputFile(filePath);
        m_mediaRecorder.setVideoEncodingBitRate(10000000);
        m_mediaRecorder.setVideoFrameRate(30);
        m_mediaRecorder.setVideoSize(m_previewSize.getWidth(), m_previewSize.getHeight());
        m_mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        m_sensorOrientation = m_cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        switch (m_sensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                m_mediaRecorder.setOrientationHint(ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                m_mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        m_mediaRecorder.prepare();

    }

}
