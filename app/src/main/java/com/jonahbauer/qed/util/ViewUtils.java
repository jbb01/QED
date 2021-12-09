package com.jonahbauer.qed.util;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogEditTextBinding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;

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

    public static void showPreferenceDialog(Context context, @StringRes int title, @NonNull Supplier<String> getter, Consumer<String> setter) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);

        var binding = AlertDialogEditTextBinding.inflate(LayoutInflater.from(dialog.getContext()));
        binding.input.setText(getter.get());

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
    public static float spToPx(@NonNull Resources resources, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
    }

    public static float spToPx(@NonNull Context context, float sp) {
        return spToPx(context.getResources(), sp);
    }

    public static float spToPx(@NonNull View view, float sp) {
        return spToPx(view.getResources(), sp);
    }

    public static float dpToPx(@NonNull Resources resources, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
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
}
