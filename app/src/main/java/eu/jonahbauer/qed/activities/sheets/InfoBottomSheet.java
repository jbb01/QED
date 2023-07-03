package eu.jonahbauer.qed.activities.sheets;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.FragmentInfoBinding;
import eu.jonahbauer.qed.layoutStuff.ColorfulBottomSheetCallback;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.Colors;
import eu.jonahbauer.qed.util.Themes;
import eu.jonahbauer.qed.util.ViewUtils;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

public abstract class InfoBottomSheet extends BottomSheetDialogFragment {
    private static final String SAVED_BACKGROUND_PATTERN_IMAGE_ALPHA = "backgroundPatternImageAlpha";
    private static final String SAVED_TOOLBAR_TITLE_VISIBLE = "toolbarTitleVisible";
    private static final String SAVED_TOOLBAR_BACKGROUND_COLOR = "toolbarBackgroundColor";

    private final MutableLiveData<Boolean> mToolbarTitleVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> mToolbarBackgroundColor = new MutableLiveData<>(Color.TRANSPARENT);

    private Window mDialogWindow;
    private MenuInflater mMenuInflater;
    private BottomSheetBehavior<?> mBottomSheetBehavior;

    private FragmentInfoBinding mBinding;
    private InfoFragment mFragment;

    private ColorfulBottomSheetCallback mBottomSheetCallback;

