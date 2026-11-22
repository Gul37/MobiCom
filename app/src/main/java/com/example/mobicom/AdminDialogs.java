package kz.mobicom.app;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class AdminDialogs {

    public interface OnSave { void save(String[] values); }

    private AdminDialogs() { }

    /** Бірнеше EditText-і бар диалог (қосу немесе өңдеу формасы). */
    public static void form(Context c, int titleRes, String[] hints, int[] inputTypes,
                            String[] initial, OnSave callback) {
        int pad = (int) (20 * c.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText[] fields = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            EditText e = new EditText(c);
            e.setHint(hints[i]);
            e.setInputType(inputTypes[i]);
            if (initial != null) e.setText(initial[i]);
            box.addView(e);
            fields[i] = e;
        }

        new MaterialAlertDialogBuilder(c)
                .setTitle(titleRes)
                .setView(box)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String[] values = new String[fields.length];
                    for (int i = 0; i < fields.length; i++) values[i] = fields[i].getText().toString();
                    callback.save(values);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    public static void confirm(Context c, String title, String message, Runnable onYes) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_confirm, (d, w) -> onYes.run())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Мәтінді санға айналдырады. Дұрыс емес болса -1. */
    public static int toInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
