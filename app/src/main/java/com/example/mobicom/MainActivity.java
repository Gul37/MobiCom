package kz.mobicom.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity.onCreate");

        // Кірмеген (немесе дерекқор тазаланған) болса, Кіру экранына жібереміз
        if (Prefs.getUserId(this) == -1 || Db.get(this).getUser(Prefs.getUserId(this)) == null) {
            Prefs.setUserId(this, -1);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        Ui.applyInsets(findViewById(R.id.main));

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            if (id == R.id.nav_home) fragment = new HomeFragment();
            else if (id == R.id.nav_tariffs) fragment = new TariffsFragment();
            else if (id == R.id.nav_services) fragment = new ServicesFragment();
            else if (id == R.id.nav_topup) fragment = new TopUpFragment();
            else fragment = new ProfileFragment();
            show(fragment, false);
            return true;
        });

        if (savedInstanceState == null) {
            show(new HomeFragment(), false);
        }
    }

    /** Фрагментті ауыстырады. addToBackStack = true болса, "Артқа" түймесі алдыңғысына қайтарады. */
    public void show(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        if (!addToBackStack) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction()
                .replace(R.id.container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    /** Басқа фрагменттерден төменгі менюдегі бөлімге өту. */
    public void goTo(int navId) {
        bottomNav.setSelectedItemId(navId);
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "MainActivity.onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "MainActivity.onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "MainActivity.onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "MainActivity.onStop"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "MainActivity.onRestart"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "MainActivity.onDestroy"); }
}
