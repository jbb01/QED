package com.jonahbauer.qed.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.databinding.AlertDialogEditTextBinding;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static androidx.core.view.WindowInsetsCompat.Type.ime;
import static androidx.core.view.WindowInsetsCompat.Type.systemBars;

@UtilityClass
@SuppressWarnings("unused")
public class ViewUtils {

    /**
     * Expands the given view.
     */
    public static void expand(@NonNull final View view) {
        // measure view
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(matchParentMeasureSpec, wrapContentMeasureSpec);

        final int duration = view.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        final int targetHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        view.getLayoutParams().height = 1;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0);

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    view.setAlpha(1);
                    setPaddingTop(view, initialPadding);
                } else {
                    view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                    view.setAlpha(interpolatedTime);
                    setPaddingTop(view, initialPadding - (int) (targetHeight * (1 - interpolatedTime)));
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    /**
     * Collapses the given view.
     */
    public static void collapse(@NonNull final View view) {
        final int duration = view.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        final int initialHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1);
                    setPaddingTop(view, initialPadding);
                } else {
                    view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    view.setAlpha(1 - interpolatedTime);
                    setPaddingTop(view, initialPadding - (int) (initialHeight * interpolatedTime));
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    //<editor-fold desc="Padding" defaultstate="collapsed">
    public static void setPaddingTop(@NonNull View view, int paddingTop) {
        view.setPadding(
                view.getPaddingLeft(),
                paddingTop,
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingLeft(@NonNull View view, int paddingLeft) {
        view.setPadding(
                paddingLeft,
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingRight(@NonNull View view, int paddingRight) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                paddingRight,
                view.getPaddingBottom()
        );
    }

    public static void setPaddingBottom(@NonNull View view, int paddingBottom) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                paddingBottom
        );
    }
    //</editor-fold>

    public static void setupDateEditText(@NonNull EditText editText, @NonNull MutableLiveData<LocalDate> date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        editText.setText(formatter.format(date.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(date.getValue());
            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        date.setValue(LocalDate.of(year, month + 1, dayOfMonth));
                        editText.setText(formatter.format(date.getValue()));
                    },
                    value.getYear(),
                    value.getMonthValue() - 1,
                    value.getDayOfMonth()
            );
            dialog.show();
        });
    }

    public static void setupTimeEditText(@NonNull EditText editText, @NonNull MutableLiveData<LocalTime> time) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        editText.setText(formatter.format(time.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(time.getValue());
            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        time.setValue(LocalTime.of(hourOfDay, minute));
                        editText.setText(formatter.format(time.getValue()));
                    },
                    value.getHour(),
                    value.getMinute(),
                    android.text.format.DateFormat.is24HourFormat(context)
            );

            dialog.show();
        });
    }

    public static void showPreferenceDialog(@NonNull Context context, @StringRes int title, @NonNull Supplier<String> getter, Consumer<String> setter) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);

        var binding = AlertDialogEditTextBinding.inflate(LayoutInflater.from(context));
        binding.input.setText(getter.get());
        binding.input.requestFocus();
        binding.input.setSelection(binding.input.getText().length());

        var imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(R.string.ok, (d, which) -> {
            setter.accept(binding.input.getText().toString());
            d.dismiss();
        });
        dialog.setNegativeButton(R.string.cancel, (d, which) -> d.cancel());
        dialog.show();
    }

    public static void setError(@NonNull EditText editText, boolean error) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, error ? R.drawable.ic_error : 0, 0);
    }

    //<editor-fold desc="Conversions" defaultstate="collapsed">
    public static float spToPx(@NonNull DisplayMetrics displayMetrics, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics);
    }

    public static float spToPx(@NonNull Resources resources, float sp) {
        return spToPx(resources.getDisplayMetrics(), sp);
    }

    public static float spToPx(@NonNull Context context, float sp) {
        return spToPx(context.getResources(), sp);
    }

    public static float spToPx(@NonNull View view, float sp) {
        return spToPx(view.getResources(), sp);
    }

    public static float dpToPx(@NonNull DisplayMetrics displayMetrics, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    public static float dpToPx(@NonNull Resources resources, float dp) {
        return dpToPx(resources.getDisplayMetrics(), dp);
    }

    public static float dpToPx(@NonNull Context context, float dp) {
        return dpToPx(context.getResources(), dp);
    }

    public static float dpToPx(@NonNull View view, float dp) {
        return dpToPx(view.getResources(), dp);
    }

    public static float dpToPx(@NonNull Fragment fragment, float dp) {
        return dpToPx(fragment.getResources(), dp);
    }
    //</editor-fold>

    public static void setActionBarText(@NonNull Fragment fragment, CharSequence title) {
        AppCompatActivity activity = (AppCompatActivity) fragment.requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public static void setTransparentSystemBars(@NonNull Fragment fragment) {
        var activity = fragment.requireActivity();
        var window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public static void resetTransparentSystemBars(@NonNull Fragment fragment) {
        var theme = fragment.requireContext().getTheme();
        TypedValue statusBarColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.statusBarColor, statusBarColor, true);
        TypedValue navigationBarColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.navigationBarColor, navigationBarColor, true);

        var activity = fragment.requireActivity();
        var window = activity.getWindow();
        window.setStatusBarColor(statusBarColor.data);
        window.setNavigationBarColor(navigationBarColor.data);
    }

    public static void setFitsSystemWindows(View view) {
        var lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.topMargin = (int) dpToPx(view, 80);
        view.setLayoutParams(lp);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            var mask = systemBars() | ime();
            var insets = windowInsets.getInsets(mask);
            setPaddingBottom(v, insets.bottom);
            return new WindowInsetsCompat.Builder(windowInsets)
                    .setInsets(mask, Insets.of(insets.left, insets.top, insets.right, 0))
                    .build();
        });
    }

    @NonNull
    @Contract("_ -> new")
    public static ViewModelProvider getViewModelProvider(Fragment fragment) {
        NavBackStackEntry entry = NavHostFragment.findNavController(fragment).getCurrentBackStackEntry();
        assert entry != null;
        return new ViewModelProvider(entry.getViewModelStore(), entry.getDefaultViewModelProviderFactory());
    }
}
