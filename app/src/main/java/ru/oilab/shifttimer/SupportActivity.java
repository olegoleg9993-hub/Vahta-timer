package ru.oilab.shifttimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Checkout is deliberately gated until this app and its products exist in RuStore Console. */
public class SupportActivity extends Activity {
    private static final int[] AMOUNTS = {99, 199, 399, 499, 999, 1999, 3999, 5999, 9999};
    private float density;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        density = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(0xFF07090D);
        getWindow().setNavigationBarColor(0xFF07090D);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF0B1017);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(24));
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(dp(20), dp(18) + insets.getSystemWindowInsetTop(), dp(20),
                    dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        TextView back = label("‹  Назад", 16, 0xFFE2BF75, false);
        back.setOnClickListener(v -> finish());
        content.addView(back, spacing(40, 0));
        content.addView(label("Поддержать разработчика", 25, 0xFFF4F1E8, true), spacing(50, 6));
        TextView intro = label("Выберите сумму, которой хотите поддержать приложение.",
                14, 0xFFADB3BE, false);
        content.addView(intro, spacing(50, 3));

        TextView benefit = label("Любая сумма, даже 99 ₽, после успешной оплаты уберёт надпись о поддержке с виджета. Таймер и расчёт зарплаты остаются бесплатными.",
                14, 0xFFE9D3A0, false);
        benefit.setPadding(dp(17), dp(15), dp(17), dp(15));
        benefit.setBackground(round(0xFF18212B, 18, 0x665D4D35));
        content.addView(benefit, spacing(-2, 12));

        TextView heading = label("СУММА ПОДДЕРЖКИ", 11, 0xFFD8B56C, true);
        heading.setLetterSpacing(.12f);
        content.addView(heading, spacing(32, 17));

        for (int row = 0; row < 3; row++) {
            LinearLayout group = new LinearLayout(this);
            group.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 3; col++) {
                int amount = AMOUNTS[row * 3 + col];
                Button choice = new Button(this);
                choice.setText(String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,d ₽", amount));
                choice.setAllCaps(false);
                choice.setTextSize(15);
                choice.setTextColor(0xFFF4F1E8);
                choice.setBackground(round(0xFF171F29, 16, 0x665D4D35));
                choice.setOnClickListener(v -> explainCheckout());
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(62), 1);
                cell.setMargins(col == 0 ? 0 : dp(4), 0, col == 2 ? 0 : dp(4), 0);
                group.addView(choice, cell);
            }
            content.addView(group, spacing(62, row == 0 ? 0 : 9));
        }

        TextView note = label("Оплата через RuStore появится после подключения товаров в кабинете разработчика. Пока деньги не списываются.",
                12, 0xFF878F9A, false);
        note.setGravity(Gravity.CENTER);
        content.addView(note, spacing(-2, 22));
        setContentView(scroll);
    }

    private void explainCheckout() {
        new AlertDialog.Builder(this)
                .setTitle("Оплата пока недоступна")
                .setMessage("Для оплаты нужно создать товары в RuStore и привязать приложение к кабинету разработчика. Сейчас покупка не совершается и деньги не списываются.")
                .setPositiveButton("Понятно", null).show();
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams spacing(int height, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height < 0 ? -2 : dp(height));
        p.topMargin = dp(top);
        return p;
    }

    private GradientDrawable round(int color, int radius, int border) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private int dp(int n) { return Math.round(n * density); }
}
