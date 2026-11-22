package kz.mobicom.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends LogFragment {

    private static final String[] FILTER_TYPES = {
            null, Models.Tx.TOPUP, Models.Tx.TARIFF, Models.Tx.SERVICE, Models.Tx.BONUS};

    private final List<Models.Tx> items = new ArrayList<>();
    private TxAdapter adapter;
    private TextView statsText, emptyText;
    private String filter = null;
    private String sort = "date_desc";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.historyToolbar);
        toolbar.inflateMenu(R.menu.menu_history);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this::onMenu);

        statsText = view.findViewById(R.id.statsText);
        emptyText = view.findViewById(R.id.historyEmpty);

        // Spinner + ArrayAdapter
        Spinner spinner = view.findViewById(R.id.historyFilter);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.history_filters, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                filter = FILTER_TYPES[position];
                reload();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        RecyclerView recycler = view.findViewById(R.id.historyRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TxAdapter();
        recycler.setAdapter(adapter);
    }

    private void reload() {
        Db db = Db.get(requireContext());
        int userId = Prefs.getUserId(requireContext());

        items.clear();
        items.addAll(db.getTransactions(userId, filter, sort));
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        StringBuilder sb = new StringBuilder();
        for (String[] row : db.statsByType(userId)) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(getString(R.string.stats_line, Ui.typeName(requireContext(), row[0]),
                    Ui.signedMoney(Double.parseDouble(row[1])), Integer.parseInt(row[2])));
        }
        statsText.setText(sb.length() == 0 ? getString(R.string.stats_empty) : sb.toString());
    }

    private boolean onMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_date_desc) sort = "date_desc";
        else if (id == R.id.sort_date_asc) sort = "date_asc";
        else if (id == R.id.sort_amount_desc) sort = "amount_desc";
        else if (id == R.id.sort_amount_asc) sort = "amount_asc";
        else if (id == R.id.action_clear) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_clear_title)
                    .setMessage(R.string.dialog_clear_message)
                    .setPositiveButton(R.string.action_delete, (d, w) -> {
                        Db.get(requireContext()).clearTransactions(Prefs.getUserId(requireContext()));
                        reload();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return true;
        } else {
            return false;
        }
        item.setChecked(true);
        reload();
        return true;
    }

    /** PopupMenu: жолды ұзақ басқанда «Өшіру». */
    private void showPopup(View anchor, Models.Tx tx) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_tx_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(m -> {
            Db.get(requireContext()).deleteTransaction(tx.id);
            reload();
            return true;
        });
        popup.show();
    }

    // ------------------------------------------------------------ адаптер
    private class TxAdapter extends RecyclerView.Adapter<TxAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tx, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Tx tx = items.get(position);
            h.title.setText(Ui.txTitle(requireContext(), tx));
            h.date.setText(tx.createdAt);
            h.amount.setText(Ui.signedMoney(tx.amount));
            h.amount.setTextColor(ContextCompat.getColor(requireContext(),
                    tx.amount >= 0 ? R.color.mobi_green : R.color.mobi_text));
            h.itemView.setOnLongClickListener(v -> {
                showPopup(v, tx);
                return true;
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title, date, amount;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.txTitle);
                date = v.findViewById(R.id.txDate);
                amount = v.findViewById(R.id.txAmount);
            }
        }
    }
}
