package com.jonahbauer.qed.util;

import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
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

        final int duration = view.getContext().getResources().getInteger(R.integer.material_motion_duration_medium_2);
        final int targetHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        view.getLayoutParams().height = 1;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0);

        var animator = ObjectAnimator.ofFloat(0, 1);
        animator.addUpdateListener(animation -> {
            var fraction = animation.getAnimatedFraction();
            if (fraction == 1) {
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setAlpha(1);
                setPaddingTop(view, initialPadding);
            } else {
                view.getLayoutParams().height = (int) (targetHeight * fraction);
                view.setAlpha(fraction);
                setPaddingTop(view, initialPadding - (int) (targetHeight * (1 - fraction)));
            }
            view.requestLayout();
        });
        animator.setDuration(duration);
        animator.start();
    }

    /**
     * Collapses the given view.
     */
    public static void collapse(@NonNull final View view) {
        final int duration = view.getContext().getResources().getInteger(R.integer.material_motion_duration_medium_2);
        final int initialHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        var animator = ObjectAnimator.ofFloat(0, 1);
        animator.addUpdateListener(animation -> {
            var fraction = animation.getAnimatedFraction();
            if (fraction == 1) {
                view.setVisibility(View.GONE);
                view.setAlpha(1);
                setPaddingTop(view, initialPadding);
            } else {
                view.getLayoutParams().height = initialHeight - (int) (initialHeight * fraction);
                view.setAlpha(1 - fraction);
                setPaddingTop(view, initialPadding - (int) (initialHeight * fraction));
            }
            view.requestLayout();
        });
        animator.setDuration(duration);
        animator.start();
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

    public static @Px float getActionBarSize(@NonNull Context context) {
        var out = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, out, true)) {
            return out.getDimension(context.getResources().getDisplayMetrics());
        } else {
            return dpToPx(context, 56);
        }
    }

    @NonNull
    @Contract("_,_ -> new")
    public static ViewModelProvider getViewModelProvider(Fragment fragment, @IdRes int destinationId) {
        NavBackStackEntry entry = NavHostFragment.findNavController(fragment).getBackStackEntry(destinationId);
        return new ViewModelProvider(entry.getViewModelStore(), entry.getDefaultViewModelProviderFactory());
    }

    public static void link(@NonNull CompoundButton button, View...views) {
        if (views.length == 0) return;
        var listener = (CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> {
            for (View view : views) {
                view.setEnabled(isChecked);
            }
            if (isChecked) views[0].requestFocus();
        };
        button.setOnCheckedChangeListener(listener);
        listener.onCheckedChanged(button, button.isChecked());
    }

    public static void setupExpandable(@NonNull CompoundButton button, @NonNull View expandingView) {
        button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.expand(expandingView);
            } else {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.collapse(expandingView);
            }
        });
    }
}
