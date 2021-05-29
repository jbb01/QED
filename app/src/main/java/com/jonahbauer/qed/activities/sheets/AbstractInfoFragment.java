package com.jonahbauer.qed.activities.sheets;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.OnRebindCallback;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentInfoBinding;

public abstract class AbstractInfoFragment extends Fragment implements View.OnScrollChangeListener {

    private FragmentInfoBinding binding;
    private ViewDataBinding contentBinding;

    private float actionBarSize;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInfoBinding.inflate(inflater, container, false);
        contentBinding = onCreateContentView(inflater, binding.contentHolder, savedInstanceState);

        contentBinding.addOnRebindCallback(new OnRebindCallback<ViewDataBinding>() {
            @Override
            public void onBound(ViewDataBinding binding) {
                ((AbstractInfoBottomSheet) getParentFragment()).adjustHeight();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int color = getColor();
        int darkColor = Color.rgb(
                Color.red(color) * 204 / 255,
                Color.green(color) * 204 / 255,
                Color.blue(color) * 204 / 255
        );

        binding.background.setBackgroundResource(getBackground());
        binding.background.setBackgroundTintList(ColorStateList.valueOf(color));
        binding.solidBackground.setBackgroundColor(darkColor);
        if (binding.toolbar != null) {
            binding.toolbar.setBackgroundColor(darkColor);
            binding.toolbar.setTitleTextColor(Color.WHITE);
        }

        // wait for layout
        binding.background.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.background.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // make background 16:9
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    int height = binding.background.getHeight();
                    int width = height * 9 / 16;

                    ViewGroup.LayoutParams layoutParams = binding.background.getLayoutParams();
                    if (layoutParams != null) {
                        layoutParams.width = width;
                    } else {
                        layoutParams = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                    binding.background.setLayoutParams(layoutParams);
                } else {
                    int width = binding.background.getWidth();
                    int height = width * 9 / 16;

                    ViewGroup.LayoutParams layoutParams = binding.background.getLayoutParams();
                    if (layoutParams != null) {
                        layoutParams.height = height;
                    } else {
                        layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                    }

                    binding.background.setLayoutParams(layoutParams);
                }


                // forward touch event on header to content scroll view
                binding.background.setOnTouchListener(new View.OnTouchListener() {
                    private boolean intercepted;

                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);

                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            intercepted = getContentRoot().onInterceptTouchEvent(event);
                        } else if (!intercepted) {
                            intercepted = getContentRoot().onInterceptTouchEvent(event);
                        }

                        if (intercepted) {
                            return getContentRoot().onTouchEvent(event);
                        }

                        return true;
                    }
                });
            }
        });

        // setup fading
        TypedValue typedValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
        actionBarSize = typedValue.getDimension(getResources().getDisplayMetrics());

        if (binding.toolbar != null) {
            getContentRoot().setOnScrollChangeListener(this);
        }
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        final float totalFadeOut = 1.5f;

        ViewGroup contentHolder = binding.contentHolder;

        // fade out header
        float max = contentHolder.getTop() - totalFadeOut * actionBarSize;
        float alpha = 1 - scrollY / max;

        if (alpha < 0) alpha = 0;
        else if (alpha > 1) alpha = 1;

        binding.background.setAlpha(alpha);

        if (contentHolder.getTop() - scrollY + getTitleBottom() < actionBarSize) {
            binding.toolbar.setTitle(getTitle());
        } else if (alpha == 0) {
            binding.toolbar.setTitle("");
            binding.toolbar.setVisibility(View.VISIBLE);
            binding.toolbarLayout.setVisibility(View.VISIBLE);

            // merge toolbar and background
            float toolbarTransition = (totalFadeOut * actionBarSize - (contentHolder.getTop() - scrollY)) / ((totalFadeOut - 1) * actionBarSize);
            if (toolbarTransition < 0) toolbarTransition = 0;
            else if (toolbarTransition > 1) toolbarTransition = 1;

            // toolbarTransition == 0 => toolbar completely merged with background
            // toolbarTransition == 1 => only toolbar visible
            float height = (totalFadeOut - (totalFadeOut - 1) * toolbarTransition) * actionBarSize;
            float elevation = toolbarTransition * 30;

            ViewGroup.LayoutParams layoutParams = binding.toolbar.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) height);
            } else {
                layoutParams.height = (int) height;
            }
            binding.toolbar.setLayoutParams(layoutParams);
            binding.toolbarLayout.setElevation(toolbarTransition * 30 + 1);
        } else {
            binding.toolbar.setVisibility(View.GONE);
            binding.toolbarLayout.setVisibility(View.GONE);
        }

        binding.toolbar.setVisibility(alpha == 0 ? View.VISIBLE : View.GONE);
        binding.toolbar.setVisibility(View.VISIBLE);

        Log.d(Application.LOG_TAG_DEBUG, contentHolder.getTop() + ";" + scrollY + ";" + contentHolder.getBottom());
    }

    private ViewGroup getContentRoot() {
        return ((ViewGroup)contentBinding.getRoot());
    }

    protected abstract ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @ColorInt
    protected abstract int getColor();

    @DrawableRes
    protected abstract int getBackground();

    protected abstract String getTitle();

    protected abstract float getTitleBottom();
}
