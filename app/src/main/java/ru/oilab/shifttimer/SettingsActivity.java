package ru.oilab.shifttimer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int REQUEST_PHOTO = 301;
    private static final int REQUEST_CROP = 302;
    private final Locale ru = new Locale("ru", "RU");
    private final DateTimeFormatter display = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm", ru);
    private long startMillis;
    private long endMillis;
    private Button startButton;
    private Button endButton;
    private EditText salaryInput;
    private TextView photoStatus;
    private float d;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
        getWindow().setStatusBarColor(Color.rgb(7, 9, 13));
        getWindow().setNavigationBarColor(Color.rgb(7, 9, 13));
        d = getResources().getDisplayMetrics().density;
        ShiftPreferences.ShiftData data = ShiftPreferences.load(this);
        startMillis = data.startMillis;
        endMillis = data.endMillis;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7, 9, 13));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(26), dp(24), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView back = text("‹  Назад", 15, 0xFFD8B56C, false);
        back.setPadding(0, dp(8), 0, dp(18));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        root.addView(text("Настройка вахты", 31, 0xFFF4F1E8, true));
        TextView intro = text("Укажите период и месячную зарплату. Расчёт идёт по фактическим календарным дням каждого месяца.",
                14, 0xFF8B919C, false);
        intro.setLineSpacing(0, 1.25f);
        intro.setPadding(0, dp(10), 0, dp(27));
        root.addView(intro);

        root.addView(label("НАЧАЛО ВАХТЫ"));
        startButton = dateButton();
        startButton.setOnClickListener(v -> chooseDateTime(true));
        root.addView(startButton, margins(dp(58), dp(0), dp(0), dp(19)));

        root.addView(label("ОКОНЧАНИЕ ВАХТЫ"));
        endButton = dateButton();
        endButton.setOnClickListener(v -> chooseDateTime(false));
        root.addView(endButton, margins(dp(58), dp(0), dp(0), dp(25)));

        root.addView(label("ЗАРПЛАТА ЗА КАЛЕНДАРНЫЙ МЕСЯЦ"));
        salaryInput = new EditText(this);
        salaryInput.setTextColor(0xFFF4F1E8);
        salaryInput.setTextSize(23);
        salaryInput.setSingleLine(true);
        salaryInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        salaryInput.setPadding(dp(18), 0, dp(18), 0);
        salaryInput.setBackground(fieldBackground());
        salaryInput.setText(formatInput(data.monthlySalary));
        salaryInput.setSelectAllOnFocus(true);
        root.addView(salaryInput, margins(dp(62), 0, 0, dp(8)));

        TextView hint = text("Например: 210 000 ₽. Полный календарный месяц всегда равен указанной сумме.",
                12, 0xFF6E7580, false);
        hint.setPadding(dp(4), dp(2), 0, dp(30));
        root.addView(hint);

        root.addView(label("ФОН ВИДЖЕТА"));
        photoStatus = text("", 12, 0xFF87909C, false);
        photoStatus.setPadding(dp(4), 0, 0, dp(10));
        root.addView(photoStatus);

        LinearLayout photoRow = new LinearLayout(this);
        photoRow.setOrientation(LinearLayout.HORIZONTAL);
        Button choosePhoto = secondaryButton("Выбрать фото");
        Button removePhoto = secondaryButton("Убрать фон");
        choosePhoto.setOnClickListener(v -> chooseWidgetPhoto());
        removePhoto.setOnClickListener(v -> {
            ShiftPreferences.removeWidgetPhoto(this);
            ShiftWidgetProvider.updateAll(this);
            refreshPhotoStatus();
        });
        photoRow.addView(choosePhoto, weightedButton(0, dp(5)));
        photoRow.addView(removePhoto, weightedButton(dp(5), 0));
        root.addView(photoRow, margins(dp(54), 0, 0, dp(24)));

        Button save = new Button(this);
        save.setText("СОХРАНИТЬ И ЗАПУСТИТЬ");
        save.setTextColor(0xFF0B0D11);
        save.setTextSize(13);
        save.setAllCaps(false);
        save.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable saveBg = rounded(0xFFD8B56C, 20, 0, 0);
        save.setBackground(saveBg);
        save.setOnClickListener(v -> save());
        root.addView(save, margins(dp(62), 0, 0, 0));

        Button widget = new Button(this);
        widget.setText("Добавить виджет на главный экран");
        widget.setTextColor(0xFFD8B56C);
        widget.setTextSize(13);
        widget.setAllCaps(false);
        widget.setBackground(rounded(0x00121720, 20, 0x55D8B56C, 1));
        widget.setOnClickListener(v -> pinWidget());
        root.addView(widget, margins(dp(58), 0, dp(14), 0));

        TextView supportTitle = label("ПОДДЕРЖАТЬ ПРОЕКТ");
        supportTitle.setPadding(dp(4), dp(24), 0, dp(8));
        root.addView(supportTitle);

        Button support = new Button(this);
        support.setText("Разработчику на доширак  🍜");
        support.setTextColor(0xFFE6C77E);
        support.setTextSize(13);
        support.setAllCaps(false);
        support.setBackground(rounded(0xFF111720, 18, 0x556F5A31, 1));
        support.setOnClickListener(v -> Toast.makeText(this,
                "Подключим безопасную оплату через RuStore после публикации приложения",
                Toast.LENGTH_LONG).show());
        root.addView(support, margins(dp(54), 0, dp(8), 0));

        TextView supportHint = text("Добровольная поддержка без рекламы и платных ограничений. Скоро.",
                11, 0xFF6E7580, false);
        supportHint.setPadding(dp(5), dp(7), dp(5), 0);
        root.addView(supportHint);

        refreshDates();
        refreshPhotoStatus();
        setContentView(scroll);
        } catch (Throwable error) {
            TextView crash = new TextView(this);
            crash.setText("Ошибка экрана настроек:\n\n" + android.util.Log.getStackTraceString(error));
            crash.setTextColor(Color.WHITE);
            crash.setTextSize(12);
            crash.setPadding(24, 48, 24, 24);
            crash.setBackgroundColor(0xFF4A1010);
            setContentView(crash);
        }
    }

    private void chooseWidgetPhoto() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        startActivityForResult(pick, REQUEST_PHOTO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQUEST_PHOTO && intent != null && intent.getData() != null) {
            Intent crop = new Intent(this, CropPhotoActivity.class);
            crop.setData(intent.getData());
            crop.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(crop, REQUEST_CROP);
        } else if (requestCode == REQUEST_CROP) {
            refreshPhotoStatus();
            ShiftWidgetProvider.updateAll(this);
        }
    }

    private void refreshPhotoStatus() {
        if (photoStatus == null) return;
        photoStatus.setText(ShiftPreferences.hasWidgetPhoto(this)
                ? "Фото установлено. Оно автоматически подгоняется под размер виджета."
                : "Без фото используется тёмный премиальный фон.");
    }

    private void chooseDateTime(boolean start) {
        long current = start ? startMillis : endMillis;
        ZonedDateTime z = Instant.ofEpochMilli(current).atZone(ZoneId.systemDefault());
        DatePickerDialog dateDialog = new DatePickerDialog(this, (picker, year, month, day) -> {
            TimePickerDialog timeDialog = new TimePickerDialog(this, (time, hour, minute) -> {
                ZonedDateTime selected = ZonedDateTime.of(year, month + 1, day, hour, minute, 0, 0,
                        ZoneId.systemDefault());
                if (start) startMillis = selected.toInstant().toEpochMilli();
                else endMillis = selected.toInstant().toEpochMilli();
                refreshDates();
            }, z.getHour(), z.getMinute(), true);
            timeDialog.show();
        }, z.getYear(), z.getMonthValue() - 1, z.getDayOfMonth());
        dateDialog.show();
    }

    private void save() {
        String raw = salaryInput.getText().toString().replace(" ", "").replace(',', '.');
        double salary;
        try { salary = Double.parseDouble(raw); }
        catch (Exception e) { salary = 0; }
        if (endMillis <= startMillis) {
            Toast.makeText(this, "Окончание должно быть позже начала вахты", Toast.LENGTH_LONG).show();
            return;
        }
        if (salary <= 0) {
            Toast.makeText(this, "Укажите зарплату больше нуля", Toast.LENGTH_LONG).show();
            return;
        }
        ShiftPreferences.save(this, new ShiftPreferences.ShiftData(startMillis, endMillis, salary));
        ShiftWidgetProvider.updateAll(this);
        finish();
    }

    private void pinWidget() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        ComponentName provider = new ComponentName(this, ShiftWidgetProvider.class);
        if (manager != null && manager.isRequestPinAppWidgetSupported()) {
            Intent callbackIntent = new Intent(this, MainActivity.class);
            PendingIntent callback = PendingIntent.getActivity(this, 11, callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            manager.requestPinAppWidget(provider, null, callback);
        } else {
            Toast.makeText(this, "Добавьте виджет «Вахта» через меню виджетов телефона",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshDates() {
        startButton.setText(display.format(Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault())));
        endButton.setText(display.format(Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault())));
    }

    private Button dateButton() {
        Button b = new Button(this);
        b.setTextColor(0xFFF4F1E8);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(18), 0, dp(18), 0);
        b.setBackground(fieldBackground());
        return b;
    }

    private Button secondaryButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(0xFFE2BF75);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackground(rounded(0xFF121923, 16, 0x445B4B31, 1));
        return b;
    }

    private LinearLayout.LayoutParams weightedButton(int left, int right) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f);
        p.setMargins(left, 0, right, 0);
        return p;
    }

    private TextView label(String value) {
        TextView t = text(value, 11, 0xFFD8B56C, true);
        t.setLetterSpacing(.12f);
        t.setPadding(dp(3), 0, 0, dp(9));
        return t;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable fieldBackground() { return rounded(0xFF121720, 18, 0x332F3948, 1); }
    private GradientDrawable rounded(int fill, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), strokeColor);
        return g;
    }

    private LinearLayout.LayoutParams margins(int height, int l, int t, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        p.setMargins(l, t, 0, bottom);
        return p;
    }

    private String formatInput(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(ru);
        nf.setMaximumFractionDigits(2);
        nf.setGroupingUsed(false);
        return nf.format(value);
    }

    private int dp(int value) { return Math.round(value * d); }
}
