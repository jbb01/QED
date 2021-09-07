package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.views.Histogram;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.viewmodel.ImageInfoViewModel;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.Comparator;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ImageInfoActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE = "image";
    private static final Object2IntMap<String> INFO_PRIORITY;
    private static final Object2IntMap<String> INFO_NAMES;

    static {
        Object2IntMap<String> priority = new Object2IntOpenHashMap<>();
        priority.put(Image.DATA_KEY_CREATION_DATE, 0);
        priority.put(Image.DATA_KEY_FORMAT, 1);
        priority.put(Image.DATA_KEY_RESOLUTION, 2);
        priority.put(Image.DATA_KEY_ORIENTATION, 3);
        priority.put(Image.DATA_KEY_MANUFACTURER, 4);
        priority.put(Image.DATA_KEY_MODEL, 5);
        priority.put(Image.DATA_KEY_FLASH, 6);
        priority.put(Image.DATA_KEY_FOCAL_LENGTH, 7);
        priority.put(Image.DATA_KEY_FOCAL_RATIO, 8);
        priority.put(Image.DATA_KEY_EXPOSURE_TIME, 9);
        priority.put(Image.DATA_KEY_ISO, 10);
        priority.put(Image.DATA_KEY_ALBUM, 11);
        priority.put(Image.DATA_KEY_OWNER, 12);
        priority.put(Image.DATA_KEY_UPLOAD_DATE, 13);
        priority.put(Image.DATA_KEY_VISITS, 14);
        INFO_PRIORITY = Object2IntMaps.unmodifiable(priority);

        Object2IntMap<String> names = new Object2IntOpenHashMap<>();
        names.put(Image.DATA_KEY_CREATION_DATE, R.string.image_info_creation_date);
        names.put(Image.DATA_KEY_FORMAT, R.string.image_info_format);
        names.put(Image.DATA_KEY_RESOLUTION, R.string.image_info_resolution);
        names.put(Image.DATA_KEY_ORIENTATION, R.string.image_info_orientation);
        names.put(Image.DATA_KEY_MANUFACTURER, R.string.image_info_camera_manufacturer);
        names.put(Image.DATA_KEY_MODEL, R.string.image_info_camera_model);
        names.put(Image.DATA_KEY_FLASH, R.string.image_info_flash);
        names.put(Image.DATA_KEY_FOCAL_LENGTH, R.string.image_info_focal_length);
        names.put(Image.DATA_KEY_FOCAL_RATIO, R.string.image_info_focal_ratio);
        names.put(Image.DATA_KEY_EXPOSURE_TIME, R.string.image_info_exposure_time);
        names.put(Image.DATA_KEY_ISO, R.string.image_info_iso);
        names.put(Image.DATA_KEY_ALBUM, R.string.image_info_album);
        names.put(Image.DATA_KEY_OWNER, R.string.image_info_owner);
        names.put(Image.DATA_KEY_UPLOAD_DATE, R.string.image_info_upload_date);
        names.put(Image.DATA_KEY_VISITS, R.string.image_info_number_of_calls);
        INFO_NAMES = Object2IntMaps.unmodifiable(names);
    }

    private LinearLayout mDetailContainer;
    private LinearLayout mQedContainer;
    private LinearLayout mOtherContainer;

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
        Image image = intent.getParcelableExtra(EXTRA_IMAGE);
        if (image == null) {
            // TODO show toast
            finish();
            return;
        }

        mDetailContainer = findViewById(R.id.image_info_detail_container);
        mQedContainer = findViewById(R.id.image_info_qed_container);
        mOtherContainer = findViewById(R.id.image_info_other_container);

        ImageInfoViewModel viewModel = new ViewModelProvider(this).get(ImageInfoViewModel.class);
        viewModel.load(image);

        viewModel.getImage().observe(this, status -> {
            Image img = status.getValue();
            if (img != null) {
                updateView(img);
            }
        });
    }

    private void updateView(Image image) {
        while (mDetailContainer.getChildCount() > 1) {
            mDetailContainer.removeViewAt(1);
        }

        while (mOtherContainer.getChildCount() > 1) {
            mOtherContainer.removeViewAt(1);
        }

        while (mQedContainer.getChildCount() > 1) {
            mQedContainer.removeViewAt(1);
        }

        if (image.getPath() != null) {
            File file = new File(image.getPath());

            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                // create histogram
                if (bitmap != null) {
                    image.getData().put(Image.DATA_KEY_RESOLUTION, bitmap.getWidth() + "x" + bitmap.getHeight());

                    LinearLayout histogramContainer = findViewById(R.id.image_info_histogram_container);
                    Histogram histogram = findViewById(R.id.image_detail_histogram);
                    histogram.setBitmap(bitmap);
                    histogramContainer.setVisibility(View.VISIBLE);
                }

                addLine(R.string.image_info_path, image.getPath(), mOtherContainer);
            }
        }

        image.getData().keySet().stream()
             .sorted(Comparator.comparing(key -> INFO_PRIORITY.getOrDefault(key, Integer.MAX_VALUE)))
             .forEach(key -> {
                 String value = image.getData().get(String.valueOf(key));
                 if (value == null) return;

                 LinearLayout container = null;

                 switch (key) {
                     case Image.DATA_KEY_FOCAL_LENGTH:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1])) + "mm";
                         } catch (Exception ignored) {}
                         container = mDetailContainer;
                         break;
                     case Image.DATA_KEY_FOCAL_RATIO:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = "f/" + ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]));
                         } catch (Exception ignored) {}
                         container = mDetailContainer;
                         break;
                     case Image.DATA_KEY_EXPOSURE_TIME:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = approximate((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]), 10000);
                         } catch (Exception ignored) {}
                         container = mDetailContainer;
                         break;
                     case Image.DATA_KEY_FLASH:
                         try {
                             int flash = Integer.parseInt(value);
                             value = ((flash & 0b1) == 0) ? getString(R.string.image_info_flash_no_flash) : getString(R.string.image_info_flash_triggered);
                         } catch (Exception ignored) {}
                         container = mDetailContainer;
                         break;
                     case Image.DATA_KEY_MANUFACTURER:
                     case Image.DATA_KEY_MODEL:
                     case Image.DATA_KEY_ISO:
                     case Image.DATA_KEY_RESOLUTION:
                     case Image.DATA_KEY_ORIENTATION:
                     case Image.DATA_KEY_CREATION_DATE:
                     case Image.DATA_KEY_FORMAT:
                     case Image.DATA_KEY_POSITION:
                         container = mDetailContainer;
                         break;
                     case Image.DATA_KEY_ALBUM:
                     case Image.DATA_KEY_OWNER:
                     case Image.DATA_KEY_UPLOAD_DATE:
                     case Image.DATA_KEY_VISITS:
                         container = mQedContainer;
                         break;
                 }

                 if (INFO_NAMES.containsKey(key)) {
                     addLine(INFO_NAMES.getInt(key), value, container);
                 } else {
                     addLine(key, value, container);
                 }
             });
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
