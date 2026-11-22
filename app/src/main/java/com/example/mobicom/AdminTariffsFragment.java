package kz.mobicom.app;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminTariffsFragment extends LogFragment {

    private final List<Models.Tariff> items = new ArrayList<>();
    private TariffAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_tariffs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recycler = view.findViewById(R.id.adminTariffsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TariffAdapter();
        recycler.setAdapter(adapter);
        view.findViewById(R.id.btnAddTariff).setOnClickListener(v -> showForm(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(Db.get(requireContext()).getTariffs());
        adapter.notifyDataSetChanged();
    }

    private void toast(int res) {
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show();
    }

    /** tariff == null болса жаңа тариф қосылады, әйтпесе өңделеді. */
    private void showForm(Models.Tariff tariff) {
        String[] initial = tariff == null ? null : new String[]{tariff.name, String.valueOf(tariff.price),
                String.valueOf(tariff.internetGb), String.valueOf(tariff.minutes), String.valueOf(tariff.sms)};
        int text = InputType.TYPE_CLASS_TEXT, number = InputType.TYPE_CLASS_NUMBER;

        AdminDialogs.form(requireContext(),
                tariff == null ? R.string.admin_tariff_new : R.string.admin_tariff_edit,
                new String[]{getString(R.string.admin_h_name), getString(R.string.admin_h_price),
                        getString(R.string.admin_h_gb), getString(R.string.admin_h_min), getString(R.string.admin_h_sms)},
                new int[]{text, number, number, number, number}, initial, v -> {
                    String name = v[0].trim().toUpperCase();
                    int price = AdminDialogs.toInt(v[1]), gb = AdminDialogs.toInt(v[2]);
                    int min = AdminDialogs.toInt(v[3]), sms = AdminDialogs.toInt(v[4]);
                    if (name.isEmpty() || price <= 0 || gb < 0 || min < 0 || sms < 0) {
                        toast(R.string.admin_invalid);
                        return;
                    }
                    Db.get(requireContext()).saveTariff(tariff == null ? 0 : tariff.id, name, price, gb, min, sms);
                    toast(R.string.admin_saved);
                    reload();
                });
    }

    private void showActions(Models.Tariff t) {
        String[] actions = {getString(R.string.admin_edit), getString(R.string.action_delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(t.name)
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        showForm(t);
                    } else {
                        AdminDialogs.confirm(requireContext(), t.name, getString(R.string.admin_delete_confirm), () -> {
                            if (Db.get(requireContext()).deleteTariff(t.id)) {
                                toast(R.string.admin_deleted);
                                reload();
                            } else {
                                toast(R.string.admin_tariff_in_use);
                            }
                        });
                    }
                })
                .show();
    }

    // ------------------------------------------------------------ адаптер (item_tariff макеті)
    private class TariffAdapter extends RecyclerView.Adapter<TariffAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tariff, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Tariff t = items.get(position);
            h.name.setText(t.name);
            h.price.setText(getString(R.string.price_per_month, t.price));
            h.info.setText(getString(R.string.tariff_info, t.internetGb, t.minutes, t.sms));
            h.badge.setText(getString(R.string.admin_subscribers, Db.get(requireContext()).countUsersOnTariff(t.id)));
            h.badge.setVisibility(View.VISIBLE);
            h.itemView.setOnClickListener(v -> showActions(t));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name, price, info, badge;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.tariffName);
                price = v.findViewById(R.id.tariffPrice);
                info = v.findViewById(R.id.tariffInfo);
                badge = v.findViewById(R.id.tariffBadge);
            }
        }
    }
}
