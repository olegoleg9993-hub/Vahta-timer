package ru.oilab.shifttimer;

import android.app.Activity;
import android.content.Intent;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
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
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(52), dp(22), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView brand = text("ВАХТА", 15, 0xFFD8B56C, true);
        brand.setLetterSpacing(.18f);
        root.addView(brand);
        periodText = text("", 13, 0xFF7D8591, false);
        periodText.setPadding(0, dp(12), 0, dp(42));
        root.addView(periodText);

        LinearLayout timerCard = card();
        stateText = text("ДОМОЙ ЧЕРЕЗ", 12, 0xFFD8B56C, true);
        stateText.setLetterSpacing(.10f);
        timerCard.addView(stateText);
        countdownText = text("00:00:00:00", 31, 0xFFF4F1E8, true);
        countdownText.setPadding(0, dp(15), 0, dp(8));
        timerCard.addView(countdownText);
        TextView units = text("ДНИ     ЧАСЫ     МИН     СЕК", 10, 0xFF6E7682, true);
        units.setLetterSpacing(.05f);
        timerCard.addView(units);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFD8B56C));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(5));
        progressParams.setMargins(0, dp(28), 0, 0);
        timerCard.addView(progress, progressParams);
        root.addView(timerCard, blockParams(dp(230), 0, dp(20)));

        LinearLayout moneyCard = card();
        TextView earnedLabel = text("ЗАРАБОТАНО", 11, 0xFF8B929E, true);
        earnedLabel.setGravity(Gravity.START);
        moneyCard.addView(earnedLabel, new LinearLayout.LayoutParams(-1, -2));
        earnedText = text("0,00 ₽", 31, 0xFFF4F1E8, true);
        earnedText.setGravity(Gravity.START);
        earnedText.setPadding(0, dp(13), 0, dp(29));
        moneyCard.addView(earnedText, new LinearLayout.LayoutParams(-1, -2));
        TextView totalLabel = text("К КОНЦУ ВАХТЫ", 10, 0xFF6F7682, true);
        totalLabel.setGravity(Gravity.START);
        moneyCard.addView(totalLabel, new LinearLayout.LayoutParams(-1, -2));
        totalText = text("0,00 ₽", 18, 0xFFD8B56C, true);
        totalText.setGravity(Gravity.START);
        totalText.setPadding(0, dp(8), 0, 0);
        moneyCard.addView(totalText, new LinearLayout.LayoutParams(-1, -2));
        root.addView(moneyCard, blockParams(dp(245), 0, dp(24)));

        Button settings = new Button(this);
        settings.setText("НАСТРОИТЬ ВАХТУ");
        settings.setTextColor(0xFF0B0D11);
        settings.setTextSize(13);
        settings.setTypeface(null, android.graphics.Typeface.BOLD);
        settings.setBackground(round(0xFFD8B56C, 20, 0, 0));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(settings, blockParams(dp(62), 0, 0));

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
        progress.setProgress((int) Math.round(r.progress * 1000));
        ZoneId zone = ZoneId.systemDefault();
        periodText.setText(date.format(Instant.ofEpochMilli(data.startMillis).atZone(zone))
                + "  —  " + date.format(Instant.ofEpochMilli(data.endMillis).atZone(zone)));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(28), dp(24), dp(28));
        card.setBackground(round(0xDD151A22, 25, 0x33D8B56C, 1));
        return card;
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
