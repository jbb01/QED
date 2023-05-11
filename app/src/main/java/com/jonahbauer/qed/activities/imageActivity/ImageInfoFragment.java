package com.jonahbauer.qed.activities.imageActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentImageInfoBinding;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.viewmodel.ImageInfoViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

import androidx.transition.Fade;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.Comparator;

public class ImageInfoFragment extends Fragment {
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

    private Image mImage;

    private FragmentImageInfoBinding mBinding;
    private ImageInfoViewModel mImageInfoViewModel;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageInfoFragmentArgs args = ImageInfoFragmentArgs.fromBundle(getArguments());
        mImage = args.getImage();

        var transition = new Fade();
        transition.setDuration(TransitionUtils.getTransitionDuration(this));
        setEnterTransition(transition);
        setReturnTransition(transition);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentImageInfoBinding.inflate(inflater, container, false);
        mImageInfoViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_image_info).get(ImageInfoViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mImageInfoViewModel.getImage().observe(getViewLifecycleOwner(), status -> {
            if (status.getCode() == StatusWrapper.STATUS_ERROR) {
                Toast.makeText(requireContext(), status.getReason().getStringRes(), Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }

            var image = status.getValue();
            updateView(image);
            mBinding.toolbar.setTitle(image != null ? image.getName() : "");
        });
        mBinding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    @Override
    public void onStart() {
        mImageInfoViewModel.load(mImage);
        super.onStart();
    }

    @Override
    public void onStop() {
        mDisposable.clear();
        super.onStop();
    }

    private void updateView(@Nullable Image image) {
        mBinding.imageInfo.removeAllViews();
        mBinding.qedInfos.removeAllViews();

        if (image == null) return;

        // histogram
        if (image.getPath() != null) {
            asyncLoadHistogramm(image);
        }

        // image info
        image.getData().entrySet().stream()
             .sorted(Comparator.comparingInt(e -> INFO_PRIORITY.getOrDefault(e.getKey(), Integer.MAX_VALUE)))
             .forEach(entry -> {
                 String key = entry.getKey();
                 String value = entry.getValue();

                 LinearLayout container = null;

                 switch (key) {
                     case Image.DATA_KEY_FOCAL_LENGTH:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1])) + "mm";
                         } catch (Exception ignored) {}
                         container = mBinding.imageInfo;
                         break;
                     case Image.DATA_KEY_FOCAL_RATIO:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = "f/" + ((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]));
                         } catch (Exception ignored) {}
                         container = mBinding.imageInfo;
                         break;
                     case Image.DATA_KEY_EXPOSURE_TIME:
                         try {
                             String[] split = value.split("/");
                             if (split.length == 2) value = approximate((double)Integer.parseInt(split[0]) / Integer.parseInt(split[1]), 10000);
                         } catch (Exception ignored) {}
                         container = mBinding.imageInfo;
                         break;
                     case Image.DATA_KEY_FLASH:
                         try {
                             int flash = Integer.parseInt(value);
                             value = ((flash & 0b1) == 0) ? getString(R.string.image_info_flash_no_flash) : getString(R.string.image_info_flash_triggered);
                         } catch (Exception ignored) {}
                         container = mBinding.imageInfo;
                         break;
                     case Image.DATA_KEY_MANUFACTURER:
                     case Image.DATA_KEY_MODEL:
                     case Image.DATA_KEY_ISO:
                     case Image.DATA_KEY_RESOLUTION:
                     case Image.DATA_KEY_ORIENTATION:
                     case Image.DATA_KEY_CREATION_DATE:
                     case Image.DATA_KEY_FORMAT:
                     case Image.DATA_KEY_POSITION:
                         container = mBinding.imageInfo;
                         break;
                     case Image.DATA_KEY_ALBUM:
                     case Image.DATA_KEY_OWNER:
                     case Image.DATA_KEY_UPLOAD_DATE:
                     case Image.DATA_KEY_VISITS:
                         container = mBinding.qedInfos;
                         break;
                 }

                 if (container != null) {
                     if (INFO_NAMES.containsKey(key)) {
                         addLine(INFO_NAMES.getInt(key), value, container);
                     } else {
                         addLine(key, value, container);
                     }
                 }
             });
    }

    private void asyncLoadHistogramm(Image image) {
        mBinding.histogramLayout.setVisibility(View.VISIBLE);

        var bitmapSingle = Single.fromCallable(() -> {
            File file = new File(image.getPath());
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                if (bitmap != null) {
                    return bitmap;
                }
            }
            throw new NullPointerException();
        });

        mDisposable.add(
                bitmapSingle.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    bitmap -> {
                                        image.getData().put(Image.DATA_KEY_RESOLUTION, bitmap.getWidth() + "x" + bitmap.getHeight());

                                        mBinding.histogram.setBitmap(bitmap);
                                        mBinding.histogramLayout.setVisibility(View.VISIBLE);
                                    },
                                    throwable -> mBinding.histogramLayout.setVisibility(View.GONE)
                            )
        );
    }

    private void addLine(@StringRes int resId, String value, LinearLayout container) {
        addLine(getString(resId), value, container);
    }

    private void addLine(String title, String value, LinearLayout container) {
        if (container == null) return;
        TextView textView = new TextView(requireContext());
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        textView.setText(String.format("%s: %s", title, value));

        container.addView(textView);
        ((View)container.getParent()).setVisibility(View.VISIBLE);
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
