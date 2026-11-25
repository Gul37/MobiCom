package com.example.mobicom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SplashActivity.onCreate");
        setContentView(R.layout.activity_splash);
        Ui.applyInsets(findViewById(R.id.splashRoot));

        View logo = findViewById(R.id.splashLogo);
        View title = findViewById(R.id.splashTitle);
        View slogan = findViewById(R.id.splashSlogan);

        // Property animation: логотип үлкейеді, мәтін біртіндеп көрінеді
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        title.setAlpha(0f);
        slogan.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(700)
                .setInterpolator(new OvershootInterpolator()).start();
        title.animate().alpha(1f).setStartDelay(400).setDuration(500).start();
        slogan.animate().alpha(1f).setStartDelay(700).setDuration(500).start();

        handler.postDelayed(() -> {
            Class<?> next = Prefs.getUserId(this) == -1 ? LoginActivity.class : MainActivity.class;
            startActivity(new Intent(this, next));
            finish();
        }, 1600);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
        Log.d(TAG, "SplashActivity.onDestroy");
    }
}
