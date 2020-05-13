package com.huike.face.mycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.RuntimeABI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private CameraManager cameraManagerRgb;
    private CameraManager cameraManagerIr;
    private FaceEngine ftEngine;
    private BoxView rgb, ir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rgb = findViewById(R.id.rgbBox);
        ir = findViewById(R.id.irBox);
        if (!(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                || !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                || !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            //没有权限，申请权限
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA};
            //申请权限，其中RC_PERMISSION是权限申请码，用来标志权限申请的
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 10001);
        } else {
            initCamera();
            activeEngine();
        }
    }

    private void initCamera() {
        cameraManagerRgb = new CameraManager();
        cameraManagerRgb.setPreviewDisplay(findViewById(R.id.cameraPreviewRgb));
        cameraManagerRgb.setListener(ComplexFrameHelper::addRgbFrame);
        cameraManagerIr = new CameraManager();
        cameraManagerIr.setPreviewDisplay(findViewById(R.id.cameraPreviewIr));
        cameraManagerIr.setListener(ComplexFrameHelper::addIRFrame);
    }

    private void initThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Pair<CameraPreviewData, CameraPreviewData> pair = null;
                    try {
                        pair = ComplexFrameHelper.takeComplexFrame();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (pair == null) {
                        continue;
                    }
                    CameraPreviewData rgbData = pair.first;
                    CameraPreviewData irData = pair.second;
                    List<FaceInfo> faceInfoList = new ArrayList<>();
                    // 人脸框
                    int code = ftEngine.detectFaces(rgbData.nv21Data, rgbData.width, rgbData.height, FaceEngine.CP_PAF_NV21, faceInfoList);
                    if (code != ErrorInfo.MOK || faceInfoList.size() == 0) {
                        Log.i("ccccccccccc", "无人脸");
                        continue;
                    }
                    rgb.showFaceBox(faceInfoList.get(0).getRect());
                    // 活体
                    int flCode = ftEngine.processIr(irData.nv21Data, irData.width, irData.height, FaceEngine.CP_PAF_NV21, Arrays.asList(new FaceInfo(faceInfoList.get(0).clone())), FaceEngine.ASF_IR_LIVENESS);
                    if (flCode != ErrorInfo.MOK) {
                        Log.i("ccccccccccc", "活体检测失败");
                        continue;
                    }
                    ir.showFaceBox(faceInfoList.get(0).getRect());
                    List<LivenessInfo> livenessInfoList = new ArrayList<>();
                    flCode = ftEngine.getIrLiveness(livenessInfoList);
                    if (flCode != ErrorInfo.MOK || livenessInfoList.size() == 0) {
                        Log.i("ccccccccccc", "IR无活体");
                        continue;
                    }
                    LivenessInfo livenessInfo = livenessInfoList.get(0);
                    if (livenessInfo.getLiveness() != LivenessInfo.ALIVE) {
                        Log.i("ccccccccccc", "非活体:" + livenessInfo.getLiveness());
                        continue;
                    }
                    Log.i("ccccccccccc", "活体检测成功");
                    Log.i("ccccccccccc", "FaceId:" + faceInfoList.get(0).getFaceId());
                    // 提取特征值
                    FaceFeature faceFeature = new FaceFeature();
                    ftEngine.extractFaceFeature(rgbData.nv21Data, rgbData.width, rgbData.height, FaceEngine.CP_PAF_NV21, new FaceInfo(faceInfoList.get(0)), faceFeature);
                    Log.i("ccccccccccc", "特征值:" + new String(Base64.encode(faceFeature.getFeatureData(), Base64.NO_WRAP)));
                    FaceFeature faceFeatureOld = new FaceFeature();
                    faceFeatureOld.setFeatureData(Base64.decode("AID6RAAAoEEWQzI9wJEtPgmUub0AdQG8nI8UvaSxJj2GKyO+eVeQPADopztYKCy9FoQbPbfvFTxlFMo9hagTune+kD1beM09b/oNPdD2b7w/SY+98/9DPTbq0r3E3pk87KUOPtqebDwUS5C95IRKvRXuDj2yc6a9kzFePa8jA76qVYe92aJQPVTtpb2MFqO9Nz2fvZE2wbyYtU08E2B9PCXF3ruYnFG9bffnvXaH5byS7O25kyu+vAWFAj6Azm09diDgPWDwDz12iI499Up0PXEqwbxxKIe8IFg1u87ZIb6eOpC9N4xRvIdXcjyg12w7WqM5PRAt7T0KdBa8rtfivOoJmT0MDsQ8sQsxPvfTNb7tyes8uxXBPSP7OT1kQ3g9fUbfOkMXkr0AMCS9oCfePZ/3uTx9g0k8f5wBPFLHgL1TZK29nIWKvDaEWr3s18u8fGgSvL2vmL0HWRQ9QucoPhhylb3xCaS9pTuaPYnurb18ak08EDJRPPk/hD2f7fe9OTcQvRHKqjxLdjY9nC7GPCTOhLw1Mrs9wpvMvfktCz3gG98635Y0vdFxzrzOQIy8b2XAvH68+7w5GMw8ddWpvVlFJz0a8iy9oBFyPCfP3LzX5I+7JqXBvNqQ0bwDyjg97GPpPSm4rb2dphm9zLmhvOxt2LqLv/W8qR40vU7Wv7wCVYE9CaeSvRXSnLxl56M9SY6IvRatqTyfac68SC0VvaKhEz286v+4D393PE44FD0fooO891LmO35ZSD2QwCE9fR1AuxrNxj2/FYO7dxzyvEa7L7yf0fs9vcuLPX82pb17CpU8sQkFPf5YlLx85BO+X3u4PdycSb3Z/eO8HoeNPPyqCb1M1uq8jYuJPNHmojzsSlY9Yl0bvQVIrTxiXy++8mS2vZgYQL3fxqS9ZIaBO+0gGD2Ofrk9XYCyPVgrTDzJCXO9tq5sO2pDwzpFSWk9OTX1PObDQz0uPp09ldlTPa+I8TxDcoy7QHp1vVpzXj1NdkM9Q//4vPpPRzyRCuU9DnQ2PdjE17wwfFu80l4NvjyrGLwlqWq9n+u4vWiecr2Bsau8FxI/vFFeBz7I9XW99CZmPe9bUD0kCuS8NjPPvS6Pv704K+G8xWhgPXaNob2cEsu8/AGWPW8AirygL/e9qIIWvKofmjxfw0+9oOzKvZNyHbxSzts8EKzEPBZVJbwXy+G9hX38vKeNcz29mSg9HCMJvRg+jzwcOda8NLSpPHDaZr0VA3a91hSdPfA0u7zS5/085dZnPXZlVzud6w49VUaKvbO4Nbwc7as8ekTaPJclIDzvH1A8NW2wvFXDtjvWS3o75tENPvfzcb3OR+09w3vCvXGDh719oMk856/IPZ0rBr1WQNa9".getBytes(), Base64.NO_WRAP));
                    FaceSimilar faceSimilar = new FaceSimilar();
                    flCode = ftEngine.compareFaceFeature(faceFeature, faceFeatureOld, faceSimilar);
                    if (flCode != ErrorInfo.MOK) {
                        Log.i("ccccccccccc", "特征值对比失败!");
                        continue;
                    }
                    Log.i("ccccccccccc", "特征值对比分数：" + faceSimilar.getScore());
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openManager();
    }

    private void openManager() {
        if (cameraManagerRgb != null) {
            cameraManagerRgb.open(false, 640, 480);
        }
        if (cameraManagerIr != null) {
            cameraManagerIr.open(true, 640, 480);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraManagerRgb != null) {
            cameraManagerRgb.release();
        }
        if (cameraManagerIr != null) {
            cameraManagerIr.release();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraManagerRgb != null) {
            cameraManagerRgb.finalRelease();
        }
        if (cameraManagerIr != null) {
            cameraManagerIr.finalRelease();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 10001 && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 下次进来再开启
            finish();
        }
    }

    //激活
    public void activeEngine() {
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
            Log.i(TAG, "subscribe: getRuntimeABI() " + runtimeABI);
            int activeCode = FaceEngine.activeOnline(MainActivity.this, Constants.ACTIVE_KEY, Constants.APP_ID, Constants.SDK_KEY);
            emitter.onNext(activeCode);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast("激活引擎成功");
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            showToast("引擎已激活，无需再次激活");
                        } else {
                            showToast(String.format("引擎激活失败，错误码为 %d", activeCode));
                        }
                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                            initSdk();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    // 初始化SDK
    private void initSdk() {
        ftEngine = new FaceEngine();
        ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_90_ONLY,
                16, 1, FaceEngine.ASF_FACE_DETECT
                        | FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_IR_LIVENESS);
        initThread();
    }

    protected void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    protected void showLongToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
