package com.example.mobicom;

import android.content.Context;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public final class Ui {

    private Ui() { }

    /** Android 15-те экран жүйелік жолақтың астына созылады, сондықтан шегініс қосамыз. */
    public static void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    public static String money(double value) {
        return String.format(Locale.US, "%,.0f ₸", value).replace(',', ' ');
    }

    public static String signedMoney(double value) {
        return (value > 0 ? "+" : "") + money(value);
    }

    public static boolean isRu(Context c) {
        return c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ru");
    }

    public static String typeName(Context c, String type) {
        switch (type) {
            case Models.Tx.TOPUP: return c.getString(R.string.tx_topup);
            case Models.Tx.TARIFF: return c.getString(R.string.tx_tariff);
            case Models.Tx.SERVICE: return c.getString(R.string.tx_service);
            default: return c.getString(R.string.tx_bonus);
        }
    }

    public static String txTitle(Context c, Models.Tx tx) {
        String name = typeName(c, tx.type);
        return tx.title.isEmpty() ? name : name + ": " + tx.title;
    }
}
