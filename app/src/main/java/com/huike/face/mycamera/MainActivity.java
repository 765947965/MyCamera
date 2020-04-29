package com.huike.face.mycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private CameraManager cameraManagerRgb;
    private CameraManager cameraManagerIr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                || !(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            //没有权限，申请权限
            String[] permissions = {Manifest.permission.CAMERA};
            //申请权限，其中RC_PERMISSION是权限申请码，用来标志权限申请的
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 10001);
        } else {
            initCamera();
        }
    }

    private void initCamera() {
        cameraManagerRgb = new CameraManager();
        cameraManagerRgb.setPreviewDisplay((CameraPreview) findViewById(R.id.cameraPreviewRgb));
        cameraManagerIr = new CameraManager();
        cameraManagerIr.setPreviewDisplay((CameraPreview) findViewById(R.id.cameraPreviewIr));
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
            initCamera();
            openManager();
        }
    }
}
