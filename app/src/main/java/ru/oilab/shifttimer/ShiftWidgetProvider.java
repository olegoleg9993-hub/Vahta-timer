package ru.oilab.shifttimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Locale;

public class ShiftWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_DAY_TICK = "ru.oilab.shifttimer.DAY_TICK";
    private static final String ACTION_REFRESH = "ru.oilab.shifttimer.REFRESH_WIDGET";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                     int appWidgetId, Bundle newOptions) {
        update(context, manager, appWidgetId);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || ACTION_DAY_TICK.equals(action)
                || ACTION_REFRESH.equals(action)) {
            updateAll(context);
        }
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
        long remaining = Math.max(0L, target - now);
        long totalSeconds = remaining / 1000L;
        long days = totalSeconds / 86400L;
        long withinDayMillis = remaining % 86400000L;
        if (remaining > 0 && withinDayMillis < 1000L && days > 0) {
            days -= 1;
            withinDayMillis = 86400000L;
        }
        long base = SystemClock.elapsedRealtime() + withinDayMillis;

        views.setTextViewText(R.id.widget_days, String.format(new Locale("ru", "RU"), "%d ДН · ", days));
        views.setChronometer(R.id.widget_countdown, base, null, target > now);
        views.setChronometerCountDown(R.id.widget_countdown, true);
        views.setTextViewText(R.id.widget_label, now < data.startMillis ? "ДО НАЧАЛА ВАХТЫ" :
                now >= data.endMillis ? "ВАХТА ЗАВЕРШЕНА" : "ДОМОЙ ЧЕРЕЗ");

        Locale ru = new Locale("ru", "RU");
        NumberFormat money = NumberFormat.getNumberInstance(ru);
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        views.setTextViewText(R.id.widget_earned,
                "Заработано  " + money.format(result.earned) + " ₽");

        int preciseProgress = (int) Math.round(Math.max(0, Math.min(1, result.progress)) * 100000d);
        views.setProgressBar(R.id.widget_progress, 100000, preciseProgress, false);
        DecimalFormat percent = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(ru));
        views.setTextViewText(R.id.widget_percent,
                percent.format(result.progress * 100d) + "% вахты завершено");

        if (ShiftPreferences.hasWidgetPhoto(context)) {
            Bitmap photo = createWidgetPhoto(context, manager.getAppWidgetOptions(id));
            if (photo != null) {
                views.setViewVisibility(R.id.widget_photo, View.VISIBLE);
                views.setImageViewBitmap(R.id.widget_photo, photo);
            } else views.setViewVisibility(R.id.widget_photo, View.GONE);
        } else views.setViewVisibility(R.id.widget_photo, View.GONE);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);

        Intent refreshIntent = new Intent(context, ShiftWidgetProvider.class)
                .setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(context, 9000 + id,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending);
        manager.updateAppWidget(id, views);
        scheduleDayBoundary(context, id, remaining);
    }

    private static void scheduleDayBoundary(Context context, int id, long remaining) {
        if (remaining <= 0) return;
        long untilBoundary = remaining % 86400000L;
        if (untilBoundary < 1500L) untilBoundary = 86400000L;
        Intent tick = new Intent(context, ShiftWidgetProvider.class).setAction(ACTION_DAY_TICK);
        PendingIntent pending = PendingIntent.getBroadcast(context, 7000 + id, tick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + untilBoundary + 1000L, pending);
    }

    private static Bitmap createWidgetPhoto(Context context, Bundle options) {
        try {
            File file = new File(context.getFilesDir(), ShiftPreferences.WIDGET_PHOTO_FILE);
            Bitmap source = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (source == null) return null;
            float density = context.getResources().getDisplayMetrics().density;
            int widthDp = Math.max(220, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300));
            int heightDp = Math.max(100, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130));
            int width = Math.min(560, Math.max(360, Math.round(widthDp * density)));
            int height = Math.min(360, Math.max(180, Math.round(heightDp * density)));
            float pixels = width * (float) height;
            if (pixels > 180000f) {
                float factor = (float) Math.sqrt(180000f / pixels);
                width = Math.round(width * factor);
                height = Math.round(height * factor);
            }

            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Path clip = new Path();
            float radius = Math.max(18f, 24f * density * width / Math.max(1f, widthDp * density));
            clip.addRoundRect(new RectF(0, 0, width, height), radius, radius, Path.Direction.CW);
            canvas.clipPath(clip);
            float scale = Math.max(width / (float) source.getWidth(), height / (float) source.getHeight());
            float drawW = source.getWidth() * scale;
            float drawH = source.getHeight() * scale;
            RectF dst = new RectF((width - drawW) / 2f, (height - drawH) / 2f,
                    (width + drawW) / 2f, (height + drawH) / 2f);
            canvas.drawBitmap(source, null, dst, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            source.recycle();
            return output;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
