package kz.mobicom.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TopUpFragment extends LogFragment {

    private SeekBar seek;
    private EditText amountInput;
    private TextView label;
    private RadioGroup methodGroup;
    private CheckBox receipt;
    private ToggleButton smsToggle;
    private boolean syncing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seek = view.findViewById(R.id.topUpSeek);
        amountInput = view.findViewById(R.id.topUpAmount);
        label = view.findViewById(R.id.topUpLabel);
        methodGroup = view.findViewById(R.id.methodGroup);
        receipt = view.findViewById(R.id.receiptCheck);
        smsToggle = view.findViewById(R.id.smsToggle);

        // SeekBar -> EditText
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                syncing = true;
                amountInput.setText(String.valueOf((progress + 1) * 100));
                syncing = false;
                updateLabel();
            }

            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });

        // EditText -> SeekBar
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!syncing) {
                    syncing = true;
                    seek.setProgress(Math.max(0, Math.min(99, readAmount() / 100 - 1)));
                    syncing = false;
                }
                updateLabel();
            }
        });

        amountInput.setText("1000");
        view.findViewById(R.id.topUpButton).setOnClickListener(v -> confirm());
    }

    private int readAmount() {
        String text = amountInput.getText().toString().trim();
        if (text.isEmpty() || text.length() > 7) return 0;
        return Integer.parseInt(text);
    }

    private void updateLabel() {
        label.setText(getString(R.string.topup_selected, Ui.money(readAmount())));
    }

    private String method() {
        int id = methodGroup.getCheckedRadioButtonId();
        if (id == R.id.payKaspi) return getString(R.string.pay_kaspi);
        if (id == R.id.payTerminal) return getString(R.string.pay_terminal);
        return getString(R.string.pay_card);
    }

    private void confirm() {
        int amount = readAmount();
        if (!Validators.isAmount(amount)) {
            amountInput.setError(getString(R.string.error_amount, Validators.MIN_AMOUNT, Validators.MAX_AMOUNT));
            return;
        }

        String message = getString(R.string.dialog_topup_message, Ui.money(amount), method());
        if (receipt.isChecked()) message += "\n" + getString(R.string.dialog_topup_receipt);
        if (smsToggle.isChecked()) message += "\n" + getString(R.string.dialog_topup_sms);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_topup_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_confirm, (d, w) -> {
                    Db.get(requireContext()).topUp(Prefs.getUserId(requireContext()), amount);
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_topup_done, Ui.money(amount)), Toast.LENGTH_SHORT).show();
                    ((MainActivity) requireActivity()).goTo(R.id.nav_home);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
