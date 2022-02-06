package com.dima;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;


public class CameraActivity extends ActionBarActivity {

    public static SurfaceView sv=null;
    public static CameraActivity C_A=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        C_A=this;
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // и без заголовка
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        sv = (SurfaceView) findViewById(R.id.surfaceView);
        WalkingIconService.Ser.timer_timout_perv();
        Log.d("TAG", "CamCreat");
    }

    @Override
    protected void onPause() {
        WalkingIconService.Ser.tim_bool=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WalkingIconService.Ser.releaseCamera();
        finish();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d("TAG", "CamDestroy");
        super.onDestroy();
        C_A=null;
    }
}
