package com.jonahbauer.qed.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;

import com.jonahbauer.qed.R;

import java.text.DateFormat;
import java.util.Calendar;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("unused")
public class ViewUtils {

    /**
     * Expands the given view.
     */
    public static void expand(final View view) {
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
    public static void collapse(final View view) {
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

    public static void setPaddingTop(View view, int paddingTop) {
        view.setPadding(
                view.getPaddingLeft(),
                paddingTop,
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingLeft(View view, int paddingLeft) {
        view.setPadding(
                paddingLeft,
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingRight(View view, int paddingRight) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                paddingRight,
                view.getPaddingBottom()
        );
    }

    public static void setPaddingBottom(View view, int paddingBottom) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                paddingBottom
        );
    }

    public static void setupDateEditText(EditText editText, Calendar date) {
        DateFormat format = android.text.format.DateFormat.getDateFormat(editText.getContext());

        editText.setText(format.format(date.getTime()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        date.set(year, month, dayOfMonth);
                        editText.setText(format.format(date.getTime()));
                    },
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    public static void setupTimeEditText(EditText editText, Calendar date) {
        DateFormat format = android.text.format.DateFormat.getTimeFormat(editText.getContext());

        editText.setText(format.format(date.getTime()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);
                        editText.setText(format.format(date.getTime()));
                    },
                    date.get(Calendar.HOUR_OF_DAY),
                    date.get(Calendar.MINUTE),
                    android.text.format.DateFormat.is24HourFormat(context)
            );

            dialog.show();
        });
    }

    public static void setError(EditText editText, boolean error) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, error ? R.drawable.ic_error : 0, 0);
    }
}
