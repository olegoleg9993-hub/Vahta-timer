package ru.oilab.shifttimer;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.ZonedDateTime;

public final class ShiftPreferences {
    private static final String FILE = "shift_settings";
    private static final String START = "start_epoch";
    private static final String END = "end_epoch";
    private static final String SALARY = "monthly_salary";
    private static final String CONFIGURED = "configured";

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
