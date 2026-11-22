package kz.mobicom.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileFragment extends LogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Models.User u = Db.get(requireContext()).getUser(Prefs.getUserId(requireContext()));
        if (u != null) {
            ((TextView) view.findViewById(R.id.profileName)).setText(u.name);
            ((TextView) view.findViewById(R.id.profilePhone)).setText("+7 " + u.phone);
            ((TextView) view.findViewById(R.id.profileTariff)).setText(getString(R.string.home_tariff, u.tariffName));
        }

        // Баптаулар: SharedPreferences
        SwitchMaterial dark = view.findViewById(R.id.switchDark);
        dark.setChecked(Prefs.isDark(requireContext()));
        dark.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setDark(requireContext(), checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        CheckBox notify = view.findViewById(R.id.checkNotify);
        notify.setChecked(Prefs.isNotify(requireContext()));
        notify.setOnCheckedChangeListener((b, checked) -> Prefs.setNotify(requireContext(), checked));

        view.findViewById(R.id.buttonLanguage).setOnClickListener(v -> chooseLanguage());
        view.findViewById(R.id.buttonHistory).setOnClickListener(v ->
                ((MainActivity) requireActivity()).show(new HistoryFragment(), true));
        view.findViewById(R.id.buttonReminder).setOnClickListener(v ->
                new ReminderDialogFragment().show(getChildFragmentManager(), "reminder"));
        view.findViewById(R.id.buttonPromo).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PromoActivity.class)));
        view.findViewById(R.id.buttonRingtones).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RingtonesActivity.class)));

        // Жасырын (implicit) Intent-тер
        view.findViewById(R.id.buttonCall).setOnClickListener(v ->
                open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:611"))));
        view.findViewById(R.id.buttonShare).setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
            open(Intent.createChooser(send, getString(R.string.profile_share)));
        });

        view.findViewById(R.id.buttonLogout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_logout_title)
                        .setMessage(R.string.dialog_logout_message)
                        .setPositiveButton(R.string.profile_logout, (d, w) -> {
                            Prefs.setUserId(requireContext(), -1);
                            startActivity(new Intent(requireContext(), LoginActivity.class));
                            requireActivity().finish();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show());
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.error_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseLanguage() {
        String[] names = getResources().getStringArray(R.array.languages);
        String[] tags = {"kk", "ru"};
        int checked = Ui.isRu(requireContext()) ? 1 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_language)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[which]));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
