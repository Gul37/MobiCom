package kz.mobicom.app;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ServicesFragment extends LogFragment {

    private final List<Models.Service> items = new ArrayList<>();
    private ServiceAdapter adapter;
    private ListView listView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_services, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.servicesList);
        adapter = new ServiceAdapter();
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(Db.get(requireContext()).getServices(Prefs.getUserId(requireContext())));
        adapter.notifyDataSetChanged();
        listView.scheduleLayoutAnimation();
    }

    private void confirmToggle(Models.Service s, boolean wantOn) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(s.name(Ui.isRu(requireContext())))
                .setMessage(wantOn
                        ? getString(R.string.dialog_service_on, Ui.money(s.price))
                        : getString(R.string.dialog_service_off))
                .setPositiveButton(R.string.action_confirm, (d, w) -> toggle(s, wantOn))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void toggle(Models.Service s, boolean wantOn) {
        boolean ok = Db.get(requireContext()).setService(Prefs.getUserId(requireContext()), s, wantOn);
        if (ok) {
            Toast.makeText(requireContext(),
                    wantOn ? R.string.toast_service_on : R.string.toast_service_off, Toast.LENGTH_SHORT).show();
            reload();
        } else {
            Toast.makeText(requireContext(), R.string.toast_not_enough, Toast.LENGTH_LONG).show();
            ((MainActivity) requireActivity()).goTo(R.id.nav_topup);
        }
    }

    // ------------------------------------------------------------ Context menu
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        int pos = ((AdapterView.AdapterContextMenuInfo) info).position;
        Models.Service s = items.get(pos);
        menu.setHeaderTitle(s.name(Ui.isRu(requireContext())));
        requireActivity().getMenuInflater().inflate(R.menu.menu_service_context, menu);
        menu.findItem(R.id.ctx_toggle).setTitle(s.active ? R.string.ctx_deactivate : R.string.ctx_activate);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int pos = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        Models.Service s = items.get(pos);
        if (item.getItemId() == R.id.ctx_details) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(s.name(Ui.isRu(requireContext())))
                    .setMessage(s.description)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.ctx_toggle) {
            confirmToggle(s, !s.active);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // ------------------------------------------------------------ ListView адаптері
    private class ServiceAdapter extends BaseAdapter {

        @Override public int getCount() { return items.size(); }

        @Override public Models.Service getItem(int position) { return items.get(position); }

        @Override public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_service, parent, false);
            }
            Models.Service s = items.get(position);
            ((TextView) convertView.findViewById(R.id.serviceName)).setText(s.name(Ui.isRu(requireContext())));
            ((TextView) convertView.findViewById(R.id.serviceDesc)).setText(s.description);
            ((TextView) convertView.findViewById(R.id.servicePrice))
                    .setText(getString(R.string.price_per_month, s.price));

            CheckBox check = convertView.findViewById(R.id.serviceCheck);
            check.setChecked(s.active);
            // Нақты күй дерекқордан келеді, сондықтан белгіні қайтарып қойып, растау сұраймыз
            check.setOnClickListener(v -> {
                boolean wanted = check.isChecked();
                check.setChecked(!wanted);
                confirmToggle(s, wanted);
            });
            return convertView;
        }
    }
}
