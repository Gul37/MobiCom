package com.example.mobicom;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PromoActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";

    private VideoView video;
    private int position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PromoActivity.onCreate");
        setContentView(R.layout.activity_promo);
        Ui.applyInsets(findViewById(R.id.promoRoot));

        if (savedInstanceState != null) position = savedInstanceState.getInt("position", 0);

        findViewById(R.id.promoBack).setOnClickListener(v -> finish());

        video = findViewById(R.id.promoVideo);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.promo));
        video.setOnPreparedListener(mp -> {
            if (position > 0) video.seekTo(position);
            video.start();
        });
        video.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, R.string.promo_error, Toast.LENGTH_LONG).show();
            return true;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "PromoActivity.onPause");
        position = video.getCurrentPosition();
        video.pause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position", position);
    }

    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "PromoActivity.onDestroy"); }
}
