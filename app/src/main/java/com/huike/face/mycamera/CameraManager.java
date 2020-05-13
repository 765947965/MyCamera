package com.huike.face.mycamera;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraManager implements CameraPreview.CameraPreviewListener {
    private final String TAG = CameraManager.class.getSimpleName();
    protected boolean front = false;

    protected Camera camera = null;

    protected int cameraId = -1;

    protected SurfaceHolder surfaceHolder = null;

    private CameraListener listener = null;

    private CameraPreview cameraPreview;

    private CameraState state = CameraState.IDEL;

    private int previewDegreen = 0;

    private int manualWidth, manualHeight;

    private Camera.Size previewSize = null;
    private Disposable disposable;

    private byte[] mPicBuffer;

    @SuppressLint("CheckResult")
    public boolean open() {
        if (state != CameraState.OPENING) {
            state = CameraState.OPENING;
            release();
            disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
                camera = Camera.open(cameraId);
                if (camera != null) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraId, info);
                    int previewRotation = 90;
                    camera.setDisplayOrientation(previewRotation);
                    Camera.Parameters param = camera.getParameters();
                    param.setPreviewSize(manualWidth, manualHeight);
                    param.setPreviewFormat(ImageFormat.NV21);
                    camera.setParameters(param);
                    PixelFormat pixelinfo = new PixelFormat();
                    int pixelformat = camera.getParameters().getPreviewFormat();
                    PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
                    Camera.Parameters parameters = camera.getParameters();
                    Camera.Size sz = parameters.getPreviewSize();
                    Log.i(TAG, "camerawidth : " + sz.width + "  height  : " + sz.height);
                    int bufSize = sz.width * sz.height * pixelinfo.bitsPerPixel / 8;
                    if (mPicBuffer == null || mPicBuffer.length != bufSize) {
                        mPicBuffer = new byte[bufSize];
                    }
                    camera.addCallbackBuffer(mPicBuffer);
                    previewSize = sz;
                    emitter.onNext(true);
                    emitter.onComplete();
                } else {
                    emitter.onError(new RuntimeException("camera is null"));
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            cameraPreview.setCamera(camera);
                            state = CameraState.OPENED;
                        }
                        disposable = null;
                    });
            return true;
        } else {
            return false;
        }
    }

    public boolean open(boolean front, int width, int height) {
        if (state == CameraState.OPENING) {
            return false;
        }
        this.manualHeight = height;
        this.manualWidth = width;
        this.front = front;
        return open();
    }

    public void release() {
        if (camera != null) {
            this.cameraPreview.setCamera(null);
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    public void finalRelease() {
        this.listener = null;
        this.cameraPreview = null;
        this.surfaceHolder = null;
    }

    public void setPreviewDisplay(CameraPreview preview) {
        this.cameraPreview = preview;
        this.surfaceHolder = preview.getHolder();
        preview.setListener(this);
    }

    public void setListener(CameraListener listener) {
        Log.i(TAG, "setListener");
        this.listener = listener;
    }

    @Override
    public void onStartPreview() {
        Log.i(TAG, "onStartPreview");
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (listener != null) {
                    listener.onPictureTaken(
                            new CameraPreviewData(data, previewSize.width, previewSize.height,
                                    previewDegreen, front));
                }
                camera.addCallbackBuffer(data);
            }
        });
    }

    public enum CameraState {
        IDEL,
        OPENING,
        OPENED
    }

    public interface CameraListener {
        void onPictureTaken(CameraPreviewData cameraPreviewData);
    }
}
