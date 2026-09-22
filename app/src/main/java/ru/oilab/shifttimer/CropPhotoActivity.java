package ru.oilab.shifttimer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CropPhotoActivity extends Activity {
    private float d;
    private CropView cropView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF080B10);
        getWindow().setNavigationBarColor(0xFF080B10);
        d = getResources().getDisplayMetrics().density;

        Uri uri = getIntent().getData();
        Bitmap bitmap = uri == null ? null : decodePhoto(uri);
        if (bitmap == null) {
            Toast.makeText(this, "Не удалось открыть фотографию", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(22));
        root.setBackgroundColor(0xFF080B10);

        TextView title = text("Кадр для виджета", 27, 0xFFF5F1E8, true);
        root.addView(title);
        TextView hint = text("Двигайте фото пальцем. Щипком меняйте масштаб — в рамку попадёт именно то, что будет на виджете.",
                14, 0xFF9AA2AE, false);
        hint.setLineSpacing(0, 1.2f);
        hint.setPadding(0, dp(8), 0, dp(20));
        root.addView(hint);

        cropView = new CropView(this, bitmap);
        int available = getResources().getDisplayMetrics().widthPixels - dp(36);
        root.addView(cropView, new LinearLayout.LayoutParams(-1, Math.max(dp(145), available / 2)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(16), 0, dp(22));
        Button minus = smallButton("−");
        Button center = smallButton("По центру");
        Button plus = smallButton("+");
        minus.setOnClickListener(v -> cropView.zoom(.86f));
        center.setOnClickListener(v -> cropView.reset());
        plus.setOnClickListener(v -> cropView.zoom(1.16f));
        controls.addView(minus, weight());
        controls.addView(center, weight());
        controls.addView(plus, weight());
        root.addView(controls);

        Button save = new Button(this);
        save.setText("ИСПОЛЬЗОВАТЬ НА ВИДЖЕТЕ");
        save.setTextSize(13);
        save.setTextColor(0xFF111318);
        save.setTypeface(null, android.graphics.Typeface.BOLD);
        save.setBackground(round(0xFFE2BF75, 20, 0, 0));
        save.setOnClickListener(v -> saveCrop());
        root.addView(save, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView cancel = text("Отмена", 14, 0xFF9098A5, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(18), 0, dp(8));
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel);
        setContentView(root);
    }

    private void saveCrop() {
        try {
            File file = new File(getFilesDir(), ShiftPreferences.WIDGET_PHOTO_FILE);
            Bitmap out = cropView.createCrop(1200, 600);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                out.compress(Bitmap.CompressFormat.JPEG, 92, stream);
            }
            out.recycle();
            ShiftPreferences.setWidgetPhoto(this, true);
            ShiftWidgetProvider.updateAll(this);
            setResult(RESULT_OK, new Intent());
            finish();
        } catch (Throwable error) {
            Toast.makeText(this, "Не удалось сохранить фото", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap decodePhoto(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 2400) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, options);
            }
            if (bitmap == null) return null;

            int rotation = 0;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ExifInterface exif = new ExifInterface(in);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            } catch (Throwable ignored) {}
            if (rotation == 0) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (Throwable error) {
            return null;
        }
    }

    private Button smallButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(0xFFE2BF75);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(round(0xFF151B24, 14, 0x334B586A, 1));
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), stroke);
        return g;
    }

    private int dp(int value) { return Math.round(value * d); }

    private static final class CropView extends View {
        private final Bitmap bitmap;
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float minScale;
        private float scale;
        private float offsetX;
        private float offsetY;
        private float lastX;
        private float lastY;
        private float lastDistance;

        CropView(android.content.Context context, Bitmap bitmap) {
            super(context);
            this.bitmap = bitmap;
            setBackgroundColor(Color.BLACK);
            shadePaint.setColor(0x44000000);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
            borderPaint.setColor(0xFFE2BF75);
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { reset(); }

        void reset() {
            if (getWidth() == 0 || getHeight() == 0) return;
            minScale = Math.max(getWidth() / (float) bitmap.getWidth(),
                    getHeight() / (float) bitmap.getHeight());
            scale = minScale;
            offsetX = (getWidth() - bitmap.getWidth() * scale) / 2f;
            offsetY = (getHeight() - bitmap.getHeight() * scale) / 2f;
            invalidate();
        }

        void zoom(float factor) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            applyScale(factor, cx, cy);
        }

        private void applyScale(float factor, float cx, float cy) {
            float old = scale;
            float next = Math.max(minScale, Math.min(minScale * 5f, scale * factor));
            if (old <= 0 || next == old) return;
            offsetX = cx - (cx - offsetX) * (next / old);
            offsetY = cy - (cy - offsetY) * (next / old);
            scale = next;
            clamp();
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            RectF frame = new RectF(1, 1, getWidth() - 1, getHeight() - 1);
            Path clippingPath = new Path();
            clippingPath.addRoundRect(frame, 28f, 28f, Path.Direction.CW);
            canvas.clipPath(clippingPath);
            canvas.drawBitmap(bitmap, null, new RectF(offsetX, offsetY,
                    offsetX + bitmap.getWidth() * scale,
                    offsetY + bitmap.getHeight() * scale), imagePaint);
            canvas.drawRect(frame, shadePaint);
            canvas.restore();
            canvas.drawRoundRect(frame, 28f, 28f, borderPaint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX(); lastY = event.getY(); return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() >= 2) lastDistance = distance(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() >= 2) {
                        float next = distance(event);
                        if (lastDistance > 0 && next > 0) {
                            applyScale(next / lastDistance,
                                    (event.getX(0) + event.getX(1)) / 2f,
                                    (event.getY(0) + event.getY(1)) / 2f);
                        }
                        lastDistance = next;
                    } else {
                        float x = event.getX(), y = event.getY();
                        offsetX += x - lastX; offsetY += y - lastY;
                        lastX = x; lastY = y;
                        clamp(); invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    lastDistance = 0; return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private float distance(MotionEvent e) {
            if (e.getPointerCount() < 2) return 0;
            float dx = e.getX(0) - e.getX(1), dy = e.getY(0) - e.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private void clamp() {
            float imageW = bitmap.getWidth() * scale;
            float imageH = bitmap.getHeight() * scale;
            offsetX = Math.min(0, Math.max(getWidth() - imageW, offsetX));
            offsetY = Math.min(0, Math.max(getHeight() - imageH, offsetY));
        }

        Bitmap createCrop(int outWidth, int outHeight) {
            Bitmap output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            float sx = outWidth / (float) getWidth();
            float sy = outHeight / (float) getHeight();
            canvas.drawBitmap(bitmap, null, new RectF(offsetX * sx, offsetY * sy,
                    (offsetX + bitmap.getWidth() * scale) * sx,
                    (offsetY + bitmap.getHeight() * scale) * sy), imagePaint);
            return output;
        }
    }
}
