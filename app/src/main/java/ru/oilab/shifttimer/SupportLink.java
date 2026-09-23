package ru.oilab.shifttimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

/** Opens a public payment form. No payment credentials or in-app purchase logic are stored in the app. */
final class SupportLink {
    private SupportLink() {}

    static void open(Activity activity) {
        String url = activity.getString(R.string.support_form_url).trim();
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || !(host.equals("yookassa.ru") || host.endsWith(".yookassa.ru"))) {
            new AlertDialog.Builder(activity)
                    .setMessage("Форма поддержки пока не подключена.")
                    .setPositiveButton("Понятно", null).show();
            return;
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(activity)
                    .setMessage("Не удалось открыть форму оплаты. Проверьте, установлен ли браузер.")
                    .setPositiveButton("Понятно", null).show();
        }
    }
}
