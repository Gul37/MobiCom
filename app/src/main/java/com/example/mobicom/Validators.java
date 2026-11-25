package com.example.mobicom;

import java.util.Calendar;

public final class Validators {

    public static final int MIN_AMOUNT = 100;
    public static final int MAX_AMOUNT = 100000;
    /** Нөмірде ең көбі 11 сан болады (мысалы 8 700 123 45 67). */
    public static final int MAX_PHONE_DIGITS = 11;
    /** Админ кіру нөмірі. SMS-код әдеттегідей хабарландыру арқылы келеді. */
    public static final String ADMIN_PHONE = "0000000000";

    /** Пароль кемінде 6 таңба болуы керек. */
    public static final int MIN_PASSWORD = 6;

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= MIN_PASSWORD && password.length() <= 30;
    }

    private Validators() { }

    // ------------------------------------------------------------ телефон нөмірі
    /** Мәтіндегі сандарды санайды: "+7 (701) 123-45-67" -> 11. */
    public static int digitCount(String raw) {
        if (raw == null) return 0;
        int n = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (Character.isDigit(raw.charAt(i))) n++;
        }
        return n;
    }

    /** 11 саннан артық па? Онда қате көрсетеміз. */
    public static boolean isPhoneTooLong(String raw) {
        return digitCount(raw) > MAX_PHONE_DIGITS;
    }

    /** Әріптер мен белгілерді алып тастап, соңғы 10 санды қалдырады: "+7 701 123 45 67" -> "7011234567". */
    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits;
    }

    /**
     * Дұрыс нөмір: 10 сан, немесе 11 сан (бірінші сан 7 не 8).
     * 11-ден артық, 10-нан кем немесе 11 сан басқа санмен басталса, қате.
     */
    public static boolean isPhoneValid(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() == 10) return true;
        return digits.length() == 11 && (digits.charAt(0) == '7' || digits.charAt(0) == '8');
    }

    // ------------------------------------------------------------ сома
    public static boolean isAmount(int amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }

    // ------------------------------------------------------------ банк картасы
    /** Мәтіндегі сандарды ғана қалдырады: "4242 4242 4242 4242" -> "4242424242424242". */
    public static String digitsOnly(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    /** Карта нөмірі: 16 сан және Luhn алгоритмі бойынша дұрыс. */
    public static boolean isCardNumber(String raw) {
        String d = digitsOnly(raw);
        if (d.length() != 16) return false;
        int sum = 0;
        boolean doubleIt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (doubleIt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** Карта түрі: 4 -> Visa, 51-55 және 2 -> Mastercard, қалғаны -> бос. */
    public static String cardBrand(String raw) {
        String d = digitsOnly(raw);
        if (d.startsWith("4")) return "Visa";
        if (d.startsWith("2")) return "Mastercard";
        if (d.length() >= 2 && d.charAt(0) == '5' && d.charAt(1) >= '1' && d.charAt(1) <= '5') return "Mastercard";
        return "";
    }

    /** Жарамдылық мерзімі: "MM/YY" форматында, ай 1-12, мерзімі өтпеген. */
    public static boolean isExpiry(String text) {
        if (text == null || !text.matches("\\d{2}/\\d{2}")) return false;
        int month = Integer.parseInt(text.substring(0, 2));
        int year = 2000 + Integer.parseInt(text.substring(3, 5));
        if (month < 1 || month > 12) return false;
        Calendar now = Calendar.getInstance();
        int nowYear = now.get(Calendar.YEAR);
        int nowMonth = now.get(Calendar.MONTH) + 1;
        return year > nowYear || (year == nowYear && month >= nowMonth);
    }

    public static boolean isCvv(String text) {
        return text != null && text.matches("\\d{3}");
    }

    /** Карта иесінің аты: латын әріптері мен бос орын, кемінде 3 символ. */
    public static boolean isCardHolder(String text) {
        return text != null && text.trim().matches("[A-Za-z ]{3,26}");
    }
}
