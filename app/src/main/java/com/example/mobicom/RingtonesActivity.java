package com.example.mobicom;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/** Рингтон плеері: тізімнен таңдау, ойнату/кідірту, жылжыту (SeekBar), ±10 секунд. */
public class RingtonesActivity extends AppCompatActivity {

    private static final int STEP_MS = 10_000;

    private final int[] raws = {R.raw.ringtone_waltz, R.raw.ringtone_wave, R.raw.ringtone_pulse};
    private final int[] buttonIds = {R.id.ring1, R.id.ring2, R.id.ring3};
    private final Button[] buttons = new Button[3];

    private MediaPlayer player;
    private int current = -1;

    private TextView title, timeText, totalText;
    private SeekBar seek;
    private ImageButton playButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Әр 300 мс сайын SeekBar мен уақытты жаңартады. */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                seek.setProgress(player.getCurrentPosition());
                timeText.setText(format(player.getCurrentPosition()));
                handler.postDelayed(this, 300);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MobiComLifecycle", "RingtonesActivity.onCreate");
        setContentView(R.layout.activity_ringtones);
        Ui.applyInsets(findViewById(R.id.ringRoot));
        findViewById(R.id.ringBack).setOnClickListener(v -> finish());

        title = findViewById(R.id.playerTitle);
        timeText = findViewById(R.id.playerTime);
        totalText = findViewById(R.id.playerTotal);
        seek = findViewById(R.id.playerSeek);
        playButton = findViewById(R.id.playerPlay);

        String[] names = getResources().getStringArray(R.array.ringtone_names);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            buttons[i] = findViewById(buttonIds[i]);
            buttons[i].setText(names[i]);
            buttons[i].setOnClickListener(v -> select(index));
        }

        playButton.setOnClickListener(v -> togglePlay());
        findViewById(R.id.playerBack).setOnClickListener(v -> jump(-STEP_MS));
        findViewById(R.id.playerForward).setOnClickListener(v -> jump(STEP_MS));

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) timeText.setText(format(progress));
            }

            @Override public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (player != null) player.seekTo(bar.getProgress());   // жібін босатқанда сол жерден ойнайды
            }
        });
    }

    // ------------------------------------------------------------ әуенді таңдау
    private void select(int index) {
        if (index == current && player != null) {   // сол әуенді қайта басса, ойнату/кідірту ауысады
            togglePlay();
            return;
        }
        releasePlayer();

        player = MediaPlayer.create(this, raws[index]);
        if (player == null) {
            Toast.makeText(this, R.string.ring_error, Toast.LENGTH_SHORT).show();
            return;
        }
        current = index;
        player.setOnCompletionListener(mp -> {
            handler.removeCallbacks(ticker);
            seek.setProgress(0);
            timeText.setText(format(0));
            updateUi();
        });

        String[] names = getResources().getStringArray(R.array.ringtone_names);
        title.setText(names[index]);
        seek.setEnabled(true);
        seek.setMax(player.getDuration());
        seek.setProgress(0);
        timeText.setText(format(0));
        totalText.setText(format(player.getDuration()));

        player.start();
        handler.post(ticker);
        updateUi();
    }

    // ------------------------------------------------------------ ойнату, кідірту, жылжыту
    private void togglePlay() {
        if (player == null) {
            Toast.makeText(this, R.string.ring_choose, Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            handler.removeCallbacks(ticker);
        } else {
            player.start();
            handler.post(ticker);
        }
        updateUi();
    }

    /** ±10 секундқа жылжыту. */
    private void jump(int deltaMs) {
        if (player == null) return;
        int target = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + deltaMs));
        player.seekTo(target);
        seek.setProgress(target);
        timeText.setText(format(target));
    }

    private void updateUi() {
        boolean playing = player != null && player.isPlaying();
        playButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        for (int i = 0; i < 3; i++) {
            buttons[i].setAlpha(i == current ? 1f : 0.6f);
        }
    }

    private static String format(int ms) {
        int sec = ms / 1000;
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
    }

    private void releasePlayer() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            player.release();       // ресурсты босату
            player = null;
        }
        current = -1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {   // экраннан шыққанда әуен кідіреді
            player.pause();
            handler.removeCallbacks(ticker);
            updateUi();
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}

