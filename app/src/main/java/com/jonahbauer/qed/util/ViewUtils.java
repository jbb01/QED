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
import androidx.annotation.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogEditTextBinding;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * Expands the given view using an animator.
     * The view will slide in from its top edge while increasing its alpha value from transparent to fully visible.
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
     * Collapses the given view using an animator. The view will slide out its top edge while decreasing its alpha value
     * from fully visible to transparent.
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

    /**
     * Prepares the given {@link EditText} for input of a {@link LocalDate} using a {@link DatePickerDialog}, i.e.
     * when the text field is clicked a date picker will be shown and the selected date will be reflected in the
     * text field and the provided live data.
     * Only when a {@link LifecycleOwner} is provided will changes to the live data also be reflected in the text field.
     * Since the provided live data stores a {@link LocalDateTime} it can be shared with a
     * {@linkplain #setupTimeEditText(LifecycleOwner, EditText, MutableLiveData) time text field}.
     *
     * @param owner a lifecycle owner used for observing the live data
     * @param editText a text field
     * @param date a live data that contains the currently selected date. {@link LiveData#getValue()} must not return {@code null}.
     * @see #setupTimeEditText(LifecycleOwner, EditText, MutableLiveData)
     */
    public static void setupDateEditText(@Nullable LifecycleOwner owner, @NonNull EditText editText, @NonNull MutableLiveData<LocalDateTime> date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        if (owner != null) {
            date.observe(owner, dateTime -> {
                editText.setText(formatter.format(dateTime));
            });
        }

        editText.setText(formatter.format(date.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(date.getValue());
            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        date.setValue(date.getValue().with(LocalDate.of(year, month + 1, dayOfMonth)));
                        editText.setText(formatter.format(date.getValue()));
                    },
                    value.getYear(),
                    value.getMonthValue() - 1,
                    value.getDayOfMonth()
            );
            dialog.show();
        });
    }

    /**
     * Prepares the given {@link EditText} for input of a {@link LocalTime} using a {@link TimePickerDialog}, i.e.
     * when the text field is clicked a ticker picker will be shown and the selected time will be reflected in the
     * text field and the provided live data.
     * Only when a {@link LifecycleOwner} is provided will changes to the live data also be reflected in the text field.
     * Since the provided live data stores a {@link LocalDateTime} it can be shared with a
     * {@linkplain #setupDateEditText(LifecycleOwner, EditText, MutableLiveData) date text field}.
     *
     * @param owner a lifecycle owner used for observing the live data
     * @param editText a text field
     * @param time a live data that contains the currently selected time. {@link LiveData#getValue()} must not return {@code null}.
     * @see #setupDateEditText(LifecycleOwner, EditText, MutableLiveData)
     */
    public static void setupTimeEditText(@Nullable LifecycleOwner owner, @NonNull EditText editText, @NonNull MutableLiveData<LocalDateTime> time) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        if (owner != null) {
            time.observe(owner, dateTime -> {
                editText.setText(formatter.format(dateTime));
            });
        }

        editText.setText(formatter.format(time.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(time.getValue());
            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        time.setValue(time.getValue().with(LocalTime.of(hourOfDay, minute)));
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

    /**
     * Sets the title of the {@linkplain AppCompatActivity#getSupportActionBar() action bar} when present.
     * @throws IllegalStateException if the fragment is not attached to an activity.
     */
    public static void setActionBarText(@NonNull Fragment fragment, CharSequence title) {
        AppCompatActivity activity = (AppCompatActivity) fragment.requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    /**
     * Makes the status and navigation bar transparent.
     * @throws IllegalStateException if the fragment is not attached to an activity
     * @throws NullPointerException if the activity is not attached to a window
     * @see #resetTransparentSystemBars(Fragment)
     */
    public static void setTransparentSystemBars(@NonNull Fragment fragment) {
        var activity = fragment.requireActivity();
        var window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    /**
     * Resets the status and navigation bar to their default colors using the fragment's context
     * to resolve the respective attributes
     * ({@link android.R.attr#statusBarColor} and {@link android.R.attr#navigationBarColor}).
     * @throws IllegalStateException if the fragment is not attached to an activity
     * @throws NullPointerException if the activity is not attached to a window
     */
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

    /**
     * Links the button's checked to the enabled state of the provided views. When the button gets checked
     * the first view will request focus.
     */
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

    /**
     * Links the button's checked state to the visibility of the expanding view.
     * Visibility changes are handled using {@link #expand(View)} and {@link #collapse(View)}.
     * The provided live data provides access to the current state and is initially queried for the initial state, but
     * further changes will not be reflected.
     */
    public static void setupExpandable(@NonNull CompoundButton button, @NonNull View expandingView, @NonNull MutableLiveData<Boolean> state) {
        var checkedSetter = (BooleanConsumer) checked -> {
            button.setChecked(checked);
            button.setButtonDrawable(checked ? R.drawable.ic_arrow_up_accent_animation : R.drawable.ic_arrow_down_accent_animation);

            var drawable = button.getButtonDrawable();
            if (drawable instanceof Animatable) ((Animatable) drawable).start();

            if (checked) expand(expandingView);
            else collapse(expandingView);
        };

        var expanded = state.getValue();
        if (expanded == null) expanded = false;
        checkedSetter.accept((boolean) expanded);

        button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkedSetter.accept(isChecked);
            state.setValue(isChecked);
        });
    }
}
