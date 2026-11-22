package kz.mobicom.app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RingtonesActivity extends AppCompatActivity {

    private final int[] raws = {R.raw.ringtone_waltz, R.raw.ringtone_wave, R.raw.ringtone_pulse2};
    private final int[] buttonIds = {R.id.ring1, R.id.ring2, R.id.ring3};
    private final Button[] buttons = new Button[3];

    private MediaPlayer player;
    private int playing = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MobiComLifecycle", "RingtonesActivity.onCreate");
        setContentView(R.layout.activity_ringtones);
        Ui.applyInsets(findViewById(R.id.ringRoot));
        findViewById(R.id.ringBack).setOnClickListener(v -> finish());

        for (int i = 0; i < 3; i++) {
            final int index = i;
            buttons[i] = findViewById(buttonIds[i]);
            buttons[i].setOnClickListener(v -> toggle(index));
        }
        refresh();
    }

    private void toggle(int index) {
        boolean same = playing == index;
        stopPlayer();
        if (same) return;

        player = MediaPlayer.create(this, raws[index]);
        player.setOnCompletionListener(mp -> {
            stopPlayer();
            refresh();
        });
        player.start();
        playing = index;
        refresh();
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        playing = -1;
        refresh();
    }

    private void refresh() {
        String[] names = getResources().getStringArray(R.array.ringtone_names);
        for (int i = 0; i < 3; i++) {
            if (buttons[i] == null) continue;
            buttons[i].setText((playing == i ? getString(R.string.ring_stop) : getString(R.string.ring_play))
                    + ": " + names[i]);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();   // экраннан шыққанда дыбыс тоқтайды, ресурс босатылады
    }
}

