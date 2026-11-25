package com.example.mobicom;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

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
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_admin_password) {
                changeAdminPassword();
                return true;
            }
            return false;
        });

        BottomNavigationView nav = findViewById(R.id.adminNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            if (id == R.id.admin_nav_stats) fragment = new AdminStatsFragment();
            else if (id == R.id.admin_nav_users) fragment = new AdminUsersFragment();
            else if (id == R.id.admin_nav_tariffs) fragment = new AdminTariffsFragment();
            else if (id == R.id.admin_nav_cloud) fragment = new AdminCloudFragment();
            else fragment = new AdminServicesFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.adminContainer, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) nav.setSelectedItemId(R.id.admin_nav_stats);
    }

    /** Админнің жеке паролін өзгерту: ескі пароль дұрыс болуы керек. */
    private void changeAdminPassword() {
        AdminDialogs.form(this, R.string.admin_change_password,
                new String[]{getString(R.string.admin_password_current), getString(R.string.hint_new_password)},
                new int[]{InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD},
                null, v -> {
                    if (!v[0].equals(Prefs.getAdminPassword(this))) {
                        Toast.makeText(this, R.string.error_password_wrong, Toast.LENGTH_SHORT).show();
                    } else if (!Validators.isPasswordValid(v[1])) {
                        Toast.makeText(this, getString(R.string.error_password_short, Validators.MIN_PASSWORD),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Prefs.setAdminPassword(this, v[1]);
                        Toast.makeText(this, R.string.admin_password_changed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

