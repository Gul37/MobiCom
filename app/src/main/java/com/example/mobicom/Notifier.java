package com.example.mobicom;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.security.SecureRandom;

/**
 * SMS орнына жұмыс істейтін хабарландыру.
 * Нақты SMS ақылы қызметті немесе серверді қажет етеді, ал хабарландыру интернетсіз,
 * SIM-картасыз және эмуляторда да жұмыс істейді.
 */
public final class Notifier {

    private static final String CHANNEL_ID = "mobicom_sms";
    private static final long CODE_LIFETIME_MS = 2 * 60 * 1000;

    private static String lastCode = "";
    private static String lastPhone = "";
    private static long issuedAt = 0;
    private static int nextId = 100;

    private Notifier() { }

    // ------------------------------------------------------------ растау коды
    /** 4 таңбалы кездейсоқ код жасап, нөмірге байлап сақтайды (жадта, дискіге жазылмайды). */
    public static String newCode(String phone) {
        lastCode = String.valueOf(1000 + new SecureRandom().nextInt(9000));
        lastPhone = phone;
        issuedAt = System.currentTimeMillis();
        return lastCode;
    }

    public static boolean hasActiveCode(String phone) {
        return !lastCode.isEmpty() && lastPhone.equals(phone)
                && System.currentTimeMillis() - issuedAt <= CODE_LIFETIME_MS;
    }

    public static boolean isCodeCorrect(String phone, String input) {
        return hasActiveCode(phone) && lastCode.equals(input);
    }

    // ------------------------------------------------------------ хабарландыру
    /** Android 13+ үшін хабарландыруға рұқсат берілген бе. */
    public static boolean canPost(Context c) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(c).areNotificationsEnabled();
    }

    /** Хабарландыру шығарады. Рұқсат болмаса false қайтарады. */
    public static boolean post(Context c, String title, String text) {
        if (!canPost(c)) return false;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "MobiCom SMS",
                    NotificationManager.IMPORTANCE_HIGH);
            c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sms)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(nextId++, b.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}

