package kz.mobicom.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";
    private static final String DEMO_CODE = "1234";

    private boolean registerMode = false;
    private EditText nameInput, phoneInput, codeInput;
    private TextView title, hint;
    private Button mainButton, switchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LoginActivity.onCreate");
        setContentView(R.layout.activity_login);
        Ui.applyInsets(findViewById(R.id.loginRoot));

        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        codeInput = findViewById(R.id.codeInput);
        title = findViewById(R.id.loginTitle);
        hint = findViewById(R.id.loginHint);
        mainButton = findViewById(R.id.loginButton);
        switchButton = findViewById(R.id.switchMode);
        CheckBox terms = findViewById(R.id.termsCheck);

        applyMode();
        switchButton.setOnClickListener(v -> {
            registerMode = !registerMode;
            applyMode();
        });

        mainButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = Validators.normalizePhone(phoneInput.getText().toString());
            String code = codeInput.getText().toString().trim();

            if (registerMode && name.length() < 2) {
                nameInput.setError(getString(R.string.error_name));
            } else if (!Validators.isPhone(phone)) {
                phoneInput.setError(getString(R.string.error_phone));
            } else if (!code.equals(DEMO_CODE)) {
                codeInput.setError(getString(R.string.error_code));
            } else if (!terms.isChecked()) {
                Toast.makeText(this, R.string.error_terms, Toast.LENGTH_SHORT).show();
            } else if (!registerMode && phone.equals(Validators.ADMIN_PHONE)) {
                startActivity(new Intent(this, AdminActivity.class));
            } else {
                enter(name, phone);
            }
        });
    }

    /** Кіру: нөмір базада болуы керек. Тіркелу: нөмір базада болмауы керек. */
    private void enter(String name, String phone) {
        Db db = Db.get(this);
        int id = db.findUserByPhone(phone);

        if (registerMode) {
            if (id != -1) {
                phoneInput.setError(getString(R.string.error_registered));
                return;
            }
            id = db.createUser(name, phone);
            Toast.makeText(this, R.string.toast_registered, Toast.LENGTH_LONG).show();
        } else {
            if (id == -1) {
                phoneInput.setError(getString(R.string.error_not_registered));
                return;
            }
            Models.User user = db.getUser(id);
            Toast.makeText(this, getString(R.string.toast_welcome_name, user.name), Toast.LENGTH_SHORT).show();
        }

        Prefs.setUserId(this, id);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void applyMode() {
        nameInput.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        title.setText(registerMode ? R.string.register_title : R.string.login_title);
        hint.setText(registerMode ? R.string.register_hint : R.string.login_hint);
        mainButton.setText(registerMode ? R.string.btn_register : R.string.btn_login);
        switchButton.setText(registerMode ? R.string.switch_to_login : R.string.switch_to_register);
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "LoginActivity.onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "LoginActivity.onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "LoginActivity.onDestroy"); }
}
