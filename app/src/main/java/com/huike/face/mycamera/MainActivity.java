package com.huike.face.mycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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
import com.arcsoft.face.model.ArcSoftImageInfo;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.google.gson.Gson;
import com.q_zheng.QZhengIFManager;
import com.tencent.mmkv.MMKV;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private TimeImage imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MMKV.initialize(this);
        new QZhengIFManager(this).setMonitorApp(getPackageName(), false, 15);
        setContentView(R.layout.activity_main);
        rgb = findViewById(R.id.rgbBox);
        ir = findViewById(R.id.irBox);
        imageView = findViewById(R.id.imageView);
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
                    int code = ftEngine.detectFaces(irData.nv21Data, irData.width, irData.height, FaceEngine.CP_PAF_NV21, faceInfoList);
                    if (code != ErrorInfo.MOK || faceInfoList.size() == 0) {
                        //Log.i("ccccccccccc", "无人脸");
                        continue;
                    }
                    keepMaxFace(faceInfoList);
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
                    //Log.i("ccccccccccc", "特征值:" + new String(Base64.encode(faceFeature.getFeatureData(), Base64.NO_WRAP)));
                    String name = null;
                    float score = 0;
                    for (Map.Entry<String, FaceFeature> item : FaceFeatureMap.entrySet()) {
                        FaceSimilar faceSimilar = new FaceSimilar();
                        flCode = ftEngine.compareFaceFeature(faceFeature, item.getValue(), faceSimilar);
                        if (flCode == ErrorInfo.MOK) {
                            if (faceSimilar.getScore() > score) {
                                score = faceSimilar.getScore();
                                name = item.getKey();
                            }
                        }
                    }
                    if (score > 0.8) {
                        Log.i("ccccccccccc", "识别成功：CODE=" + score + "; NAME=" + name);
                        final String path = name;
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(BitmapFactory.decodeFile(new File("/sdcard/Android/data/com.east.face.device.east/files/Documents/face", path).getAbsolutePath()));
                                imageView.timing();
                            }
                        });
                    } else {
                        Log.i("ccccccccccc", "陌生人:" + score);
                    }
                }
            }

            private void keepMaxFace(List<FaceInfo> ftFaceList) {
                if (ftFaceList == null || ftFaceList.size() <= 1) {
                    return;
                }
                FaceInfo maxFaceInfo = ftFaceList.get(0);
                for (FaceInfo faceInfo : ftFaceList) {
                    if (faceInfo.getRect().width() > maxFaceInfo.getRect().width()) {
                        maxFaceInfo = faceInfo;
                    }
                }
                ftFaceList.clear();
                ftFaceList.add(maxFaceInfo);
            }

            private void setMaxFace(List<FaceInfo> faceInfoList) {
                FaceInfo add = null;
                for (FaceInfo item : faceInfoList) {
                    if (add == null) {
                        add = item;
                    } else {
                        Rect rectOld = add.getRect();
                        Rect rectNew = item.getRect();
                        int oldWith = rectOld.right - rectOld.left;
                        int oldHeight = rectOld.bottom - rectOld.top;
                        int newWith = rectNew.right - rectNew.left;
                        int newHeight = rectNew.bottom - rectNew.top;
                        if (oldWith * oldHeight < newHeight * newWith) {
                            add = item;
                        }
                    }
                }
                faceInfoList.clear();
                faceInfoList.add(add);
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

    private Map<String, FaceFeature> FaceFeatureMap = new HashMap<>();

    // 初始化SDK
    private void initSdk() {
        ftEngine = new FaceEngine();
        ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_90_ONLY,
                16, 10, FaceEngine.ASF_FACE_DETECT
                        | FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_IR_LIVENESS);
        final FaceEngine rill = new FaceEngine();
        rill.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                32, 1, FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_FACE_RECOGNITION);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String value = MMKV.defaultMMKV().decodeString("value1", null);
                if (value == null) {
                    File fl = new File("/sdcard/Android/data/com.east.face.device.east/files/Documents/face");
                    for (File item : fl.listFiles()) {
                        Bitmap fileBitMap = BitmapFactory.decodeFile(item.getAbsolutePath());
                        Bitmap bitmap = ArcSoftImageUtil.getAlignedBitmap(fileBitMap, true);
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), ArcSoftImageFormat.BGR24);
                        int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
                        if (transformCode == ArcSoftImageUtilError.CODE_SUCCESS) {
                            ArcSoftImageInfo arcSoftImageInfo = new ArcSoftImageInfo(width, height, FaceEngine.CP_PAF_BGR24, new byte[][]{bgr24}, new int[]{width * 3});
                            // 传入ArcSoftImageInfo对象进行人脸检测
                            List<FaceInfo> faceInfoList = new ArrayList<>();
                            int code = rill.detectFaces(arcSoftImageInfo, faceInfoList);
                            if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
                                FaceFeature faceFeature = new FaceFeature();
                                code = rill.extractFaceFeature(arcSoftImageInfo, new FaceInfo(faceInfoList.get(0)), faceFeature);
                                if (code == ErrorInfo.MOK) {
                                    FaceFeatureMap.put(item.getName(), faceFeature);
                                    Log.i("ccccccccccc", "提取特征值成功了:" + item.getName());
                                } else {
                                    Log.i("ccccccccccc", "提取特征值失败:" + item.getName());
                                }
                            } else {
                                Log.i("ccccccccccc", "寻找人脸失败:" + item.getName());
                            }
                        } else {
                            Log.i("ccccccccccc", "构建图片失败:" + item.getName());
                        }
                    }
                    BaseBean baseBean = new BaseBean();
                    List<BaseBean.Data> list = new ArrayList<>();
                    baseBean.setList(list);
                    for (Map.Entry<String, FaceFeature> item : FaceFeatureMap.entrySet()) {
                        BaseBean.Data data = new BaseBean.Data();
                        data.setName(item.getKey());
                        data.setFeature(new String(Base64.encode(item.getValue().getFeatureData(), Base64.NO_WRAP)));
                        list.add(data);
                    }
                    MMKV.defaultMMKV().encode("value1", new Gson().toJson(baseBean));
                } else {
                    BaseBean baseBean = new Gson().fromJson(value, BaseBean.class);
                    for (BaseBean.Data item : baseBean.getList()) {
                        FaceFeatureMap.put(item.getName(), new FaceFeature(Base64.decode(item.getFeature().getBytes(), Base64.NO_WRAP)));
                    }
                }
                rgb.post(new Runnable() {
                    @Override
                    public void run() {
                        initThread();
                    }
                });
            }
        }).start();
    }

    protected void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    protected void showLongToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
