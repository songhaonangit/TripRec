package com.gc.triprec;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TripPreviewActivity extends AppCompatActivity {

    private static final int CAM_LENS_FACING = 0;
    private static final int CAM_LENS_FACING_BACK = 1;
    private CameraManager m_manager;
    private CameraDevice m_camdev;
    private Size m_previewSize;
    private String m_camId;
    private AutoFitTextureView m_textureView;
    private static final String TAG = "TripPreviewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_preview);
        m_textureView = (AutoFitTextureView) findViewById(R.id.preview_texture_view);
        m_textureView.setSurfaceTextureListener(m_textureListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private SurfaceTextureListener m_textureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable width: " + String.valueOf(width) + " height: " + String.valueOf(height));
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged width: " + String.valueOf(width) + " height: " + String.valueOf(height));
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "onSurfaceTextureDestroyed ");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void openCamera(int w, int h) {
        m_manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] list = m_manager.getCameraIdList();

            for (int i = 0; i < list.length; i++) {
                Log.i(TAG, "camid: " + list[i]);
                CameraCharacteristics cameraCharacteristics = m_manager.getCameraCharacteristics(list[i]);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i(TAG, "LENS_FACING: " + facing.toString());
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                Size[] size = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
//
//                for (int j = 0; j < size.length; j ++) {
//                    Log.i(TAG, "size: " + size[j].toString());
//                }
                Size largest = Collections.max(Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                Log.i(TAG, "largest size: " + largest.toString());

            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            m_camId = list[CAM_LENS_FACING_BACK];
            configCamera(w, h);

            m_manager.openCamera(list[CAM_LENS_FACING], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.i(TAG, "camera opened");
                    m_camdev = camera;
                    createCameraPreviewSess();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.i(TAG, "camera disconnected");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.i(TAG, "camera error");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configCamera(int w, int h) {
        try {
            CameraCharacteristics cameraCharacteristics = m_manager.getCameraCharacteristics(m_camId);
            StreamConfigurationMap map = cameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            m_previewSize = chooseOptimalSize(map.getOutputSizes(
                    SurfaceTexture.class), w, h, largest);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                m_textureView.setAspectRatio(m_previewSize.getWidth(), m_previewSize.getHeight());
            }
            else
            {
                m_textureView.setAspectRatio(m_previewSize.getHeight(), m_previewSize.getWidth());
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio)
    {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            System.out.println("找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }

    private CaptureRequest.Builder  m_previewReqBuilder;
    private CaptureRequest  m_previewReq;
    private CameraCaptureSession    m_session;
    private void createCameraPreviewSess() {
        SurfaceTexture surfaceTexture = m_textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(m_previewSize.getWidth(), m_previewSize.getHeight());
        try {
            m_previewReqBuilder = m_camdev.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface surface = new Surface(m_textureView.getSurfaceTexture());
            m_previewReqBuilder.addTarget(surface);

            m_camdev.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    m_session = session;

                    m_previewReqBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    m_previewReqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    // 开始显示相机预览
                    m_previewReq = m_previewReqBuilder.build();
                    // 设置预览时连续捕获图像数据
                    try {
                        m_session.setRepeatingRequest(m_previewReq, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
