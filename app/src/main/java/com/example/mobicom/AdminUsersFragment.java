package kz.mobicom.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends LogFragment {

    private final List<Models.AdminUser> items = new ArrayList<>();
    private UserAdapter adapter;
    private TextView emptyText;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyText = view.findViewById(R.id.usersEmpty);

        RecyclerView recycler = view.findViewById(R.id.usersRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter();
        recycler.setAdapter(adapter);

        EditText search = view.findViewById(R.id.userSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                reload();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(Db.get(requireContext()).adminUsers(query));
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toast(int res) {
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------ әрекеттер мәзірі
    private void showActions(Models.AdminUser u) {
        String[] actions = {
                getString(R.string.admin_action_balance),
                getString(R.string.admin_action_tariff),
                getString(R.string.admin_action_history),
                getString(R.string.admin_action_delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(u.name + ", +7 " + u.phone)
                .setItems(actions, (d, which) -> {
                    if (which == 0) addBalance(u);
                    else if (which == 1) changeTariff(u);
                    else if (which == 2) showHistory(u);
                    else deleteUser(u);
                })
                .show();
    }

    private void addBalance(Models.AdminUser u) {
        AdminDialogs.form(requireContext(), R.string.admin_balance_title,
                new String[]{getString(R.string.hint_amount)},
                new int[]{InputType.TYPE_CLASS_NUMBER}, null, v -> {
                    int amount = AdminDialogs.toInt(v[0]);
                    if (!Validators.isAmount(amount)) {
                        toast(R.string.admin_invalid);
                        return;
                    }
                    Db.get(requireContext()).adminAddBalance(u.id, amount);
                    Toast.makeText(requireContext(),
                            getString(R.string.admin_balance_done, Ui.money(amount)), Toast.LENGTH_SHORT).show();
                    reload();
                });
    }

    private void changeTariff(Models.AdminUser u) {
        List<Models.Tariff> tariffs = Db.get(requireContext()).getTariffs();
        String[] names = new String[tariffs.size()];
        for (int i = 0; i < names.length; i++) names[i] = tariffs.get(i).name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_action_tariff)
                .setItems(names, (d, which) -> {
                    Db.get(requireContext()).adminChangeTariff(u.id, tariffs.get(which));
                    toast(R.string.admin_tariff_changed);
                    reload();
                })
                .show();
    }

    private void showHistory(Models.AdminUser u) {
        List<Models.Tx> txs = Db.get(requireContext()).getTransactions(u.id, null, "date_desc");
        StringBuilder sb = new StringBuilder();
        for (Models.Tx tx : txs) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(tx.createdAt).append("\n")
                    .append(Ui.txTitle(requireContext(), tx)).append(": ").append(Ui.signedMoney(tx.amount));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_action_history)
                .setMessage(sb.length() == 0 ? getString(R.string.history_empty) : sb.toString())
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private void deleteUser(Models.AdminUser u) {
        AdminDialogs.confirm(requireContext(), u.name + ", +7 " + u.phone,
                getString(R.string.admin_delete_user_msg), () -> {
                    Db.get(requireContext()).adminDeleteUser(u.id);
                    toast(R.string.admin_user_deleted);
                    reload();
                });
    }

    // ------------------------------------------------------------ адаптер
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.AdminUser u = items.get(position);
            h.title.setText(u.name + ", +7 " + u.phone);
            h.sub.setText(getString(R.string.admin_user_sub, u.tariffName, u.purchases, Ui.money(u.spent)));
            h.right.setText(Ui.money(u.balance));
            h.itemView.setOnClickListener(v -> showActions(u));
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

