package kz.mobicom.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TariffDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TARIFF = "extra_tariff";
    public static final String EXTRA_CURRENT_ID = "extra_current_id";
    /** Баланс жетпегенде шақырған экранға қайтарылатын нәтиже коды. */
    public static final int RESULT_NEED_TOPUP = RESULT_FIRST_USER;

    private static final String TAG = "MobiComLifecycle";

    private Models.Tariff tariff;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "TariffDetailActivity.onCreate");
        setContentView(R.layout.activity_tariff_detail);
        Ui.applyInsets(findViewById(R.id.detailRoot));

        tariff = (Models.Tariff) getIntent().getSerializableExtra(EXTRA_TARIFF);
        int currentId = getIntent().getIntExtra(EXTRA_CURRENT_ID, -1);
        if (tariff == null) {
            finish();
            return;
        }

        findViewById(R.id.detailBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.detailName)).setText(tariff.name);
        ((TextView) findViewById(R.id.detailPrice)).setText(getString(R.string.price_per_month, tariff.price));
        ((TextView) findViewById(R.id.detailDesc)).setText(tariff.desc(Ui.isRu(this)));
        ((TextView) findViewById(R.id.detailInternet)).setText(getString(R.string.detail_internet, tariff.internetGb));
        ((TextView) findViewById(R.id.detailMinutes)).setText(getString(R.string.detail_minutes, tariff.minutes));
        ((TextView) findViewById(R.id.detailSms)).setText(getString(R.string.detail_sms, tariff.sms));

        Button button = findViewById(R.id.detailButton);
        if (tariff.id == currentId) {
            button.setEnabled(false);
            button.setText(R.string.tariff_current);
        } else {
            button.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_switch_title)
                    .setMessage(getString(R.string.dialog_switch_message, tariff.name, tariff.price))
                    .setPositiveButton(R.string.action_confirm, (d, w) -> doSwitch())
                    .setNegativeButton(R.string.action_cancel, null)
                    .show());
        }
    }

    private void doSwitch() {
        boolean ok = Db.get(this).switchTariff(Prefs.getUserId(this), tariff);
        if (ok) {
            Toast.makeText(this, R.string.toast_tariff_switched, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_NEED_TOPUP);
        }
        finish();
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "TariffDetailActivity.onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "TariffDetailActivity.onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "TariffDetailActivity.onDestroy"); }
}
