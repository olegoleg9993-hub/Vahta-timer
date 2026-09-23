package ru.oilab.shifttimer;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Locale ru = new Locale("ru", "RU");
    private final NumberFormat money = NumberFormat.getNumberInstance(ru);
    private final DateTimeFormatter date = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", ru);
    private TextView stateText;
    private TextView periodText;
    private TextView countdownText;
    private TextView earnedText;
    private TextView totalText;
    private ProgressBar progress;
    private TextView progressText;
    private float density;
    private ShiftPreferences.ShiftData data;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateValues();
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            buildScreen();
        } catch (Throwable error) {
            showCrash(error);
        }
    }

    private void buildScreen() {
        density = getResources().getDisplayMetrics().density;
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        getWindow().setStatusBarColor(0xFF07090D);
        getWindow().setNavigationBarColor(0xFF07090D);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF131A25, 0xFF07090D}));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        root.setPadding(dp(18), dp(15), dp(18), dp(15));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            root.setPadding(dp(18), insets.getSystemWindowInsetTop() + dp(15),
                    dp(18), insets.getSystemWindowInsetBottom() + dp(15));
            return insets;
        });

        TextView brand = text("ВАХТА  •  LIVE", 13, 0xFFE2BF75, true);
        brand.setLetterSpacing(.18f);
        root.addView(brand);
        periodText = text("", 13, 0xFF7D8591, false);
        periodText.setPadding(0, dp(5), 0, dp(12));
        root.addView(periodText);

        LinearLayout timerCard = card();
        stateText = text("ДОМОЙ ЧЕРЕЗ", 12, 0xFFD8B56C, true);
        stateText.setLetterSpacing(.10f);
        timerCard.addView(stateText);
        countdownText = text("00:00:00:00", 31, 0xFFF4F1E8, true);
        countdownText.setPadding(0, dp(8), 0, dp(4));
        timerCard.addView(countdownText);
        TextView units = text("ДНИ     ЧАСЫ     МИН     СЕК", 10, 0xFF6E7682, true);
        units.setLetterSpacing(.05f);
        timerCard.addView(units);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100000);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFD8B56C));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(5));
        progressParams.setMargins(0, dp(13), 0, 0);
        timerCard.addView(progress, progressParams);
        progressText = text("0,000% ВАХТЫ ЗАВЕРШЕНО", 9, 0xFF7F8896, true);
        progressText.setLetterSpacing(.05f);
        progressText.setPadding(0, dp(5), 0, 0);
        timerCard.addView(progressText);
        timerCard.setMinimumHeight(dp(170));
        root.addView(timerCard, blockParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, dp(12)));

        LinearLayout moneyCard = card();
        TextView earnedLabel = text("ЗАРАБОТАНО", 11, 0xFF8B929E, true);
        earnedLabel.setGravity(Gravity.START);
        moneyCard.addView(earnedLabel, new LinearLayout.LayoutParams(-1, -2));
        earnedText = text("0,00 ₽", 31, 0xFFF4F1E8, true);
        earnedText.setGravity(Gravity.START);
        earnedText.setPadding(0, dp(8), 0, dp(13));
        moneyCard.addView(earnedText, new LinearLayout.LayoutParams(-1, -2));
        TextView totalLabel = text("К КОНЦУ ВАХТЫ", 10, 0xFF6F7682, true);
        totalLabel.setGravity(Gravity.START);
        moneyCard.addView(totalLabel, new LinearLayout.LayoutParams(-1, -2));
        totalText = text("0,00 ₽", 18, 0xFFD8B56C, true);
        totalText.setGravity(Gravity.START);
        totalText.setPadding(0, dp(5), 0, 0);
        moneyCard.addView(totalText, new LinearLayout.LayoutParams(-1, -2));
        moneyCard.setMinimumHeight(dp(164));
        root.addView(moneyCard, blockParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, dp(14)));

        Button settings = new Button(this);
        settings.setText("НАСТРОИТЬ ВАХТУ");
        settings.setTextColor(0xFF0B0D11);
        settings.setTextSize(13);
        settings.setTypeface(null, android.graphics.Typeface.BOLD);
        settings.setBackground(round(0xFFD8B56C, 20, 0, 0));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(settings, blockParams(dp(48), 0, dp(7)));

        Button widget = new Button(this);
        widget.setText("Добавить виджет");
        widget.setAllCaps(false);
        widget.setTextColor(0xFFE2BF75);
        widget.setTextSize(13);
        widget.setBackground(round(0x66121923, 18, 0x665B4B31, 1));
        widget.setOnClickListener(v -> pinWidget());
        root.addView(widget, blockParams(dp(44), 0, dp(3)));

        Button support = new Button(this);
        support.setText("Разработчику на доширак  🍜");
        support.setTextColor(0xFFE2BF75);
        support.setTextSize(12);
        support.setAllCaps(false);
        support.setTypeface(null, android.graphics.Typeface.BOLD);
        support.setBackground(round(0x66121923, 18, 0x665B4B31, 1));
        support.setOnClickListener(v -> showSupportInfo());
        root.addView(support, blockParams(dp(45), 0, 0));

        TextView feedbackTitle = text("Нашли ошибку или есть идея?", 16, 0xFFF4F1E8, true);
        feedbackTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(feedbackTitle);
        TextView feedbackHint = text("Напишите разработчику в MAX. Расскажите, что не работает или чего не хватает в приложении.",
                12, 0xFF9AA2AD, false);
        root.addView(feedbackHint);
        Button feedback = new Button(this);
        feedback.setText("Написать в MAX");
        feedback.setAllCaps(false);
        feedback.setTextColor(0xFFE2BF75);
        feedback.setTextSize(13);
        feedback.setBackground(round(0x66121923, 18, 0x665B4B31, 1));
        feedback.setOnClickListener(v -> sendFeedback());
        root.addView(feedback, blockParams(dp(42), dp(7), 0));

        setContentView(scroll);
        data = ShiftPreferences.load(this);
        updateValues();
    }

    @Override protected void onResume() {
        super.onResume();
        if (countdownText == null) return;
        data = ShiftPreferences.load(this);
        handler.removeCallbacks(ticker);
        ticker.run();
        ShiftWidgetProvider.updateAll(this);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void updateValues() {
        if (data == null || countdownText == null) return;
        long now = System.currentTimeMillis();
        SalaryCalculator.Result r = SalaryCalculator.calculate(data.startMillis, data.endMillis,
                now, data.monthlySalary, ZoneId.systemDefault());
        boolean waiting = now < data.startMillis;
        boolean finished = now >= data.endMillis;
        stateText.setText(finished ? "ВАХТА ЗАВЕРШЕНА" : waiting ? "ДО НАЧАЛА" : "ДОМОЙ ЧЕРЕЗ");
        long millis = waiting ? data.startMillis - now : r.remainingMillis;
        long seconds = Math.max(0, millis / 1000L);
        countdownText.setText(String.format(ru, "%02d:%02d:%02d:%02d",
                seconds / 86400, (seconds / 3600) % 24, (seconds / 60) % 60, seconds % 60));
        earnedText.setText(money.format(r.earned) + " ₽");
        totalText.setText(money.format(r.total) + " ₽");
        progress.setProgress((int) Math.round(r.progress * 100000));
        DecimalFormat percent = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(ru));
        progressText.setText(percent.format(r.progress * 100d) + "% ВАХТЫ ЗАВЕРШЕНО");
        ZoneId zone = ZoneId.systemDefault();
        periodText.setText(date.format(Instant.ofEpochMilli(data.startMillis).atZone(zone))
                + "  —  " + date.format(Instant.ofEpochMilli(data.endMillis).atZone(zone)));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setBackground(round(0xE6121923, 25, 0x445B4B31, 1));
        return card;
    }

    private void sendFeedback() {
        Intent openProfile = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://max.ru/u/f9LHodD0cOLhDfiOk3kt78i9rHODbyY58oho6L8RV7BgMvVKWIrsftmGgfo"));
        try {
            startActivity(openProfile);
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "Не удалось открыть ссылку MAX", Toast.LENGTH_LONG).show();
        }
    }

    private void showSupportInfo() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Поддержать разработчика")
                .setMessage("Спасибо! Оплата через RuStore пока не подключена. Как только она появится, поддержать проект можно будет здесь.")
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void pinWidget() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager != null && manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(new ComponentName(this, ShiftWidgetProvider.class), null, null);
        } else {
            Toast.makeText(this, "Добавьте виджет «Таймер вахтовика» через меню виджетов телефона",
                    Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), strokeColor);
        return g;
    }

    private LinearLayout.LayoutParams blockParams(int height, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        p.setMargins(0, top, 0, bottom);
        return p;
    }

    private void showCrash(Throwable error) {
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        TextView crash = new TextView(this);
        crash.setText("Ошибка запуска приложения:\n\n" + sw);
        crash.setTextColor(Color.WHITE);
        crash.setTextSize(12);
        crash.setPadding(24, 48, 24, 24);
        crash.setBackgroundColor(0xFF4A1010);
        setContentView(crash);
    }

    private int dp(int value) { return Math.round(value * density); }
}
