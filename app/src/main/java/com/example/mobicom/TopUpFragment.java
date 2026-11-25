package com.example.mobicom;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TopUpFragment extends LogFragment {

    private SeekBar seek;
    private EditText amountInput, cardNumber, cardExpiry, cardCvv, cardHolder;
    private TextView label, cardBrand, bankFixed, terminalInfo;
    private RadioGroup methodGroup;
    private CheckBox receipt;
    private ToggleButton smsToggle;
    private Spinner bankSpinner;
    private View cardForm;
    private Button payButton;
    private boolean syncing = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** SMS-хабарлама қосылғанда Android 13+ хабарландыруға рұқсат сұрайды. */
    private final ActivityResultLauncher<String> notifyPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && smsToggle != null) smsToggle.setChecked(false);
            });

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
        cardForm = view.findViewById(R.id.cardForm);
        bankSpinner = view.findViewById(R.id.bankSpinner);
        bankFixed = view.findViewById(R.id.bankFixed);
        cardNumber = view.findViewById(R.id.cardNumber);
        cardBrand = view.findViewById(R.id.cardBrand);
        cardExpiry = view.findViewById(R.id.cardExpiry);
        cardCvv = view.findViewById(R.id.cardCvv);
        cardHolder = view.findViewById(R.id.cardHolder);
        terminalInfo = view.findViewById(R.id.terminalInfo);
        payButton = view.findViewById(R.id.topUpButton);

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
        amountInput.addTextChangedListener(new SimpleWatcher() {
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

        // Банк тізімі (Spinner)
        ArrayAdapter<CharSequence> banks = ArrayAdapter.createFromResource(requireContext(),
                R.array.banks, android.R.layout.simple_spinner_item);
        banks.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(banks);

        // Төлем тәсілі ауысқанда форма өзгереді
        methodGroup.setOnCheckedChangeListener((g, id) -> updateMethodUi());
        updateMethodUi();

        // Карта нөмірі: 4 саннан топтап жазады, карта түрін көрсетеді
        cardNumber.addTextChangedListener(new SimpleWatcher() {
            private boolean busy = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (busy) return;
                busy = true;
                String digits = Validators.digitsOnly(s.toString());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) sb.append(' ');
                    sb.append(digits.charAt(i));
                }
                if (!sb.toString().equals(s.toString())) {
                    s.replace(0, s.length(), sb.toString());
                }
                cardBrand.setText(Validators.cardBrand(digits));
                busy = false;
            }
        });

        // Мерзімі: «1228» жазғанда автоматты түрде «12/28» болады
        cardExpiry.addTextChangedListener(new SimpleWatcher() {
            private boolean busy = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (busy) return;
                busy = true;
                String digits = Validators.digitsOnly(s.toString());
                String text = digits.length() > 2 ? digits.substring(0, 2) + "/" + digits.substring(2) : digits;
                if (!text.equals(s.toString())) s.replace(0, s.length(), text);
                busy = false;
            }
        });

        smsToggle.setOnCheckedChangeListener((b, checked) -> {
            if (checked && Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(requireContext())) {
                notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        payButton.setOnClickListener(v -> confirm());
    }

    // ------------------------------------------------------------ төлем тәсілі
    private boolean isCardMethod() {
        return methodGroup.getCheckedRadioButtonId() != R.id.payTerminal;
    }

    private boolean isKaspi() {
        return methodGroup.getCheckedRadioButtonId() == R.id.payKaspi;
    }

    private void updateMethodUi() {
        boolean card = isCardMethod();
        boolean kaspi = isKaspi();
        cardForm.setVisibility(card ? View.VISIBLE : View.GONE);
        bankSpinner.setVisibility(kaspi ? View.GONE : View.VISIBLE);
        bankFixed.setVisibility(kaspi ? View.VISIBLE : View.GONE);
        bankFixed.setText(R.string.bank_kaspi);
        terminalInfo.setVisibility(card ? View.GONE : View.VISIBLE);
        if (!card) {
            Models.User u = Db.get(requireContext()).getUser(Prefs.getUserId(requireContext()));
            terminalInfo.setText(getString(R.string.terminal_info, u == null ? "" : u.phone));
        }
    }

    /** Банктің атауы: Kaspi таңдалса «Kaspi Bank», әйтпесе Spinner-дегі банк. */
    private String bankName() {
        if (isKaspi()) return getString(R.string.bank_kaspi);
        return String.valueOf(bankSpinner.getSelectedItem());
    }

    private int readAmount() {
        String text = amountInput.getText().toString().trim();
        if (text.isEmpty() || text.length() > 7) return 0;
        return Integer.parseInt(text);
    }

    private void updateLabel() {
        label.setText(getString(R.string.topup_selected, Ui.money(readAmount())));
    }

    // ------------------------------------------------------------ тексеру
    /** Карта деректерін тексереді. Қате болса, сол өріске хабар шығарады. */
    private boolean cardIsValid() {
        if (!Validators.isCardNumber(cardNumber.getText().toString())) {
            cardNumber.setError(getString(R.string.error_card_number));
            return false;
        }
        if (!Validators.isExpiry(cardExpiry.getText().toString())) {
            cardExpiry.setError(getString(R.string.error_card_expiry));
            return false;
        }
        if (!Validators.isCvv(cardCvv.getText().toString())) {
            cardCvv.setError(getString(R.string.error_card_cvv));
            return false;
        }
        if (!Validators.isCardHolder(cardHolder.getText().toString())) {
            cardHolder.setError(getString(R.string.error_card_holder));
            return false;
        }
        return true;
    }

    private void confirm() {
        int amount = readAmount();
        if (!Validators.isAmount(amount)) {
            amountInput.setError(getString(R.string.error_amount, Validators.MIN_AMOUNT, Validators.MAX_AMOUNT));
            return;
        }
        if (isCardMethod() && !cardIsValid()) return;

        // Тарихқа тек банк атауы мен карта соңғы 4 саны жазылады. Толық нөмір ешқайда сақталмайды
        String digits = Validators.digitsOnly(cardNumber.getText().toString());
        final String methodTitle = isCardMethod()
                ? bankName() + " •••• " + digits.substring(digits.length() - 4)
                : getString(R.string.pay_terminal);

        String message = getString(R.string.dialog_topup_message, Ui.money(amount), methodTitle);
        if (receipt.isChecked()) message += "\n" + getString(R.string.dialog_topup_receipt);
        if (smsToggle.isChecked()) message += "\n" + getString(R.string.dialog_topup_sms);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_topup_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_confirm, (d, w) -> process(amount, methodTitle))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Төлемді «өңдеу» (1,2 секунд), содан кейін балансқа қосу. */
    private void process(int amount, String methodTitle) {
        payButton.setEnabled(false);
        payButton.setText(R.string.topup_processing);
        handler.postDelayed(() -> {
            if (!isAdded()) return;
            payButton.setEnabled(true);
            payButton.setText(R.string.btn_pay);
            finish(amount, methodTitle);
        }, 1200);
    }

    private void finish(int amount, String methodTitle) {
        int userId = Prefs.getUserId(requireContext());
        Db db = Db.get(requireContext());
        db.topUp(userId, amount, methodTitle);
        Models.User u = db.getUser(userId);

        // Карта деректерін формадан тазалаймыз
        cardNumber.setText("");
        cardExpiry.setText("");
        cardCvv.setText("");
        cardHolder.setText("");

        Toast.makeText(requireContext(),
                getString(R.string.toast_topup_done, Ui.money(amount)), Toast.LENGTH_SHORT).show();

        if (smsToggle.isChecked() && Prefs.isNotify(requireContext()) && u != null) {
            Notifier.post(requireContext(), getString(R.string.sms_sender),
                    getString(R.string.sms_topup_text, Ui.money(amount), Ui.money(u.balance)));
        }

        if (receipt.isChecked() && u != null) {
            showReceipt(amount, methodTitle, u);
        } else {
            ((MainActivity) requireActivity()).goTo(R.id.nav_home);
        }
    }

    // ------------------------------------------------------------ чек: файлға жазу және оқу
    private void showReceipt(int amount, String methodTitle, Models.User u) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "receipt_" + stamp + ".txt";
        String text = getString(R.string.receipt_text,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                u.phone, methodTitle, Ui.money(amount), Ui.money(u.balance));

        String shown;
        try {
            FileStore.saveInternal(requireContext(), fileName, text);        // файлға жазу
            shown = FileStore.readInternal(requireContext(), fileName)       // файлдан қайта оқу
                    + "\n\n" + getString(R.string.receipt_saved, fileName);
        } catch (IOException e) {
            shown = text;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.receipt_title)
                .setMessage(shown)
                .setPositiveButton(R.string.action_ok, (d, w) -> {
                    if (isAdded()) ((MainActivity) requireActivity()).goTo(R.id.nav_home);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    /** TextWatcher-дің қажет емес әдістерін жасырады. */
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
    }
}

