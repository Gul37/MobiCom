package kz.mobicom.app;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MobiComLifecycle", "AdminActivity.onCreate");
        setContentView(R.layout.activity_admin);
        Ui.applyInsets(findViewById(R.id.adminRoot));

        MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        BottomNavigationView nav = findViewById(R.id.adminNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            if (id == R.id.admin_nav_stats) fragment = new AdminStatsFragment();
            else if (id == R.id.admin_nav_users) fragment = new AdminUsersFragment();
            else if (id == R.id.admin_nav_tariffs) fragment = new AdminTariffsFragment();
            else fragment = new AdminServicesFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.adminContainer, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) nav.setSelectedItemId(R.id.admin_nav_stats);
    }
}
