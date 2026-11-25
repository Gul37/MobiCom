package com.example.mobicom;

import java.io.Serializable;

public final class Models {

    private Models() { }

    public static class Tariff implements Serializable {
        private static final long serialVersionUID = 1L;

        public final int id, price, internetGb, minutes, sms;
        public final String name, descKk, descRu;

        public Tariff(int id, String name, int price, int internetGb, int minutes, int sms,
                      String descKk, String descRu) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.internetGb = internetGb;
            this.minutes = minutes;
            this.sms = sms;
            this.descKk = descKk;
            this.descRu = descRu;
        }

        public String desc(boolean ru) { return ru ? descRu : descKk; }
    }

    public static class Service {
        public final int id, price;
        public final String nameKk, nameRu, description;
        public final boolean active;

        public Service(int id, String nameKk, String nameRu, int price, String description, boolean active) {
            this.id = id;
            this.nameKk = nameKk;
            this.nameRu = nameRu;
            this.price = price;
            this.description = description;
            this.active = active;
        }

        public String name(boolean ru) { return ru ? nameRu : nameKk; }
    }

    public static class Tx {
        public static final String TOPUP = "TOPUP";
        public static final String TARIFF = "TARIFF";
        public static final String SERVICE = "SERVICE";
        public static final String BONUS = "BONUS";

        public final int id;
        public final String type, title, createdAt;
        public final double amount;

        public Tx(int id, String type, String title, double amount, String createdAt) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.amount = amount;
            this.createdAt = createdAt;
        }
    }

    /** Админ тізіміндегі абонент: сатып алу саны және жұмсаған сомасымен. */
    public static class AdminUser {
        public int id, purchases;
        public double balance, spent;
        public String name, phone, tariffName, password;
    }

    public static class User {
        public int id, tariffId, tariffGb, tariffMinutes, tariffSms, minutesLeft, smsLeft;
        public double balance, internetLeft;
        public String name, phone, tariffName, password;
    }
}

