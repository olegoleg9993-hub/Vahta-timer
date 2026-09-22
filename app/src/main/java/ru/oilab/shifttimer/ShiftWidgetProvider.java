package ru.oilab.shifttimer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Locale;

public class ShiftWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ShiftWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        ShiftPreferences.ShiftData data = ShiftPreferences.load(context);
        long now = System.currentTimeMillis();
        SalaryCalculator.Result result = SalaryCalculator.calculate(data.startMillis, data.endMillis,
                now, data.monthlySalary, ZoneId.systemDefault());
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.shift_widget);

        long target = now < data.startMillis ? data.startMillis : data.endMillis;
        long base = SystemClock.elapsedRealtime() + Math.max(0L, target - now);
        views.setChronometer(R.id.widget_countdown, base, null, target > now);
        views.setChronometerCountDown(R.id.widget_countdown, true);
        views.setTextViewText(R.id.widget_label, now < data.startMillis ? "ДО НАЧАЛА" :
                now >= data.endMillis ? "ВАХТА ЗАВЕРШЕНА" : "ДОМОЙ ЧЕРЕЗ");

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        nf.setMaximumFractionDigits(0);
        views.setTextViewText(R.id.widget_earned, "Заработано  " + nf.format(result.earned) + " ₽");

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(id, views);
    }
}
