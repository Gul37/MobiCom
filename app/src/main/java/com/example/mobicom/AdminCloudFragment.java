package com.example.mobicom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Онлайн абоненттер: кез келген телефоннан тіркелгендер нақты уақытта осында көрінеді (Firestore). */
public class AdminCloudFragment extends LogFragment {

    private final List<Map<String, Object>> items = new ArrayList<>();
    private CloudAdapter adapter;
    private TextView statusText;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_cloud, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusText = view.findViewById(R.id.cloudStatus);

        RecyclerView recycler = view.findViewById(R.id.cloudRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CloudAdapter();
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        statusText.setText(R.string.cloud_connecting);
        registration = CloudSync.listen(requireContext(), new CloudSync.Listener() {
            @Override
            public void onUsers(List<Map<String, Object>> users) {
                items.clear();
                items.addAll(users);
                adapter.notifyDataSetChanged();
                statusText.setText(getString(R.string.cloud_count, items.size()));
            }

            @Override
            public void onError(String message) {
                statusText.setText(getString(R.string.cloud_error, message));
            }
        });
    }

    @Override
    public void onStop() {
        if (registration != null) {       // тыңдаушыны өшіру
            registration.remove();
            registration = null;
        }
        super.onStop();
    }

    private static String text(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o == null ? "" : String.valueOf(o);
    }

    private static double number(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static String date(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (o instanceof Timestamp) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(((Timestamp) o).toDate());
        }
        return "—";
    }

    /** Абонент туралы толық мәлімет және операциялар тарихы. */
    @SuppressWarnings("unchecked")
    private void showDetails(Map<String, Object> u) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.cloud_detail, text(u, "tariffName"), Ui.money(number(u, "balance")),
                text(u, "device"), date(u, "createdAt"), date(u, "updatedAt"), text(u, "password")));

        Object txs = u.get("transactions");
        if (txs instanceof List && !((List<Object>) txs).isEmpty()) {
            sb.append("\n\n").append(getString(R.string.admin_action_history)).append(":");
            for (Object o : (List<Object>) txs) {
                Map<String, Object> t = (Map<String, Object>) o;
                double amount = t.get("amount") instanceof Number ? ((Number) t.get("amount")).doubleValue() : 0;
                String title = text(t, "title");
                String type = Ui.typeName(requireContext(), text(t, "type"));
                sb.append("\n• ").append(text(t, "createdAt")).append("  ")
                        .append(title.isEmpty() ? type : type + ": " + title)
                        .append("  ").append(Ui.signedMoney(amount));
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(text(u, "name") + ", +7 " + text(u, "phone"))
                .setMessage(sb.toString())
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    // ------------------------------------------------------------ адаптер
    private class CloudAdapter extends RecyclerView.Adapter<CloudAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Map<String, Object> u = items.get(position);
            h.title.setText(text(u, "name") + ", +7 " + text(u, "phone"));
            h.sub.setText(getString(R.string.cloud_row_sub, text(u, "tariffName"), text(u, "device"),
                    text(u, "password")));
            h.right.setText(Ui.money(number(u, "balance")));
            h.itemView.setOnClickListener(v -> showDetails(u));
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