    public InfoBottomSheet() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog);
        mDialogWindow = bottomSheetDialog.getWindow();
        mBottomSheetBehavior = bottomSheetDialog.getBehavior();
        return bottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mMenuInflater = requireActivity().getMenuInflater();
        mBinding = FragmentInfoBinding.inflate(inflater, container, false);
        mFragment = createFragment();
        return mBinding.getRoot();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setOnClickListener(v -> dismiss());

        var coordinator = (ViewGroup) view.getRootView().findViewById(R.id.coordinator);
        var touchOutside = view.getRootView().findViewById(R.id.touch_outside);

        // register callback
        mBottomSheetCallback = new ColorfulBottomSheetCallback(mDialogWindow, touchOutside, getColor());
        mBottomSheetBehavior.addBottomSheetCallback(mBottomSheetCallback);

        // instantiate fragment
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, mFragment, mFragment.getTag()).commit();

        // wait for layout
        OneShotPreDrawListener.add(view, () -> {
            initBackground();

            // ensure that the bottom sheet can be expanded to cover the full screen
            ViewUtils.setHeight(view, coordinator.getHeight());

            // forward touch event on header to content scroll view
            mBinding.getRoot().setOnTouchListener(new TouchDelegator());

            // setup toolbar + toolbar fading
            if (mBinding.toolbar != null) {
                mBinding.toolbar.setOnTouchListener(new TouchDelegator(false));
                mBinding.common.setOnScrollChangeListener(new CollapsingToolbarScrollListener());
            }
        });

        observeViewModel();

        // setup open in browser button
        if (mFragment.isOpenInBrowserSupported()) {
            mBinding.openInBrowser.setVisibility(View.VISIBLE);
            mBinding.openInBrowser.setOnClickListener(v -> openInBrowser());
            TooltipCompat.setTooltipText(mBinding.openInBrowser, getString(R.string.open_in_browser));
            if (mBinding.toolbar != null) {
                mBinding.toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.open_in_browser) {
                        openInBrowser();
                        return true;
                    }
                    return false;
                });
            }
        } else {
            mBinding.openInBrowser.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_BACKGROUND_PATTERN_IMAGE_ALPHA, mBinding.backgroundPattern.getImageAlpha());
        outState.putBoolean(SAVED_TOOLBAR_TITLE_VISIBLE, Objects.requireNonNull(mToolbarTitleVisible.getValue()));
        outState.putInt(SAVED_TOOLBAR_BACKGROUND_COLOR, Objects.requireNonNull(mToolbarBackgroundColor.getValue()));
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mBottomSheetCallback.onStateChanged(requireView(), mBottomSheetBehavior.getState());

        if (savedInstanceState != null) {
            mBinding.backgroundPattern.setImageAlpha(savedInstanceState.getInt(SAVED_BACKGROUND_PATTERN_IMAGE_ALPHA));
            mToolbarTitleVisible.setValue(savedInstanceState.getBoolean(SAVED_TOOLBAR_TITLE_VISIBLE));
            mToolbarBackgroundColor.setValue(savedInstanceState.getInt(SAVED_TOOLBAR_BACKGROUND_COLOR));
        }
    }

    /**
     * make the background 16:9 and set the background pattern and color according to the visible entity
     */
    private void initBackground() {
        // set background colors
        var color = getColor();
        var darkColor = getDarkColor();
        mBinding.backgroundPattern.setImageResource(getBackground());
        mBinding.backgroundPattern.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        mBinding.backgroundSolid.setBackgroundColor(darkColor);
        mBinding.setColor(color);

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
        ViewUtils.setSize(mBinding.background, width, height);
    }

    private void observeViewModel() {
        var model = getInfoViewModel();
        model.getError().observe(getViewLifecycleOwner(), error -> {
            mBinding.setError(error ? getString(R.string.error_incomplete) : null);
        });
        model.getLoading().observe(getViewLifecycleOwner(), loading -> {
            mBinding.setLoading(loading);
            if (!loading) {
                startPostponedEnterTransition();
            }
        });
        model.getTitle().observe(getViewLifecycleOwner(), title -> {
            mBinding.setTitle(title);
        });

        // observer toolbar
        mToolbarBackgroundColor.observe(getViewLifecycleOwner(), color -> mBinding.toolbar.setBackgroundColor(color));
        mToolbarTitleVisible.observe(getViewLifecycleOwner(), visible -> updateToolbar(
                model.getToolbarTitle().getValue(),
                visible
        ));
        model.getToolbarTitle().observe(getViewLifecycleOwner(), title -> updateToolbar(
                title,
                Boolean.TRUE.equals(mToolbarTitleVisible.getValue())
        ));
    }

    private CharSequence mPreviousTitle;
    private Boolean mPreviousTitleVisible;
    private void updateToolbar(CharSequence title, boolean titleVisible) {
        if (mBinding.toolbar == null) return;

        // debounce updates
        if (Boolean.valueOf(titleVisible).equals(mPreviousTitleVisible) && title.equals(mPreviousTitle)) {
            return;
        }
        mPreviousTitle = title;
        mPreviousTitleVisible = titleVisible;

        // update title
        if (titleVisible) {
            mBinding.toolbar.setTitle(title);
        } else {
            mBinding.toolbar.setTitle("");
        }

        // update menu
        if (mFragment.isOpenInBrowserSupported()) {
            mBinding.toolbar.getMenu().clear();
            if (titleVisible) {
                mMenuInflater.inflate(R.menu.menu_open_in_browser, mBinding.toolbar.getMenu());
            }
        }
    }

    private void openInBrowser() {
        if (!mFragment.isOpenInBrowserSupported()) return;
        Actions.openInBrowser(requireContext(), mFragment.getOpenInBrowserLink());
    }

    private ViewGroup getContentRoot() {
        return ((ViewGroup) mFragment.requireView());
    }

    public abstract long getDesignSeed();

    public @ColorInt int getColor() {
        return Themes.colorful(requireContext(), getDesignSeed());
    }

    public @ColorInt int getDarkColor() {
        return Colors.multiply(getColor(), 0xFFCCCCCC);
    }

    public @DrawableRes int getBackground() {
        return Themes.pattern(getDesignSeed());
    }

    public abstract @NonNull InfoFragment createFragment();

    public abstract @NonNull InfoViewModel<?> getInfoViewModel();

    protected ViewModelProvider getViewModelProvider() {
        return new ViewModelProvider(this);
    }

    /**
     * a scroll listener that collapses the background into the toolbar
     */
    private class CollapsingToolbarScrollListener implements View.OnScrollChangeListener {
        private final Toolbar mToolbar;
        private final View mBackground;
        private final ImageView mBackgroundPattern;
        private final View mContent;

        @ColorInt
        private final int mDarkColor;

        private final float mActionBarSize;
        private final float mActionBarElevation;

        public CollapsingToolbarScrollListener() {
            mToolbar = Objects.requireNonNull(mBinding.toolbar);
            mBackground = mBinding.background;
            mBackgroundPattern = mBinding.backgroundPattern;
            mContent = mBinding.common;

            mDarkColor = getDarkColor();

            TypedValue typedValue = new TypedValue();
            mContent.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
            mActionBarSize = typedValue.getDimension(getResources().getDisplayMetrics());
            mActionBarElevation = mToolbar.getElevation();
        }

        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            final float totalFadeOut = 1.5f;

            // fade out header
            float max = mContent.getTop() - totalFadeOut * mActionBarSize;
            float alpha = 1 - scrollY / max;

            if (alpha < 0) alpha = 0;
            else if (alpha > 1) alpha = 1;

            mBackgroundPattern.setImageAlpha((int) (alpha * 255));
            mBackground.setTranslationY(- scrollY / 2f);

            boolean shouldShowTitle = mContent.getTop() - scrollY + mBinding.title.getBottom() < mActionBarSize;
            boolean shouldShowActionBar = alpha == 0;
            if (shouldShowTitle || shouldShowActionBar) {
                mToolbarBackgroundColor.setValue(mDarkColor);

                float height, elevation;
                if (shouldShowTitle) {
                    mToolbarTitleVisible.setValue(true);
                    elevation = mActionBarElevation;
                    height = mActionBarSize;
                } else {
                    mToolbarTitleVisible.setValue(false);
                    float toolbarTransition = (totalFadeOut * mActionBarSize - (mContent.getTop() - scrollY)) / ((totalFadeOut - 1) * mActionBarSize);
                    if (toolbarTransition < 0) toolbarTransition = 0;
                    else if (toolbarTransition > 1) toolbarTransition = 1;

                    // toolbarTransition == 0 => toolbar completely merged with background
                    // toolbarTransition == 1 => only toolbar visible
                    elevation = toolbarTransition * mActionBarElevation;
                    height = (totalFadeOut - (totalFadeOut - 1) * toolbarTransition) * mActionBarSize;
                }

                ViewUtils.setHeight(mToolbar, (int) height);
                ViewCompat.setElevation(mToolbar, elevation);
            } else {
                mToolbarTitleVisible.setValue(false);
                mToolbarBackgroundColor.setValue(Color.TRANSPARENT);
            }
        }
    }

    /**
     * a touch listener that delegates touch events to the main NestedScrollView
     */
    @RequiredArgsConstructor
    private class TouchDelegator implements View.OnTouchListener {
        private final boolean mClickAllowed;
        private boolean mIntercepted;

        private final int[] sourceLocation = new int[2];
        private final int[] targetLocation = new int[2];
        private final Matrix matrix = new Matrix();

        public TouchDelegator() {
            this(true);
        }

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
            view.getLocationOnScreen(sourceLocation);
            mBinding.common.getLocationOnScreen(targetLocation);

            matrix.reset();
            matrix.postTranslate(
                    sourceLocation[0] - targetLocation[0],
                    sourceLocation[1] - targetLocation[1]
            );
            motionEvent.transform(matrix);
        }
    }
}
