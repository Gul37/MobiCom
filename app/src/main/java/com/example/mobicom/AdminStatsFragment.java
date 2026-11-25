package com.example.mobicom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AdminStatsFragment extends LogFragment {

    private View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Db db = Db.get(requireContext());
        double[] s = db.adminSummary();

        ((TextView) root.findViewById(R.id.statRevenue)).setText(Ui.money(s[2]));
        ((TextView) root.findViewById(R.id.statSummary)).setText(
                getString(R.string.admin_users, (int) s[0]) + "\n"
                        + getString(R.string.admin_purchases, (int) s[1]));

        int max = Math.max((int) s[0], 1);
        fill(R.id.tariffStatsContainer, db.adminTariffStats(), max);
        fill(R.id.serviceStatsContainer, db.adminServiceStats(), max);
    }

    /** Әр жолға item_usage макетін қолданамыз: атауы, саны және прогресс жолағы. */
    private void fill(int containerId, List<String[]> rows, int max) {
        LinearLayout container = root.findViewById(containerId);
        container.removeAllViews();
        for (String[] row : rows) {
            View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_usage, container, false);
            ((TextView) v.findViewById(R.id.usageTitle)).setText(row[0]);
            ((TextView) v.findViewById(R.id.usageValue)).setText(row[1]);
            ProgressBar bar = v.findViewById(R.id.usageBar);
            bar.setMax(max);
            bar.setProgress(Integer.parseInt(row[1]));
            container.addView(v);
        }
    }
}
