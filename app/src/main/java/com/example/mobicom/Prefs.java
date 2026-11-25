package com.example.mobicom;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private Prefs() { }

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("mobicom_java", Context.MODE_PRIVATE);
    }

    /** Админнің жеке паролі (абоненттердің парольдерінен бөлек). Бастапқы мәні: Admin2026. */
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin2026";

    public static String getAdminPassword(Context c) {
        return p(c).getString("adminPassword", DEFAULT_ADMIN_PASSWORD);
    }

    public static void setAdminPassword(Context c, String v) { p(c).edit().putString("adminPassword", v).apply(); }

    public static int getUserId(Context c) { return p(c).getInt("userId", -1); }

    public static void setUserId(Context c, int id) { p(c).edit().putInt("userId", id).apply(); }

    public static boolean isDark(Context c) { return p(c).getBoolean("dark", true); }

    public static void setDark(Context c, boolean v) { p(c).edit().putBoolean("dark", v).apply(); }

    public static boolean isNotify(Context c) { return p(c).getBoolean("notify", true); }

    public static void setNotify(Context c, boolean v) { p(c).edit().putBoolean("notify", v).apply(); }

    public static String getReminder(Context c) { return p(c).getString("reminder", ""); }

    public static void setReminder(Context c, String v) { p(c).edit().putString("reminder", v).apply(); }
}
