package kz.mobicom.app;

public final class Validators {

    public static final int MIN_AMOUNT = 100;
    public static final int MAX_AMOUNT = 100000;
    /** Админ кіру нөмірі (код әдеттегідей 1234). */
    public static final String ADMIN_PHONE = "0000000000";

    private Validators() { }

    /** Әріптер мен белгілерді алып тастап, соңғы 10 санды қалдырады: "+7 701 123 45 67" -> "7011234567". */
    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits;
    }

    public static boolean isPhone(String digits) {
        return digits != null && digits.matches("\\d{10}");
    }

    public static boolean isAmount(int amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }
}
