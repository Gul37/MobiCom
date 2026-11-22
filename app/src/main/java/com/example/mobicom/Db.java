package kz.mobicom.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Db extends SQLiteOpenHelper {

    private static Db instance;

    public static synchronized Db get(Context context) {
        if (instance == null) instance = new Db(context.getApplicationContext());
        return instance;
    }

    private Db(Context context) {
        super(context, "mobicom.db", null, 1);
    }

    // ================================================================ кестелер
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                + "phone TEXT NOT NULL UNIQUE, password TEXT, balance REAL NOT NULL, tariff_id INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE tariffs (id INTEGER PRIMARY KEY, name TEXT NOT NULL, price REAL NOT NULL, "
                + "internet_gb INTEGER NOT NULL, minutes INTEGER NOT NULL, sms INTEGER NOT NULL, "
                + "description_kk TEXT, description_ru TEXT)");
        db.execSQL("CREATE TABLE services (id INTEGER PRIMARY KEY, name_kk TEXT NOT NULL, "
                + "name_ru TEXT NOT NULL, price REAL NOT NULL, description TEXT)");
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, "
                + "type TEXT NOT NULL, title TEXT NOT NULL, amount REAL NOT NULL, created_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE usage (user_id INTEGER PRIMARY KEY, internet_left REAL NOT NULL, "
                + "minutes_left INTEGER NOT NULL, sms_left INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE user_services (user_id INTEGER NOT NULL, service_id INTEGER NOT NULL, "
                + "PRIMARY KEY (user_id, service_id))");

        // Бастапқы деректер
        addTariff(db, 1, "ОҢАЙ", 2990, 15, 150, 100,
                "Күнделікті байланысқа арналған тиімді тариф", "Выгодный тариф для ежедневного общения");
        addTariff(db, 2, "ЕРКІН", 4490, 30, 300, 200,
                "Көбірек интернет пен еркін сөйлесу", "Больше интернета и общения без ограничений");
        addTariff(db, 3, "ШЕКСІЗ", 6990, 60, 600, 500,
                "Белсенді пайдаланушыларға барынша көп мүмкіндік", "Максимум возможностей для активных пользователей");
        addTariff(db, 4, "БАСТАУ", 1490, 5, 50, 50,
                "Жаңадан бастағандарға арналған қарапайым тариф", "Простой тариф для начинающих");
        addTariff(db, 5, "ПРЕМИУМ", 9990, 100, 1000, 1000,
                "Шектеусіз қолдану және ең жоғары жылдамдық", "Использование без ограничений и максимальная скорость");

        addService(db, 1, "+10 ГБ интернет", "+10 ГБ интернета", 990, "Айға қосымша 10 ГБ");
        addService(db, 2, "Роуминг пакеті", "Пакет роуминга", 1490, "Шетелде байланыс пен интернет");
        addService(db, 3, "Күндік шексіздік", "Безлимит на день", 490, "Бір күнге шексіз интернет");
        addService(db, 4, "+100 SMS пакеті", "Пакет +100 SMS", 290, "Айға қосымша 100 SMS");
        addService(db, 5, "Музыка", "Музыка", 690, "Музыкалық қызметке шектеусіз қолжетімділік");
        addService(db, 6, "Қауіпсіз интернет", "Безопасный интернет", 390, "Зиянды сайттардан қорғау");
        addService(db, 7, "Қайта қоңырау шалу", "Перезвони мне", 190, "Баланс болмағанда қоңырау сұрау");

        // Демо абонент: нөмірі 7001234567
        ContentValues u = new ContentValues();
        u.put("name", "Пайдаланушы");
        u.put("phone", "7001234567");
        u.put("balance", 2450);
        u.put("tariff_id", 2);
        long id = db.insert("users", null, u);
        addUsage(db, id, 18.4, 245, 172);
        addTx(db, id, Models.Tx.TOPUP, "", 5000, daysAgo(20));
        addTx(db, id, Models.Tx.TARIFF, "ЕРКІН", -4490, daysAgo(18));
        addTx(db, id, Models.Tx.TOPUP, "", 3000, daysAgo(4));

        // Қосымша демо абоненттер (админ статистикасы үшін)
        addDemoUser(db, "Айдана", "7015550101", 1800, 1, 0, 0, 12);
        addDemoUser(db, "Данияр", "7025550102", 5200, 3, 2, 1490, 9);
        addDemoUser(db, "Мадина", "7475550103", 900, 2, 1, 990, 6);
        addDemoUser(db, "Ерлан", "7775550104", 3100, 1, 3, 490, 3);
        addDemoUser(db, "Алия", "7055550105", 700, 4, 4, 290, 8);
        addDemoUser(db, "Нұрлан", "7085550106", 8200, 5, 5, 690, 5);
        addDemoUser(db, "Сәуле", "7715550107", 1200, 4, 6, 390, 2);
    }

    /** Демо абонент: тариф сатып алған, serviceId > 0 болса қызмет те қосқан. */
    private void addDemoUser(SQLiteDatabase db, String name, String phone, double balance,
                             int tariffId, int serviceId, int servicePrice, int days) {
        String[] names = {"ОҢАЙ", "ЕРКІН", "ШЕКСІЗ", "БАСТАУ", "ПРЕМИУМ"};
        int[] prices = {2990, 4490, 6990, 1490, 9990}, gb = {15, 30, 60, 5, 100};
        int[] min = {150, 300, 600, 50, 1000}, sms = {100, 200, 500, 50, 1000};
        String[] serviceNames = {"", "+10 ГБ интернет", "Роуминг пакеті", "Күндік шексіздік",
                "+100 SMS пакеті", "Музыка", "Қауіпсіз интернет", "Қайта қоңырау шалу"};
        int i = tariffId - 1;

        ContentValues u = new ContentValues();
        u.put("name", name);
        u.put("phone", phone);
        u.put("balance", balance);
        u.put("tariff_id", tariffId);
        long id = db.insert("users", null, u);
        addUsage(db, id, gb[i] * 0.6, (int) (min[i] * 0.6), (int) (sms[i] * 0.6));
        addTx(db, id, Models.Tx.TOPUP, "", 5000, daysAgo(days + 1));
        addTx(db, id, Models.Tx.TARIFF, names[i], -prices[i], daysAgo(days));
        if (serviceId > 0) {
            ContentValues us = new ContentValues();
            us.put("user_id", id);
            us.put("service_id", serviceId);
            db.insert("user_services", null, us);
            addTx(db, id, Models.Tx.SERVICE, serviceNames[serviceId], -servicePrice, daysAgo(days - 1));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS user_services");
        db.execSQL("DROP TABLE IF EXISTS usage");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS services");
        db.execSQL("DROP TABLE IF EXISTS tariffs");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // ================================================================ көмекші әдістер
    private void addTariff(SQLiteDatabase db, int id, String name, int price, int gb, int min, int sms,
                           String kk, String ru) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("name", name);
        v.put("price", price);
        v.put("internet_gb", gb);
        v.put("minutes", min);
        v.put("sms", sms);
        v.put("description_kk", kk);
        v.put("description_ru", ru);
        db.insert("tariffs", null, v);
    }

    private void addService(SQLiteDatabase db, int id, String kk, String ru, int price, String desc) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("name_kk", kk);
        v.put("name_ru", ru);
        v.put("price", price);
        v.put("description", desc);
        db.insert("services", null, v);
    }

    private void addUsage(SQLiteDatabase db, long userId, double internet, int minutes, int sms) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("internet_left", internet);
        v.put("minutes_left", minutes);
        v.put("sms_left", sms);
        db.insert("usage", null, v);
    }

    private void addTx(SQLiteDatabase db, long userId, String type, String title, double amount, String date) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("type", type);
        v.put("title", title);
        v.put("amount", amount);
        v.put("created_at", date);
        db.insert("transactions", null, v);
    }

    private static String format(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date);
    }

    private static String now() { return format(new Date()); }

    private static String daysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return format(c.getTime());
    }

    private double balanceOf(SQLiteDatabase db, int userId) {
        try (Cursor c = db.query("users", new String[]{"balance"}, "id = ?",
                new String[]{String.valueOf(userId)}, null, null, null)) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    /** UPDATE: жаңа баланс жазу. */
    private void setBalance(SQLiteDatabase db, int userId, double balance) {
        ContentValues v = new ContentValues();
        v.put("balance", balance);
        db.update("users", v, "id = ?", new String[]{String.valueOf(userId)});
    }

    // ================================================================ абонент
    /** SELECT ... WHERE: нөмір бойынша іздеу. Табылмаса -1. */
    public int findUserByPhone(String phone) {
        try (Cursor c = getReadableDatabase().query("users", new String[]{"id"}, "phone = ?",
                new String[]{phone}, null, null, null)) {
            return c.moveToFirst() ? c.getInt(0) : -1;
        }
    }

    /** INSERT: жаңа абонент (баланс 1000 ₸ бонус). */
    public int createUser(String name, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("name", name);
            v.put("phone", phone);
            v.put("balance", 1000);
            v.put("tariff_id", 1);
            long id = db.insert("users", null, v);
            addUsage(db, id, 15, 150, 100);
            addTx(db, id, Models.Tx.BONUS, "", 1000, now());
            db.setTransactionSuccessful();
            return (int) id;
        } finally {
            db.endTransaction();
        }
    }

    /** SELECT ... JOIN: абонент + тариф + пакеттер қалдығы бір сұрауда. */
    public Models.User getUser(int id) {
        String sql = "SELECT u.id, u.name, u.phone, u.balance, u.tariff_id, t.name, t.internet_gb, "
                + "t.minutes, t.sms, g.internet_left, g.minutes_left, g.sms_left "
                + "FROM users u JOIN tariffs t ON t.id = u.tariff_id "
                + "JOIN usage g ON g.user_id = u.id WHERE u.id = ?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null;
            Models.User u = new Models.User();
            u.id = c.getInt(0);
            u.name = c.getString(1);
            u.phone = c.getString(2);
            u.balance = c.getDouble(3);
            u.tariffId = c.getInt(4);
            u.tariffName = c.getString(5);
            u.tariffGb = c.getInt(6);
            u.tariffMinutes = c.getInt(7);
            u.tariffSms = c.getInt(8);
            u.internetLeft = c.getDouble(9);
            u.minutesLeft = c.getInt(10);
            u.smsLeft = c.getInt(11);
            return u;
        }
    }

    /** UPDATE: баланс толтыру және тарихқа жазу (бір транзакцияда). */
    public void topUp(int userId, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            setBalance(db, userId, balanceOf(db, userId) + amount);
            addTx(db, userId, Models.Tx.TOPUP, "", amount, now());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================ тарифтер
    /** SELECT ... ORDER BY: тарифтер баға бойынша. */
    public List<Models.Tariff> getTariffs() {
        List<Models.Tariff> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("tariffs", null, null, null, null, null, "price ASC")) {
            while (c.moveToNext()) {
                list.add(new Models.Tariff(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        (int) c.getDouble(c.getColumnIndexOrThrow("price")),
                        c.getInt(c.getColumnIndexOrThrow("internet_gb")),
                        c.getInt(c.getColumnIndexOrThrow("minutes")),
                        c.getInt(c.getColumnIndexOrThrow("sms")),
                        c.getString(c.getColumnIndexOrThrow("description_kk")),
                        c.getString(c.getColumnIndexOrThrow("description_ru"))));
            }
        }
        return list;
    }

    /** Тарифті ауыстыру: баланс жетсе true. Баланс, пакеттер және тарих бірге жаңарады. */
    public boolean switchTariff(int userId, Models.Tariff tariff) {
        SQLiteDatabase db = getWritableDatabase();
        double balance = balanceOf(db, userId);
        if (balance < tariff.price) return false;

        db.beginTransaction();
        try {
            ContentValues user = new ContentValues();
            user.put("balance", balance - tariff.price);
            user.put("tariff_id", tariff.id);
            db.update("users", user, "id = ?", new String[]{String.valueOf(userId)});

            ContentValues usage = new ContentValues();
            usage.put("internet_left", tariff.internetGb);
            usage.put("minutes_left", tariff.minutes);
            usage.put("sms_left", tariff.sms);
            db.update("usage", usage, "user_id = ?", new String[]{String.valueOf(userId)});

            addTx(db, userId, Models.Tx.TARIFF, tariff.name, -tariff.price, now());
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================ қызметтер
    /** SELECT ... LEFT JOIN: барлық қызмет және абонентте қосулы ма. */
    public List<Models.Service> getServices(int userId) {
        String sql = "SELECT s.id, s.name_kk, s.name_ru, s.price, s.description, "
                + "CASE WHEN us.user_id IS NULL THEN 0 ELSE 1 END "
                + "FROM services s LEFT JOIN user_services us "
                + "ON us.service_id = s.id AND us.user_id = ? ORDER BY s.id";
        List<Models.Service> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                list.add(new Models.Service(c.getInt(0), c.getString(1), c.getString(2),
                        (int) c.getDouble(3), c.getString(4), c.getInt(5) == 1));
            }
        }
        return list;
    }

    /** Қызметті қосу (INSERT, баланстан төлем) немесе өшіру (DELETE). Баланс жетпесе false. */
    public boolean setService(int userId, Models.Service service, boolean on) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(userId), String.valueOf(service.id)};
        if (!on) {
            db.delete("user_services", "user_id = ? AND service_id = ?", args);
            return true;
        }
        double balance = balanceOf(db, userId);
        if (balance < service.price) return false;

        db.beginTransaction();
        try {
            setBalance(db, userId, balance - service.price);
            ContentValues v = new ContentValues();
            v.put("user_id", userId);
            v.put("service_id", service.id);
            db.insertWithOnConflict("user_services", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            addTx(db, userId, Models.Tx.SERVICE, service.nameKk, -service.price, now());
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================ операциялар тарихы
    /**
     * SELECT ... WHERE ... ORDER BY.
     * type == null болса, барлық түр. sort: date_desc, date_asc, amount_desc, amount_asc.
     */
    public List<Models.Tx> getTransactions(int userId, String type, String sort) {
        String where = "user_id = ?";
        String[] args = {String.valueOf(userId)};
        if (type != null) {
            where += " AND type = ?";
            args = new String[]{String.valueOf(userId), type};
        }

        String order;
        switch (sort) {
            case "date_asc": order = "created_at ASC"; break;
            case "amount_desc": order = "ABS(amount) DESC"; break;
            case "amount_asc": order = "ABS(amount) ASC"; break;
            default: order = "created_at DESC, id DESC";
        }

        List<Models.Tx> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("transactions", null, where, args, null, null, order)) {
            while (c.moveToNext()) {
                list.add(new Models.Tx(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("type")),
                        c.getString(c.getColumnIndexOrThrow("title")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("created_at"))));
            }
        }
        return list;
    }

    /** GROUP BY: түр бойынша сома мен саны. Әр жол: {түр, сома, саны}. */
    public List<String[]> statsByType(int userId) {
        List<String[]> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("transactions",
                new String[]{"type", "SUM(amount)", "COUNT(*)"},
                "user_id = ?", new String[]{String.valueOf(userId)},
                "type", null, "type ASC")) {
            while (c.moveToNext()) {
                list.add(new String[]{c.getString(0), String.valueOf(c.getDouble(1)), String.valueOf(c.getInt(2))});
            }
        }
        return list;
    }

    // ================================================================ админ статистикасы
    /** Әр жол: {абоненттер саны, сатып алу саны, түсім}. Түсім = тариф және қызмет төлемдерінің қосындысы. */
    public double[] adminSummary() {
        SQLiteDatabase db = getReadableDatabase();
        double[] r = new double[3];
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM users", null)) {
            if (c.moveToFirst()) r[0] = c.getInt(0);
        }
        try (Cursor c = db.rawQuery("SELECT COUNT(*), COALESCE(-SUM(amount), 0) FROM transactions "
                + "WHERE type IN ('TARIFF', 'SERVICE')", null)) {
            if (c.moveToFirst()) {
                r[1] = c.getInt(0);
                r[2] = c.getDouble(1);
            }
        }
        return r;
    }

    /** GROUP BY + LEFT JOIN: әр тарифте неше абонент. Жол: {атауы, саны}. */
    public List<String[]> adminTariffStats() {
        List<String[]> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT t.name, COUNT(u.id) FROM tariffs t "
                + "LEFT JOIN users u ON u.tariff_id = t.id GROUP BY t.id ORDER BY t.price", null)) {
            while (c.moveToNext()) list.add(new String[]{c.getString(0), String.valueOf(c.getInt(1))});
        }
        return list;
    }

    /** GROUP BY + LEFT JOIN: әр қызметті неше абонент қосқан. Жол: {атауы, саны}. */
    public List<String[]> adminServiceStats() {
        List<String[]> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT s.name_kk, COUNT(us.user_id) FROM services s "
                + "LEFT JOIN user_services us ON us.service_id = s.id GROUP BY s.id ORDER BY s.id", null)) {
            while (c.moveToNext()) list.add(new String[]{c.getString(0), String.valueOf(c.getInt(1))});
        }
        return list;
    }

    /** Админ: абоненттер тізімі (іздеу: нөмір немесе аты). JOIN + LEFT JOIN + GROUP BY + LIKE. */
    public List<Models.AdminUser> adminUsers(String query) {
        String sql = "SELECT u.id, u.name, u.phone, t.name, u.balance, COUNT(x.id), COALESCE(-SUM(x.amount), 0) "
                + "FROM users u JOIN tariffs t ON t.id = u.tariff_id "
                + "LEFT JOIN transactions x ON x.user_id = u.id AND x.type IN ('TARIFF', 'SERVICE') "
                + "WHERE u.phone LIKE ? OR u.name LIKE ? GROUP BY u.id ORDER BY u.id";
        String like = "%" + query + "%";
        List<Models.AdminUser> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{like, like})) {
            while (c.moveToNext()) {
                Models.AdminUser u = new Models.AdminUser();
                u.id = c.getInt(0);
                u.name = c.getString(1);
                u.phone = c.getString(2);
                u.tariffName = c.getString(3);
                u.balance = c.getDouble(4);
                u.purchases = c.getInt(5);
                u.spent = c.getDouble(6);
                list.add(u);
            }
        }
        return list;
    }

    /** Админ: кез келген абонентке баланс қосу (UPDATE + INSERT). */
    public void adminAddBalance(int userId, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            setBalance(db, userId, balanceOf(db, userId) + amount);
            addTx(db, userId, Models.Tx.BONUS, "Админ", amount, now());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Админ: абоненттің тарифін төлемсіз ауыстыру (UPDATE). */
    public void adminChangeTariff(int userId, Models.Tariff tariff) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues user = new ContentValues();
            user.put("tariff_id", tariff.id);
            db.update("users", user, "id = ?", new String[]{String.valueOf(userId)});

            ContentValues usage = new ContentValues();
            usage.put("internet_left", tariff.internetGb);
            usage.put("minutes_left", tariff.minutes);
            usage.put("sms_left", tariff.sms);
            db.update("usage", usage, "user_id = ?", new String[]{String.valueOf(userId)});

            addTx(db, userId, Models.Tx.TARIFF, tariff.name, 0, now());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Админ: абонентті және оның барлық деректерін өшіру (4 кестеден DELETE). */
    public void adminDeleteUser(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(userId)};
        db.beginTransaction();
        try {
            db.delete("user_services", "user_id = ?", args);
            db.delete("transactions", "user_id = ?", args);
            db.delete("usage", "user_id = ?", args);
            db.delete("users", "id = ?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---------------------------------------------------------------- админ: тарифтер
    public int countUsersOnTariff(int tariffId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM users WHERE tariff_id = ?",
                new String[]{String.valueOf(tariffId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** id == 0 болса INSERT (жаңа тариф), әйтпесе UPDATE. */
    public void saveTariff(int id, String name, int price, int gb, int minutes, int sms) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("price", price);
        v.put("internet_gb", gb);
        v.put("minutes", minutes);
        v.put("sms", sms);
        SQLiteDatabase db = getWritableDatabase();
        if (id == 0) {
            v.put("description_kk", "Жаңа тариф");
            v.put("description_ru", "Новый тариф");
            db.insert("tariffs", null, v);
        } else {
            db.update("tariffs", v, "id = ?", new String[]{String.valueOf(id)});
        }
    }

    /** DELETE: тарифте абонент болса өшірмейді (false). */
    public boolean deleteTariff(int id) {
        if (countUsersOnTariff(id) > 0) return false;
        getWritableDatabase().delete("tariffs", "id = ?", new String[]{String.valueOf(id)});
        return true;
    }

    // ---------------------------------------------------------------- админ: қызметтер
    /** Әр жол: {id, атауы, бағасы, қосқан абонент саны}. LEFT JOIN + GROUP BY. */
    public List<String[]> adminServices() {
        List<String[]> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT s.id, s.name_kk, s.price, COUNT(us.user_id) "
                + "FROM services s LEFT JOIN user_services us ON us.service_id = s.id "
                + "GROUP BY s.id ORDER BY s.id", null)) {
            while (c.moveToNext()) {
                list.add(new String[]{String.valueOf(c.getInt(0)), c.getString(1),
                        String.valueOf((int) c.getDouble(2)), String.valueOf(c.getInt(3))});
            }
        }
        return list;
    }

    /** id == 0 болса INSERT, әйтпесе UPDATE. */
    public void saveService(int id, String name, int price) {
        ContentValues v = new ContentValues();
        v.put("name_kk", name);
        v.put("price", price);
        SQLiteDatabase db = getWritableDatabase();
        if (id == 0) {
            v.put("name_ru", name);
            v.put("description", "Қосымша қызмет");
            db.insert("services", null, v);
        } else {
            db.update("services", v, "id = ?", new String[]{String.valueOf(id)});
        }
    }

    /** DELETE: қызмет және оны қосқан абоненттердегі байланыс. */
    public void deleteService(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("user_services", "service_id = ?", new String[]{String.valueOf(id)});
            db.delete("services", "id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** DELETE: бір жазба. */
    public void deleteTransaction(int id) {
        getWritableDatabase().delete("transactions", "id = ?", new String[]{String.valueOf(id)});
    }

    /** DELETE: абоненттің барлық жазбасы. */
    public void clearTransactions(int userId) {
        getWritableDatabase().delete("transactions", "user_id = ?", new String[]{String.valueOf(userId)});
    }
}
