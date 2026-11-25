package com.example.mobicom;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TariffsFragment extends LogFragment {

    private RecyclerView recycler;
    private final List<Models.Tariff> tariffs = new ArrayList<>();
    private int currentTariffId = -1;
    private TariffAdapter adapter;

    // Екінші Activity-ден қайтқан нәтижені қабылдау
    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == TariffDetailActivity.RESULT_NEED_TOPUP) {
                    Toast.makeText(requireContext(), R.string.toast_not_enough, Toast.LENGTH_LONG).show();
                    ((MainActivity) requireActivity()).goTo(R.id.nav_topup);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tariffs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.tariffsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TariffAdapter();
        recycler.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Db db = Db.get(requireContext());
        Models.User user = db.getUser(Prefs.getUserId(requireContext()));
        currentTariffId = user == null ? -1 : user.tariffId;
        tariffs.clear();
        tariffs.addAll(db.getTariffs());
        adapter.notifyDataSetChanged();
        recycler.scheduleLayoutAnimation();
    }

    private void open(Models.Tariff tariff) {
        Intent intent = new Intent(requireContext(), TariffDetailActivity.class);
        intent.putExtra(TariffDetailActivity.EXTRA_TARIFF, tariff);          // Serializable
        intent.putExtra(TariffDetailActivity.EXTRA_CURRENT_ID, currentTariffId);
        detailLauncher.launch(intent);
    }

    // ------------------------------------------------------------ RecyclerView адаптері
    private class TariffAdapter extends RecyclerView.Adapter<TariffAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tariff, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Tariff t = tariffs.get(position);
            h.name.setText(t.name);
            h.price.setText(getString(R.string.price_per_month, t.price));
            h.info.setText(getString(R.string.tariff_info, t.internetGb, t.minutes, t.sms));
            h.badge.setVisibility(t.id == currentTariffId ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> open(t));
        }

        @Override
        public int getItemCount() { return tariffs.size(); }

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
