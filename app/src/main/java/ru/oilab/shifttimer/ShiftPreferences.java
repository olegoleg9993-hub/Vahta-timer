package ru.oilab.shifttimer;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.time.ZonedDateTime;

public final class ShiftPreferences {
    private static final String FILE = "shift_settings";
    private static final String START = "start_epoch";
    private static final String END = "end_epoch";
    private static final String SALARY = "monthly_salary";
    private static final String CONFIGURED = "configured";
    private static final String WIDGET_PHOTO = "widget_photo";
    private static final String SUPPORTER = "supporter";
    public static final String WIDGET_PHOTO_FILE = "widget_background.jpg";

    private ShiftPreferences() {}

    public static ShiftData load(Context context) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        long defaultStart = ZonedDateTime.now().withSecond(0).withNano(0).toInstant().toEpochMilli();
        long defaultEnd = ZonedDateTime.now().plusDays(60).withSecond(0).withNano(0).toInstant().toEpochMilli();
        return new ShiftData(
                p.getLong(START, defaultStart),
                p.getLong(END, defaultEnd),
                Double.longBitsToDouble(p.getLong(SALARY, Double.doubleToLongBits(210000.0)))
        );
    }

    public static void save(Context context, ShiftData data) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putLong(START, data.startMillis)
                .putLong(END, data.endMillis)
                .putLong(SALARY, Double.doubleToRawLongBits(data.monthlySalary))
                .putBoolean(CONFIGURED, true)
                .apply();
    }

    public static boolean isConfigured(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(CONFIGURED, false);
    }

    public static boolean hasWidgetPhoto(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(WIDGET_PHOTO, false)
                && new File(context.getFilesDir(), WIDGET_PHOTO_FILE).isFile();
    }

    public static void setWidgetPhoto(Context context, boolean enabled) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(WIDGET_PHOTO, enabled)
                .apply();
    }

    public static void removeWidgetPhoto(Context context) {
        File photo = new File(context.getFilesDir(), WIDGET_PHOTO_FILE);
        if (photo.exists()) photo.delete();
        setWidgetPhoto(context, false);
    }

    static boolean isSupporter(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(SUPPORTER, false);
    }

    // Call only after the store confirms a completed purchase.
    static void markSupporterAfterPurchase(Context context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(SUPPORTER, true)
                .apply();
        ShiftWidgetProvider.updateAll(context);
    }

    public static final class ShiftData {
        public final long startMillis;
        public final long endMillis;
        public final double monthlySalary;

        public ShiftData(long startMillis, long endMillis, double monthlySalary) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.monthlySalary = monthlySalary;
        }
    }
}
