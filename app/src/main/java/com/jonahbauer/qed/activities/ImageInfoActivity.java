package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.Histogram;
import com.jonahbauer.qed.qedgallery.image.Image;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.Comparator;

public class ImageInfoActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE = "image";
    private static final SparseIntArray INFO_PRIORITY = new SparseIntArray();

    static {
        INFO_PRIORITY.put(R.string.image_info_creation_date, 0);
        INFO_PRIORITY.put(R.string.image_info_format, 1);
        INFO_PRIORITY.put(R.string.image_info_resolution, 2);
        INFO_PRIORITY.put(R.string.image_info_orientation, 3);
        INFO_PRIORITY.put(R.string.image_info_camera_manufacturer, 4);
        INFO_PRIORITY.put(R.string.image_info_camera_model, 5);
        INFO_PRIORITY.put(R.string.image_info_flash, 6);
        INFO_PRIORITY.put(R.string.image_info_focal_length, 7);
        INFO_PRIORITY.put(R.string.image_info_focal_ratio, 8);
        INFO_PRIORITY.put(R.string.image_info_exposure_time, 9);
        INFO_PRIORITY.put(R.string.image_info_iso, 10);

        INFO_PRIORITY.put(R.string.image_info_path, 11);
        INFO_PRIORITY.put(R.string.image_info_thumbnail_path, 12);

        INFO_PRIORITY.put(R.string.image_info_album, 13);
        INFO_PRIORITY.put(R.string.image_info_owner, 14);
        INFO_PRIORITY.put(R.string.image_info_upload_date, 15);
        INFO_PRIORITY.put(R.string.image_info_number_of_calls, 16);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(R.string.title_activity_image_info);

        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);


        Intent intent = getIntent();

        Image image = (Image) intent.getSerializableExtra(EXTRA_IMAGE);
        if (image != null) {
            LinearLayout detailContainer = findViewById(R.id.image_detail_container);
            LinearLayout qedContainer = findViewById(R.id.image_detail_qed_container);
            LinearLayout otherContainer = findViewById(R.id.image_detail_other_container);

            if (image.path != null) {
                File file = new File(image.path);

                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(image.path);
                    // create histogram
                    if (bitmap != null) {
                        image.data.put(String.valueOf(R.string.image_info_resolution), bitmap.getWidth() + "x" + bitmap.getHeight());

                        LinearLayout histogramContainer = findViewById(R.id.image_detail_histogram_container);
                        Histogram histogram = findViewById(R.id.image_detail_histogram);
                        histogram.setImageBitmap(bitmap);
                        histogramContainer.setVisibility(View.VISIBLE);
                    }

                    addLine(R.string.image_info_path, image.path, otherContainer);
                }
            }

            image.data.keySet().stream()
                    .map(string -> {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException ignored) {
                            addLine(string, image.data.get(string), otherContainer);
                            return -1;
                        }
                    })
                    .filter(i -> i != -1)
                    .sorted(Comparator.comparing(id -> INFO_PRIORITY.get(id, Integer.MAX_VALUE)))
                    .forEach(stringId -> {
                        String value = image.data.get(String.valueOf(stringId));
                        if (value == null) return;

                        LinearLayout container = null;

                        switch (stringId) {
                            case R.string.image_info_focal_length:
                                try {
                                    String[] split = value.split("/");
                                    if (split.length == 2) value = ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1])) + "mm";
                                } catch (Exception ignored) {}
                                container = detailContainer;
                                break;
                            case R.string.image_info_focal_ratio:
                                try {
                                    String[] split = value.split("/");
                                    if (split.length == 2) value = "f/" + ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]));
                                } catch (Exception ignored) {}
                                container = detailContainer;
                                break;
                            case R.string.image_info_exposure_time:
                                try {
                                    String[] split = value.split("/");
                                    if (split.length == 2) value = approximate((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]), 10000);
                                } catch (Exception ignored) {}
                                container = detailContainer;
                                break;
                            case R.string.image_info_flash:
                                try {
                                    int flash = Integer.parseInt(value);
                                    value = ((flash & 0b1) == 0) ? getString(R.string.image_info_flash_no_flash) : getString(R.string.image_info_flash_triggered);
                                } catch (Exception ignored) {}
                                container = detailContainer;
                                break;
                            case R.string.image_info_camera_manufacturer:
                            case R.string.image_info_camera_model:
                            case R.string.image_info_iso:
                            case R.string.image_info_resolution:
                            case R.string.image_info_orientation:
                            case R.string.image_info_creation_date:
                            case R.string.image_info_format:
                            case R.string.image_info_position:
                                container = detailContainer;
                                break;
                            case R.string.image_info_album:
                            case R.string.image_info_owner:
                            case R.string.image_info_upload_date:
                            case R.string.image_info_number_of_calls:
                                container = qedContainer;
                                break;
                        }

                        addLine(stringId, value, container);
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void addLine(@StringRes int resId, String value, LinearLayout container) {
        addLine(getString(resId), value, container);
    }

    private void addLine(String title, String value, LinearLayout container) {
        if (container == null) return;
        TextView textView = new TextView(this);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        textView.setText(String.format("%s: %s", title, value));

        container.addView(textView);
        container.setVisibility(View.VISIBLE);
    }

    @NonNull
    @Contract(pure = true)
    @SuppressWarnings("SameParameterValue")
    private static String approximate(double x, int N) {
        int a = 0, b = 1, c = 1, d = 1;
        while (b <= N && d <= N) {
            double median = (double)(a + c) / (b + d);
            if (x == median) {
                if (b + d <= N) {
                    return (a + c) + "/" + (b + d);
                } else if (d > b) {
                    return c + "/" + d;
                } else {
                    return a + "/" + b;
                }
            } else if (x > median) {
                a += c;
                b += d;
            } else {
                c += a;
                d += b;
            }
        }

        if (b > N) {
            return c + "/" + d;
        } else {
            return a + "/" + b;
        }
    }
}
