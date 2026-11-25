package com.example.mobicom;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


/** Кіру және тіркелу: нөмір + пароль. Админ үшін жеке пароль (Prefs). */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";

    private boolean registerMode = false;
    private EditText nameInput, phoneInput, passwordInput;
    private TextView title, hint;
    private Button mainButton, switchButton, forgotPasswordButton, forgotPhoneButton;
    private CheckBox terms, showPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LoginActivity.onCreate");
        setContentView(R.layout.activity_login);
        Ui.applyInsets(findViewById(R.id.loginRoot));

        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        title = findViewById(R.id.loginTitle);
        hint = findViewById(R.id.loginHint);
        mainButton = findViewById(R.id.loginButton);
        switchButton = findViewById(R.id.switchMode);
        forgotPasswordButton = findViewById(R.id.forgotPassword);
        forgotPhoneButton = findViewById(R.id.forgotPhone);
        terms = findViewById(R.id.termsCheck);
        showPassword = findViewById(R.id.showPassword);

        // Нөмірге 11 саннан артық енгізуге болмайды: 12-санды жазғанда қате шығады
        phoneInput.setFilters(new InputFilter[]{new PhoneLimitFilter()});

        showPassword.setOnCheckedChangeListener((b, checked) -> {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | (checked
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            passwordInput.setSelection(passwordInput.getText().length());
        });

        applyMode();
        switchButton.setOnClickListener(v -> {
            registerMode = !registerMode;
            applyMode();
        });
        mainButton.setOnClickListener(v -> submit());
        forgotPasswordButton.setOnClickListener(v -> openRecovery(RecoveryActivity.MODE_PASSWORD));
        forgotPhoneButton.setOnClickListener(v -> openRecovery(RecoveryActivity.MODE_PHONE));
    }

    private void openRecovery(String mode) {
        Intent i = new Intent(this, RecoveryActivity.class);
        i.putExtra(RecoveryActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    /** InputFilter: сандар саны 11-ден асатын болса, жаңа таңбаны қабылдамайды. */
    private class PhoneLimitFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            String result = dest.subSequence(0, dstart).toString()
                    + source.subSequence(start, end)
                    + dest.subSequence(dend, dest.length());
            if (Validators.isPhoneTooLong(result)) {
                phoneInput.setError(getString(R.string.error_phone_long));
                return "";     // артық санды жазуға жол бермейміз
            }
            return null;       // өзгеріссіз қабылдаймыз
        }
    }

    /** Енгізілген нөмірді тексереді. Дұрыс болса, 10 санды нөмірді қайтарады, әйтпесе null. */
    private String checkedPhone() {
        String raw = phoneInput.getText().toString();
        if (Validators.isPhoneTooLong(raw)) {
            phoneInput.setError(getString(R.string.error_phone_long));
            return null;
        }
        if (!Validators.isPhoneValid(raw)) {
            phoneInput.setError(getString(R.string.error_phone));
            return null;
        }
        return Validators.normalizePhone(raw);
    }

    // ------------------------------------------------------------ кіру / тіркелу
    private void submit() {
        String name = nameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (registerMode && name.length() < 2) {
            nameInput.setError(getString(R.string.error_name));
            return;
        }
        String phone = checkedPhone();
        if (phone == null) return;

        if (registerMode) {
            if (!Validators.isPasswordValid(password)) {
                passwordInput.setError(getString(R.string.error_password_short, Validators.MIN_PASSWORD));
                return;
            }
            if (!terms.isChecked()) {
                Toast.makeText(this, R.string.error_terms, Toast.LENGTH_SHORT).show();
                return;
            }
            register(name, phone, password);
        } else {
            if (password.isEmpty()) {
                passwordInput.setError(getString(R.string.error_password_empty));
                return;
            }
            login(phone, password);
        }
    }

    private void login(String phone, String password) {
        // Админнің нөмірі мен жеке паролі
        if (phone.equals(Validators.ADMIN_PHONE)) {
            if (password.equals(Prefs.getAdminPassword(this))) {
                startActivity(new Intent(this, AdminActivity.class));
            } else {
                passwordInput.setError(getString(R.string.error_password_wrong));
            }
            return;
        }

        Db db = Db.get(this);
        int id = db.checkLogin(phone, password);
        if (id >= 0) {
            Models.User user = db.getUser(id);
            Toast.makeText(this, getString(R.string.toast_welcome_name, user.name), Toast.LENGTH_SHORT).show();
            openMain(id);
            return;
        }
        if (id == -2) {
            passwordInput.setError(getString(R.string.error_password_wrong));
            return;
        }

        // Осы телефонда жоқ: бұлттан іздейміз (басқа телефоннан тіркелген болуы мүмкін)
        setBusy(true);
        CloudSync.fetchUser(this, phone, (ok, data) -> {
            setBusy(false);
            if (data == null) {
                phoneInput.setError(getString(ok ? R.string.error_not_registered : R.string.error_cloud));
                return;
            }
            Object saved = data.get("password");
            String cloudPassword = saved == null || String.valueOf(saved).isEmpty()
                    ? Db.DEMO_PASSWORD : String.valueOf(saved);
            if (!cloudPassword.equals(password)) {
                passwordInput.setError(getString(R.string.error_password_wrong));
                return;
            }
            int restored = db.restoreUser(data);
            Toast.makeText(this, getString(R.string.toast_welcome_name, String.valueOf(data.get("name"))),
                    Toast.LENGTH_SHORT).show();
            openMain(restored);
        });
    }

    /** Тіркелу: нөмір ешқайда (телефонда да, бұлтта да) тіркелмеген болуы керек. */
    private void register(String name, String phone, String password) {
        Db db = Db.get(this);
        if (phone.equals(Validators.ADMIN_PHONE) || db.findUserByPhone(phone) != -1) {
            phoneInput.setError(getString(R.string.error_registered));
            return;
        }
        setBusy(true);
        CloudSync.fetchUser(this, phone, (ok, data) -> {
            setBusy(false);
            if (ok && data != null) {          // басқа телефоннан тіркелген
                phoneInput.setError(getString(R.string.error_registered));
                return;
            }
            int newId = db.createUser(name, phone, password);   // INSERT + бұлтқа жіберу
            Toast.makeText(this, R.string.toast_registered, Toast.LENGTH_LONG).show();
            openMain(newId);
        });
    }

    private void openMain(int userId) {
        Prefs.setUserId(this, userId);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setBusy(boolean busy) {
        mainButton.setEnabled(!busy);
        mainButton.setText(busy ? getString(R.string.login_wait)
                : getString(registerMode ? R.string.btn_register : R.string.btn_login));
    }

    private void applyMode() {
        int reg = registerMode ? View.VISIBLE : View.GONE;
        int log = registerMode ? View.GONE : View.VISIBLE;
        nameInput.setVisibility(reg);
        terms.setVisibility(reg);
        forgotPasswordButton.setVisibility(log);
        forgotPhoneButton.setVisibility(log);
        title.setText(registerMode ? R.string.register_title : R.string.login_title);
        hint.setText(registerMode ? R.string.register_hint : R.string.login_hint);
        mainButton.setText(registerMode ? R.string.btn_register : R.string.btn_login);
        switchButton.setText(registerMode ? R.string.switch_to_login : R.string.switch_to_register);
        passwordInput.setError(null);
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "LoginActivity.onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "LoginActivity.onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "LoginActivity.onDestroy"); }
}
