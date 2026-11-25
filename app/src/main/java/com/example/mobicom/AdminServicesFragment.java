package com.example.mobicom;

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

public class AdminServicesFragment extends LogFragment {

    /** Әр жол: {id, атауы, бағасы, қосқан абонент саны}. */
    private final List<String[]> items = new ArrayList<>();
    private ServiceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_services, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recycler = view.findViewById(R.id.adminServicesRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ServiceAdapter();
        recycler.setAdapter(adapter);
        view.findViewById(R.id.btnAddService).setOnClickListener(v -> showForm(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(Db.get(requireContext()).adminServices());
        adapter.notifyDataSetChanged();
    }

    private void toast(int res) {
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show();
    }

    private void showForm(String[] service) {
        String[] initial = service == null ? null : new String[]{service[1], service[2]};
        AdminDialogs.form(requireContext(),
                service == null ? R.string.admin_service_new : R.string.admin_service_edit,
                new String[]{getString(R.string.admin_h_name), getString(R.string.admin_h_price)},
                new int[]{InputType.TYPE_CLASS_TEXT, InputType.TYPE_CLASS_NUMBER}, initial, v -> {
                    String name = v[0].trim();
                    int price = AdminDialogs.toInt(v[1]);
                    if (name.isEmpty() || price <= 0) {
                        toast(R.string.admin_invalid);
                        return;
                    }
                    Db.get(requireContext()).saveService(service == null ? 0 : Integer.parseInt(service[0]), name, price);
                    toast(R.string.admin_saved);
                    reload();
                });
    }

    private void showActions(String[] s) {
        String[] actions = {getString(R.string.admin_edit), getString(R.string.action_delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(s[1])
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        showForm(s);
                    } else {
                        AdminDialogs.confirm(requireContext(), s[1], getString(R.string.admin_delete_confirm), () -> {
                            Db.get(requireContext()).deleteService(Integer.parseInt(s[0]));
                            toast(R.string.admin_deleted);
                            reload();
                        });
                    }
                })
                .show();
    }

    // ------------------------------------------------------------ адаптер (item_admin_row макеті)
    private class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            String[] s = items.get(position);
            h.title.setText(s[1]);
            h.sub.setText(getString(R.string.admin_service_sub, Integer.parseInt(s[3])));
            h.right.setText(Ui.money(Double.parseDouble(s[2])));
            h.itemView.setOnClickListener(v -> showActions(s));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title, sub, right;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.rowTitle);
                sub = v.findViewById(R.id.rowSub);
                right = v.findViewById(R.id.rowRight);
            }
        }
    }
}
