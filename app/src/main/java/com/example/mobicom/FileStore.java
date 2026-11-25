package com.example.mobicom;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Файлдық жүйемен жұмыс (лекция: openFileOutput, openFileInput, getExternalFilesDir). */
public final class FileStore {

    private FileStore() { }

    /** Ішкі жад: /data/data/<package>/files/<name>. Файл тек осы қосымшаға қолжетімді. */
    public static void saveInternal(Context c, String name, String text) throws IOException {
        try (FileOutputStream fos = c.openFileOutput(name, Context.MODE_PRIVATE)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Ішкі жадтағы файлды оқиды. */
    public static String readInternal(Context c, String name) throws IOException {
        try (FileInputStream fin = c.openFileInput(name)) {
            byte[] bytes = new byte[fin.available()];
            int read = fin.read(bytes);
            return new String(bytes, 0, Math.max(read, 0), StandardCharsets.UTF_8);
        }
    }

    /** Сыртқы жад: Android/data/<package>/files/<name>. Басқа қосымшалар көре алады. */
    public static File saveExternal(Context c, String name, String text) throws IOException {
        File file = new File(c.getExternalFilesDir(null), name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}

