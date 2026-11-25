package com.example.mobicom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GPS: абоненттің орнын анықтап, ең жақын MobiCom салонын көрсетеді.
 * Лекциядағыдай LocationManager және екі провайдер (GPS, Network) қолданылады.
 */
public class SalonsActivity extends AppCompatActivity {

    private static final String TAG = "MobiComLifecycle";

    /** Салон: атауы және координаттары (шамамен, оқу мақсатында). */
    private static class Salon {
        final String name, address;
        final double lat, lon;
        float distance = -1;   // метр, орын анықталған соң есептеледі

        Salon(String name, String address, double lat, double lon) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private final List<Salon> salons = new ArrayList<>();
    private SalonAdapter adapter;

    private LocationManager locationManager;
    private Location best;                       // ең жақсы (дәл және жаңа) орын

    private TextView gpsStatus, netStatus, coords, nearest;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasPermission()) {
                    startUpdates();
                } else {
                    coords.setText(R.string.gps_no_permission);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SalonsActivity.onCreate");
        setContentView(R.layout.activity_salons);
        Ui.applyInsets(findViewById(R.id.salonRoot));
        findViewById(R.id.salonBack).setOnClickListener(v -> finish());

        gpsStatus = findViewById(R.id.gpsStatus);
        netStatus = findViewById(R.id.netStatus);
        coords = findViewById(R.id.gpsCoords);
        nearest = findViewById(R.id.nearestText);

        // Астана: шамамен координаттар
        salons.add(new Salon("MobiCom: Бәйтерек", "Нұржол б-ры, Астана", 51.1283, 71.4305));
        salons.add(new Salon("MobiCom: Хан Шатыр", "Тұран даңғ. 37", 51.1326, 71.4036));
        salons.add(new Salon("MobiCom: Mega Silk Way", "Қабанбай батыр даңғ. 62", 51.0889, 71.4038));
        salons.add(new Salon("MobiCom: Керуен", "Достық к-сі 9", 51.1280, 71.4160));
        salons.add(new Salon("MobiCom: ЕҰУ", "Сәтпаев к-сі 2", 51.1239, 71.4142));

        RecyclerView recycler = findViewById(R.id.salonRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalonAdapter();
        recycler.setAdapter(adapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        findViewById(R.id.btnLocate).setOnClickListener(v -> {
            if (hasPermission()) {
                startUpdates();
                Toast.makeText(this, R.string.gps_searching, Toast.LENGTH_SHORT).show();
            } else {
                askPermission();
            }
        });
        // Лекциядағыдай: орналасу параметрлерін ашу (GPS қосу)
        findViewById(R.id.btnLocationSettings).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));

        checkEnabled();
        showLocation();
    }

    // ------------------------------------------------------------ рұқсат
    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    // ------------------------------------------------------------ орынды тыңдау
    @Override
    protected void onResume() {
        super.onResume();
        checkEnabled();
        if (hasPermission()) {
            startUpdates();
        } else {
            askPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(listener);   // тыңдаушыны өшіру (батареяны үнемдеу)
    }

    @SuppressLint("MissingPermission")
    private void startUpdates() {
        if (!hasPermission()) return;
        locationManager.removeUpdates(listener);
        List<String> providers = locationManager.getAllProviders();

        // Әр 10 секунд сайын немесе 10 метр қозғалғанда жаңа координат келеді
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000, 10, listener);
            consider(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10_000, 10, listener);
            consider(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
        }
        checkEnabled();
    }

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            consider(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            checkEnabled();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            checkEnabled();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onStatusChanged(String provider, int status, Bundle extras) { }
    };

    /** Жаңа орын келгенде: ол жақсырақ болса ғана қабылдаймыз. */
    private void consider(Location location) {
        if (location == null) return;
        if (best == null || isBetter(location, best)) {
            best = location;
            showLocation();
        }
    }

    /** Жаңа және дәлірек орын «жақсы» болып саналады. */
    private boolean isBetter(Location a, Location b) {
        long timeDelta = a.getTime() - b.getTime();
        if (timeDelta > 60_000) return true;       // әлдеқайда жаңа
        if (timeDelta < -60_000) return false;
        return a.getAccuracy() <= b.getAccuracy(); // шамалас уақытта дәлірегі
    }

    // ------------------------------------------------------------ экран
    private void checkEnabled() {
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        gpsStatus.setText(getString(R.string.gps_provider, "GPS",
                getString(gps ? R.string.toggle_on : R.string.toggle_off)));
        netStatus.setText(getString(R.string.gps_provider, "Network",
                getString(net ? R.string.toggle_on : R.string.toggle_off)));
    }

    private void showLocation() {
        if (best == null) {
            coords.setText(hasPermission() ? R.string.gps_searching : R.string.gps_no_permission);
            nearest.setText(R.string.gps_nearest_unknown);
            adapter.notifyDataSetChanged();
            return;
        }
        coords.setText(String.format(Locale.US,
                "lat = %.5f, lon = %.5f\n%s: %s, ±%.0f м\n%s",
                best.getLatitude(), best.getLongitude(),
                getString(R.string.gps_provider_name), best.getProvider(), best.getAccuracy(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(best.getTime()))));

        // Әр салонға дейінгі қашықтық (метр) және жақыннан алысқа қарай сұрыптау
        float[] result = new float[1];
        for (Salon s : salons) {
            Location.distanceBetween(best.getLatitude(), best.getLongitude(), s.lat, s.lon, result);
            s.distance = result[0];
        }
        Collections.sort(salons, (a, b) -> Float.compare(a.distance, b.distance));
        Salon first = salons.get(0);
        nearest.setText(getString(R.string.gps_nearest, first.name, distanceText(first.distance)));
        adapter.notifyDataSetChanged();
    }

    private static String distanceText(float meters) {
        if (meters < 0) return "—";
        if (meters < 1000) return String.format(Locale.US, "%.0f м", meters);
        return String.format(Locale.US, "%.1f км", meters / 1000);
    }

    /** Implicit Intent: картада ашу (geo: схемасы). Карта қосымшасы болмаса, браузерде ашады. */
    private void openMap(Salon s) {
        Uri geo = Uri.parse("geo:" + s.lat + "," + s.lon + "?q=" + s.lat + "," + s.lon
                + "(" + Uri.encode(s.name) + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps?q=" + s.lat + "," + s.lon)));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------------------------------------------------------------ адаптер
    private class SalonAdapter extends RecyclerView.Adapter<SalonAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Salon s = salons.get(position);
            h.title.setText(s.name);
            h.sub.setText(s.address);
            h.right.setText(distanceText(s.distance));
            h.itemView.setOnClickListener(v -> openMap(s));
        }

        @Override
        public int getItemCount() { return salons.size(); }

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

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "SalonsActivity.onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "SalonsActivity.onStop"); }
}
