package kz.mobicom.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class MobiComApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.isDark(this)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
