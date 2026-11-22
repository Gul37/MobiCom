package kz.mobicom.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeFragment extends LogFragment {

    private View root;
    private double shownBalance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;

        MaterialToolbar toolbar = view.findViewById(R.id.homeToolbar);
        toolbar.inflateMenu(R.menu.menu_home);
        toolbar.setOnMenuItemClickListener(this::onMenu);

        view.findViewById(R.id.balanceCard).startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down));

        view.findViewById(R.id.btnTopUp).setOnClickListener(v ->
                ((MainActivity) requireActivity()).goTo(R.id.nav_topup));
        view.findViewById(R.id.btnTariffs).setOnClickListener(v ->
                ((MainActivity) requireActivity()).goTo(R.id.nav_tariffs));
    }

    @Override
    public void onResume() {
        super.onResume();
        Models.User u = Db.get(requireContext()).getUser(Prefs.getUserId(requireContext()));
        if (u == null) return;

        ((TextView) root.findViewById(R.id.homeGreeting)).setText(getString(R.string.home_greeting, u.name));
        ((TextView) root.findViewById(R.id.homePhone)).setText("+7 " + u.phone);
        ((TextView) root.findViewById(R.id.homeTariff)).setText(getString(R.string.home_tariff, u.tariffName));

        // ValueAnimator: баланс саны жаңа мәнге дейін санайды
        ValueAnimator animator = ValueAnimator.ofFloat((float) shownBalance, (float) u.balance);
        animator.setDuration(700);
        animator.addUpdateListener(a -> ((TextView) root.findViewById(R.id.balanceText))
                .setText(Ui.money((float) a.getAnimatedValue())));
        animator.start();
        shownBalance = u.balance;

        bindUsage(R.id.rowInternet, getString(R.string.usage_internet),
                getString(R.string.usage_gb, u.internetLeft, u.tariffGb), (int) u.internetLeft, u.tariffGb);
        bindUsage(R.id.rowMinutes, getString(R.string.usage_minutes),
                getString(R.string.usage_pcs, u.minutesLeft, u.tariffMinutes), u.minutesLeft, u.tariffMinutes);
        bindUsage(R.id.rowSms, getString(R.string.usage_sms),
                getString(R.string.usage_pcs, u.smsLeft, u.tariffSms), u.smsLeft, u.tariffSms);
    }

    private void bindUsage(int rowId, String title, String value, int left, int total) {
        View row = root.findViewById(rowId);
        ((TextView) row.findViewById(R.id.usageTitle)).setText(title);
        ((TextView) row.findViewById(R.id.usageValue)).setText(value);
        ProgressBar bar = row.findViewById(R.id.usageBar);
        bar.setMax(Math.max(total, 1));
        bar.setProgress(left);
    }

    private boolean onMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_promo) {
            startActivity(new Intent(requireContext(), PromoActivity.class));
        } else if (id == R.id.lang_kk || id == R.id.lang_ru) {
            item.setChecked(true);
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(id == R.id.lang_kk ? "kk" : "ru"));
        } else if (id == R.id.theme_light || id == R.id.theme_dark) {
            item.setChecked(true);
            boolean dark = id == R.id.theme_dark;
            Prefs.setDark(requireContext(), dark);
            AppCompatDelegate.setDefaultNightMode(dark
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else if (id == R.id.action_about) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
        } else {
            return false;
        }
        return true;
    }
}
