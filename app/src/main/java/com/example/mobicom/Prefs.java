package kz.mobicom.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private Prefs() { }

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("mobicom_java", Context.MODE_PRIVATE);
    }

    public static int getUserId(Context c) { return p(c).getInt("userId", -1); }

    public static void setUserId(Context c, int id) { p(c).edit().putInt("userId", id).apply(); }

    public static boolean isDark(Context c) { return p(c).getBoolean("dark", true); }

    public static void setDark(Context c, boolean v) { p(c).edit().putBoolean("dark", v).apply(); }

    public static boolean isNotify(Context c) { return p(c).getBoolean("notify", true); }

    public static void setNotify(Context c, boolean v) { p(c).edit().putBoolean("notify", v).apply(); }

    public static String getReminder(Context c) { return p(c).getString("reminder", ""); }

    public static void setReminder(Context c, String v) { p(c).edit().putString("reminder", v).apply(); }
}
