package com.example.mobicom;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ортақ онлайн база (Firebase Firestore).
 * Әр телефонда SQLite жұмыс көшірмесі болады, ал тіркелген абоненттің деректері
 * бұлтқа да жазылады: users/{нөмір}. Сондықтан кез келген телефоннан тіркелген адам
 * админ панеліндегі «Онлайн» бөлімінде көрінеді және басқа телефоннан кіре алады.
 * Карта деректері бұлтқа ЖІБЕРІЛМЕЙДІ.
 */
public final class CloudSync {

    private static final long TIMEOUT_MS = 8000;

    public interface Result {
        /** ok = сервермен байланыс болды ма; data = абонент деректері (жоқ болса null). */
        void done(boolean ok, Map<String, Object> data);
    }

    public interface Listener {
        void onUsers(List<Map<String, Object>> users);
        void onError(String message);
    }

    private CloudSync() { }

    /** Firebase қосылған ба (google-services.json бар ма). */
    public static boolean isReady(Context c) {
        try {
            return !FirebaseApp.getApps(c).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static FirebaseFirestore store() {
        return FirebaseFirestore.getInstance();
    }

    // ------------------------------------------------------------ оқу: нөмір бұлтта бар ма
    public static void fetchUser(Context c, String phone, Result result) {
        if (!isReady(c)) {
            result.done(false, null);
            return;
        }
        final AtomicBoolean finished = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable timeout = () -> {
            if (finished.compareAndSet(false, true)) result.done(false, null);
        };
        handler.postDelayed(timeout, TIMEOUT_MS);

        store().collection("users").document(phone).get()
                .addOnSuccessListener(snap -> {
                    if (!finished.compareAndSet(false, true)) return;
                    handler.removeCallbacks(timeout);
                    result.done(true, snap.exists() ? snap.getData() : null);
                })
                .addOnFailureListener(e -> {
                    if (!finished.compareAndSet(false, true)) return;
                    handler.removeCallbacks(timeout);
                    result.done(false, null);
                });
    }

    // ------------------------------------------------------------ оқу: осы атпен тіркелген абоненттер
    public interface ListResult { void done(boolean ok, List<Map<String, Object>> users); }

    public static void fetchByName(Context c, String name, ListResult result) {
        if (!isReady(c)) {
            result.done(false, new ArrayList<>());
            return;
        }
        final AtomicBoolean finished = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable timeout = () -> {
            if (finished.compareAndSet(false, true)) result.done(false, new ArrayList<>());
        };
        handler.postDelayed(timeout, TIMEOUT_MS);

        store().collection("users").whereEqualTo("name", name.trim()).get()
                .addOnSuccessListener(snap -> {
                    if (!finished.compareAndSet(false, true)) return;
                    handler.removeCallbacks(timeout);
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        if (d.getData() != null) list.add(d.getData());
                    }
                    result.done(true, list);
                })
                .addOnFailureListener(e -> {
                    if (!finished.compareAndSet(false, true)) return;
                    handler.removeCallbacks(timeout);
                    result.done(false, new ArrayList<>());
                });
    }

    // ------------------------------------------------------------ жазу: абонентті бұлтқа жіберу
    public static void push(Context c, int userId, boolean isNew) {
        if (!isReady(c)) return;
        try {
            Db db = Db.get(c);
            Models.User u = db.getUser(userId);
            if (u == null) return;

            Map<String, Object> data = new HashMap<>();
            data.put("name", u.name);
            data.put("phone", u.phone);
            data.put("password", u.password);
            data.put("balance", u.balance);
            data.put("tariffId", u.tariffId);
            data.put("tariffName", u.tariffName);
            data.put("internetLeft", u.internetLeft);
            data.put("minutesLeft", u.minutesLeft);
            data.put("smsLeft", u.smsLeft);
            data.put("device", Build.MANUFACTURER + " " + Build.MODEL);
            data.put("updatedAt", FieldValue.serverTimestamp());
            if (isNew) data.put("createdAt", FieldValue.serverTimestamp());

            List<Long> services = new ArrayList<>();
            for (Models.Service s : db.getServices(userId)) {
                if (s.active) services.add((long) s.id);
            }
            data.put("services", services);

            List<Map<String, Object>> txs = new ArrayList<>();
            for (Models.Tx t : db.getTransactions(userId, null, "date_asc")) {
                Map<String, Object> m = new HashMap<>();
                m.put("type", t.type);
                m.put("title", t.title);
                m.put("amount", t.amount);
                m.put("createdAt", t.createdAt);
                txs.add(m);
            }
            data.put("transactions", txs);

            store().collection("users").document(u.phone)
                    .set(data, com.google.firebase.firestore.SetOptions.merge());
        } catch (Exception ignored) {
            // бұлт қолжетімсіз болса да, қосымша жұмысын жалғастырады
        }
    }

    public static void delete(Context c, String phone) {
        if (!isReady(c)) return;
        try {
            store().collection("users").document(phone).delete();
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------ админ: барлық абонент (нақты уақытта)
    public static ListenerRegistration listen(Context c, Listener listener) {
        if (!isReady(c)) {
            listener.onError("Firebase қосылмаған (google-services.json жоқ)");
            return null;
        }
        return store().collection("users")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        listener.onError(e.getMessage() == null ? "Қате" : e.getMessage());
                        return;
                    }
                    List<Map<String, Object>> list = new ArrayList<>();
                    if (snap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            Map<String, Object> m = d.getData();
                            if (m != null) list.add(m);
                        }
                    }
                    listener.onUsers(list);
                });
    }
}
