package com.example.mobicom;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Қалпына келтіру: пароль немесе нөмір ұмытылғанда SMS-ке ұқсас хабарландыру арқылы код келеді.
 * MODE_PASSWORD: нөмір -> код -> жаңа пароль.
 * MODE_PHONE:    аты -> код -> тіркелген нөмір көрсетіледі.
 */
public class RecoveryActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_PASSWORD = "password";
    public static final String MODE_PHONE = "phone";

    private static final String TAG = "MobiComLifecycle";
    private static final long RESEND_MS = 30 * 1000;

    private boolean passwordMode;
    private EditText keyInput, codeInput, newPasswordInput;
    private TextView info, result;
    private Button sendButton, confirmButton;
    private CheckBox showPassword;
    private CountDownTimer resendTimer;

    /** Код қай кілтке байланған: нөмір (пароль режимі) немесе «name:аты» (нөмір режимі). */
    private String codeKey = "";
    private String phone = "";                       // пароль режимі: 10 санды нөмір
    private Map<String, Object> cloudData;           // нөмір тек бұлтта болса, соның деректері
    private final List<String> foundPhones = new ArrayList<>();

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> deliverCode());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "RecoveryActivity.onCreate");
        setContentView(R.layout.activity_recovery);
        Ui.applyInsets(findViewById(R.id.recoveryRoot));
        findViewById(R.id.recoveryBack).setOnClickListener(v -> finish());

        passwordMode = !MODE_PHONE.equals(getIntent().getStringExtra(EXTRA_MODE));

        keyInput = findViewById(R.id.recoveryKey);
        codeInput = findViewById(R.id.recoveryCode);
        newPasswordInput = findViewById(R.id.recoveryNewPassword);
        info = findViewById(R.id.recoveryInfo);
        result = findViewById(R.id.recoveryResult);
        sendButton = findViewById(R.id.recoverySend);
        confirmButton = findViewById(R.id.recoveryConfirm);
        showPassword = findViewById(R.id.recoveryShow);

        ((TextView) findViewById(R.id.recoveryTitle)).setText(
                passwordMode ? R.string.recovery_password_title : R.string.recovery_phone_title);
        ((TextView) findViewById(R.id.recoveryHint)).setText(
                passwordMode ? R.string.recovery_password_hint : R.string.recovery_phone_hint);

        if (passwordMode) {
            keyInput.setHint("+7 700 123 45 67");
            keyInput.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            keyInput.setHint(R.string.hint_name);
            keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        }

        showPassword.setOnCheckedChangeListener((b, checked) -> {
            newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | (checked
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            newPasswordInput.setSelection(newPasswordInput.getText().length());
        });

        sendButton.setOnClickListener(v -> requestCode());
        confirmButton.setOnClickListener(v -> confirm());
    }

    // ------------------------------------------------------------ 1-қадам: іздеу және код жіберу
    private void requestCode() {
        String raw = keyInput.getText().toString().trim();
        if (passwordMode) {
            if (!Validators.isPhoneValid(raw)) {
                keyInput.setError(getString(R.string.error_phone));
                return;
            }
            phone = Validators.normalizePhone(raw);
            if (phone.equals(Validators.ADMIN_PHONE)) {
                keyInput.setError(getString(R.string.error_not_registered));
                return;
            }
            codeKey = phone;
            if (Db.get(this).findUserByPhone(phone) != -1) {
                cloudData = null;
                startDelivery();
                return;
            }
            setBusy(true);
            CloudSync.fetchUser(this, phone, (ok, data) -> {
                setBusy(false);
                if (data == null) {
                    keyInput.setError(getString(ok ? R.string.error_not_registered : R.string.error_cloud));
                    return;
                }
                cloudData = data;
                startDelivery();
            });
        } else {
            if (raw.length() < 2) {
                keyInput.setError(getString(R.string.error_name));
                return;
            }
            foundPhones.clear();
            foundPhones.addAll(Db.get(this).findPhonesByName(raw));
            codeKey = "name:" + raw.toLowerCase();
            setBusy(true);
            CloudSync.fetchByName(this, raw, (ok, users) -> {
                setBusy(false);
                for (Map<String, Object> u : users) {
                    String p = String.valueOf(u.get("phone"));
                    if (!foundPhones.contains(p)) foundPhones.add(p);
                }
                if (foundPhones.isEmpty()) {
                    keyInput.setError(getString(ok ? R.string.error_name_not_found : R.string.error_cloud));
                    return;
                }
                startDelivery();
            });
        }
    }

    private void startDelivery() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            deliverCode();
        }
    }

    /** Кодты «SMS» ретінде хабарландыру арқылы көрсетеді. Рұқсат болмаса, диалогта көрсетеді. */
    private void deliverCode() {
        String code = Notifier.newCode(codeKey);
        String message = getString(R.string.sms_text, code);
        if (Notifier.post(this, getString(R.string.sms_sender), message)) {
            Toast.makeText(this, R.string.toast_code_sent, Toast.LENGTH_LONG).show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sms_sender)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
        }
        info.setVisibility(View.VISIBLE);
        info.setText(R.string.recovery_code_sent);
        codeInput.setVisibility(View.VISIBLE);
        confirmButton.setVisibility(View.VISIBLE);
        if (passwordMode) {
            newPasswordInput.setVisibility(View.VISIBLE);
            showPassword.setVisibility(View.VISIBLE);
        }
        codeInput.requestFocus();
        startResendTimer();
    }

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();
        sendButton.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_MS, 1000) {
            @Override public void onTick(long left) {
                sendButton.setText(getString(R.string.btn_resend_code, left / 1000));
            }

            @Override public void onFinish() {
                sendButton.setEnabled(true);
                sendButton.setText(R.string.btn_send_code);
            }
        }.start();
    }

    // ------------------------------------------------------------ 2-қадам: кодты тексеру
    private void confirm() {
        String code = codeInput.getText().toString().trim();
        if (!Notifier.hasActiveCode(codeKey)) {
            codeInput.setError(getString(R.string.error_code_first));
            return;
        }
        if (!Notifier.isCodeCorrect(codeKey, code)) {
            codeInput.setError(getString(R.string.error_code));
            return;
        }

        if (passwordMode) {
            String password = newPasswordInput.getText().toString();
            if (!Validators.isPasswordValid(password)) {
                newPasswordInput.setError(getString(R.string.error_password_short, Validators.MIN_PASSWORD));
                return;
            }
            Db db = Db.get(this);
            int id = db.findUserByPhone(phone);
            if (id == -1 && cloudData != null) id = db.restoreUser(cloudData);   // тек бұлтта болса, осында қалпына келтіреміз
            db.setPassword(id, password);                                        // UPDATE + бұлтқа жіберу
            Toast.makeText(this, R.string.recovery_password_done, Toast.LENGTH_LONG).show();
            finish();
        } else {
            StringBuilder sb = new StringBuilder();
            for (String p : foundPhones) sb.append("+7 ").append(p).append('\n');
            result.setVisibility(View.VISIBLE);
            result.setText(getString(R.string.recovery_phone_result, sb.toString().trim()));
            codeInput.setVisibility(View.GONE);
            confirmButton.setVisibility(View.GONE);
            sendButton.setVisibility(View.GONE);
            keyInput.setEnabled(false);
        }
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        if (busy) sendButton.setText(R.string.login_wait);
        else sendButton.setText(R.string.btn_send_code);
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "RecoveryActivity.onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "RecoveryActivity.onStop"); }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
        Log.d(TAG, "RecoveryActivity.onDestroy");
    }
}

