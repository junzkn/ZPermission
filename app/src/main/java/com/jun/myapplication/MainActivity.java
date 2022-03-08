package com.jun.myapplication;


import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

import com.jun.permission.*;
import com.jun.permission.annotation.OnMPermissionDenied;
import com.jun.permission.annotation.OnMPermissionGranted;
import com.jun.permission.annotation.OnMPermissionNeverAskAgain;


public class MainActivity extends AppCompatActivity {


    private static final int PERMISSION_REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
        ZPermission.with(this)
                .permissions(permissions)
                .requestCode(PERMISSION_REQUEST_CODE)
                .request();

    }

    @OnMPermissionNeverAskAgain(PERMISSION_REQUEST_CODE)
    public void OnMPermissionNeverAskAgain() {
        Toast.makeText(this, "OnMPermissionNeverAskAgain", Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionGranted(PERMISSION_REQUEST_CODE)
    public void OnMPermissionGranted() {
        Toast.makeText(this, "OnMPermissionGranted", Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionDenied(PERMISSION_REQUEST_CODE)
    public void OnMPermissionDenied() {
        Toast.makeText(this, "OnMPermissionDenied", Toast.LENGTH_SHORT).show();
    }


}