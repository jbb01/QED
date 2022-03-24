package com.jonahbauer.qed.activities.sheets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.*;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentInfoBinding;
import com.jonahbauer.qed.layoutStuff.ColorfulBottomSheetCallback;
import com.jonahbauer.qed.util.Colors;

public abstract class InfoBottomSheet extends BottomSheetDialogFragment implements View.OnScrollChangeListener {
    private BottomSheetBehavior.BottomSheetCallback mSheetCallback;
    private Window mDialogWindow;

    private FragmentInfoBinding mBinding;
    private InfoFragment mFragment;

    private float mActionBarSize;
    private float mActionBarElevation;

    @ColorInt
    private int mColor;
    @ColorInt
    private int mDarkColor;

    private final Rect mTempRect = new Rect();

    public InfoBottomSheet() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog);
        mDialogWindow = bottomSheetDialog.getWindow();
        return bottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoBinding.inflate(inflater, container, false);
        mFragment = createFragment();

        View view = mBinding.getRoot();
        view.setOnClickListener(v -> dismiss());

        return view;
    }

    private void adjustHeight(View view) {
        int contentHeight = 0;
        Activity activity = getActivity();
        if (activity != null) {
            Window window = activity.getWindow();
            View decorView = window.getDecorView();

            Rect rectangle = new Rect();
            decorView.getWindowVisibleDisplayFrame(rectangle);
            contentHeight = rectangle.height();

            if (!decorView.isAttachedToWindow()) {
                var insets = view.getRootWindowInsets();
                contentHeight -= insets.getSystemWindowInsetTop();
            }
        }

        // Ensure that bottom sheet can be expanded to cover the full screen
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = contentHeight;
        } else {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contentHeight);
        }
        view.setLayoutParams(layoutParams);
    }

    /**
     * Adjust the height of the coordinator layout to prevent snackbars from appearing off screen.
     */
    private void adjustCoordinatorHeight(View bottomSheet) {
        if (mBinding.coordinator != null) {
            bottomSheet.getLocalVisibleRect(mTempRect);

            var params = mBinding.coordinator.getLayoutParams();
            params.height = mTempRect.height();
            mBinding.coordinator.setLayoutParams(params);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // instantiate fragment
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, mFragment, mFragment.getTag()).commit();

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

                // set initial coordinator height
                var bottomSheet = (View) view.getParent();
                adjustCoordinatorHeight(bottomSheet);
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

        // add bottom sheet callback
        view.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
            BottomSheetBehavior<?> mBehavior;

            @Override
            public void onWindowAttached() {
                var touchOutside = view.getRootView().findViewById(R.id.touch_outside);
                var bottomSheet = (View) view.getParent();

                if (mSheetCallback == null) {
                    mSheetCallback = new ColorfulBottomSheetCallback(mDialogWindow, touchOutside, getColor());
                }

                mBehavior = BottomSheetBehavior.from(bottomSheet);
                mBehavior.addBottomSheetCallback(mSheetCallback);

                // update coordinator height when sliding
                mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {}

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        adjustCoordinatorHeight(bottomSheet);
                    }
                });

                adjustHeight(mBinding.getRoot());
            }

            @Override
            public void onWindowDetached() {
                view.getViewTreeObserver().removeOnWindowAttachListener(this);

                if (mBehavior != null)
                    mBehavior.removeBottomSheetCallback(mSheetCallback);
            }
        });
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

        boolean shouldShowTitle = contentHolder.getTop() - scrollY + mFragment.getTitleBottom() < mActionBarSize;
        boolean shouldShowActionBar = alpha == 0;
        if (shouldShowTitle || shouldShowActionBar) {
            mBinding.toolbar.setBackgroundColor(mDarkColor);

            float height, elevation;
            if (shouldShowTitle) {
                mBinding.toolbar.setTitle(mFragment.getTitle());
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
        return ((ViewGroup) mFragment.requireView());
    }

    @ColorInt
    public abstract int getColor();

    @DrawableRes
    public abstract int getBackground();

    @NonNull
    public abstract InfoFragment createFragment();

    protected ViewModelProvider getViewModelProvider() {
        return new ViewModelProvider(this);
    }
}
