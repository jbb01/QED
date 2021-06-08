package com.jonahbauer.qed.activities.sheets;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Bundle;
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
import androidx.core.view.ViewCompat;
import androidx.databinding.OnRebindCallback;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentInfoBinding;
import com.jonahbauer.qed.util.Colors;

public abstract class AbstractInfoFragment extends Fragment implements View.OnScrollChangeListener {

    private FragmentInfoBinding mBinding;
    private ViewDataBinding mContentBinding;

    private float mActionBarSize;
    private float mActionBarElevation;

    @ColorInt
    private int mColor;
    @ColorInt
    private int mDarkColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoBinding.inflate(inflater, container, false);
        mContentBinding = onCreateContentView(inflater, mBinding.contentHolder, savedInstanceState);

        mContentBinding.addOnRebindCallback(new OnRebindCallback<ViewDataBinding>() {
            @Override
            public void onBound(ViewDataBinding binding) {
                Fragment parent = getParentFragment();
                if (parent instanceof AbstractInfoBottomSheet) {
                    ((AbstractInfoBottomSheet) parent).adjustHeight();
                }
            }
        });

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mColor = getColor();
        mDarkColor = Colors.multiply(mColor, 0xFFCCCCCC);

        mBinding.backgroundPattern.setImageResource(getBackground());
        mBinding.backgroundPattern.setColorFilter(mColor, PorterDuff.Mode.MULTIPLY);
        mBinding.backgroundSolid.setBackgroundColor(mDarkColor);

        // a touch listener that delegates touch events to the main NestedScrollView
        class TouchDelegator implements View.OnTouchListener {
            private boolean mIntercepted;
            private boolean mClickAllowed = true;

            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIntercepted = getContentRoot().onInterceptTouchEvent(event);
                } else if (!mIntercepted) {
                    mIntercepted = getContentRoot().onInterceptTouchEvent(event);
                }

                if (mIntercepted) {
                    return getContentRoot().onTouchEvent(event);
                } else {
                    transformEvent(v, event);

                    if (!mClickAllowed && event.getAction() == MotionEvent.ACTION_UP)
                        event.setAction(MotionEvent.ACTION_CANCEL);

                    // delegate event
                    return mBinding.common.dispatchTouchEvent(event);
                }
            }

            private void transformEvent(View view, MotionEvent motionEvent) {
                // transform event to target view coordinates
                int[] sourceLocation = new int[2];
                int[] targetLocation = new int[2];
                view.getLocationOnScreen(sourceLocation);
                mBinding.common.getLocationOnScreen(targetLocation);

                Matrix matrix = new Matrix();
                matrix.postTranslate(
                        sourceLocation[0] - targetLocation[0],
                        sourceLocation[1] - targetLocation[1]
                );
                motionEvent.transform(matrix);
            }
        }

        // wait for layout
        mBinding.background.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mBinding.background.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // make background 16:9
                int orientation = getResources().getConfiguration().orientation;
                int height, width;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    height = ViewGroup.LayoutParams.MATCH_PARENT;
                    width = mBinding.background.getHeight() * 9 / 16;
                } else {
                    width = ViewGroup.LayoutParams.MATCH_PARENT;
                    height = mBinding.background.getWidth() * 9 / 16;
                }

                ViewGroup.LayoutParams layoutParams = mBinding.background.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.width = width;
                    layoutParams.height = height;
                } else {
                    layoutParams = new ViewGroup.LayoutParams(width, height);
                }
                mBinding.background.setLayoutParams(layoutParams);


                // forward touch event on header to content scroll view
                // noinspection ClickableViewAccessibility
                mBinding.getRoot().setOnTouchListener(new TouchDelegator());
            }
        });

        // setup toolbar + toolbar fading
        if (mBinding.toolbar != null) {
            mBinding.toolbar.setBackgroundColor(Color.TRANSPARENT);

            TouchDelegator delegator = new TouchDelegator();
            delegator.mClickAllowed = false;
            // noinspection ClickableViewAccessibility
            mBinding.toolbar.setOnTouchListener(delegator);

            TypedValue typedValue = new TypedValue();
            view.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
            mActionBarSize = typedValue.getDimension(getResources().getDisplayMetrics());

            mActionBarElevation = mBinding.toolbar.getElevation();

            mBinding.common.setOnScrollChangeListener(this);
        }
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        assert mBinding.toolbar != null;

        final float totalFadeOut = 1.5f;

        ViewGroup contentHolder = mBinding.common;

        // fade out header
        float max = contentHolder.getTop() - totalFadeOut * mActionBarSize;
        float alpha = 1 - scrollY / max;

        if (alpha < 0) alpha = 0;
        else if (alpha > 1) alpha = 1;

        mBinding.backgroundPattern.setAlpha(alpha);
        mBinding.background.setTranslationY(- scrollY / 2f);

        boolean shouldShowTitle = contentHolder.getTop() - scrollY + getTitleBottom() < mActionBarSize;
        boolean shouldShowActionBar = alpha == 0;
        if (shouldShowTitle || shouldShowActionBar) {
            mBinding.toolbar.setBackgroundColor(mDarkColor);

            float height, elevation;
            if (shouldShowTitle) {
                mBinding.toolbar.setTitle(getTitle());
                elevation = mActionBarElevation;
                height = mActionBarSize;
            } else {
                mBinding.toolbar.setTitle("");
                float toolbarTransition = (totalFadeOut * mActionBarSize - (contentHolder.getTop() - scrollY)) / ((totalFadeOut - 1) * mActionBarSize);
                if (toolbarTransition < 0) toolbarTransition = 0;
                else if (toolbarTransition > 1) toolbarTransition = 1;

                // toolbarTransition == 0 => toolbar completely merged with background
                // toolbarTransition == 1 => only toolbar visible
                elevation = toolbarTransition * mActionBarElevation;
                height = (totalFadeOut - (totalFadeOut - 1) * toolbarTransition) * mActionBarSize;
            }

            ViewGroup.LayoutParams layoutParams = mBinding.toolbar.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) height);
            } else {
                layoutParams.height = (int) height;
            }
            mBinding.toolbar.setLayoutParams(layoutParams);
            ViewCompat.setElevation(mBinding.toolbar, elevation);
        } else {
            mBinding.toolbar.setBackgroundColor(Color.TRANSPARENT);
            mBinding.toolbar.setTitle("");
        }
    }

    private ViewGroup getContentRoot() {
        return ((ViewGroup) mContentBinding.getRoot());
    }

    protected abstract ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @ColorInt
    protected abstract int getColor();

    @DrawableRes
    protected abstract int getBackground();

    protected abstract String getTitle();

    protected abstract float getTitleBottom();
}
