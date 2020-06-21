/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) github/laenger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Upgraded to androidx.viewpager2.widget.ViewPager2
 */
package com.jonahbauer.qed.layoutStuff.viewPagerSideSheet;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetBehavior;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;


/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as
 * a bottom sheet.
 */
@SuppressWarnings("ALL")
public class ViewPagerSideSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * The bottom sheet is dragging.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * The bottom sheet is settling.
     */
    public static final int STATE_SETTLING = 2;

    /**
     * The bottom sheet is expanded.
     */
    public static final int STATE_EXPANDED = 3;

    /**
     * The bottom sheet is collapsed.
     */
    public static final int STATE_COLLAPSED = 4;

    /**
     * The bottom sheet is hidden.
     */
    public static final int STATE_HIDDEN = 5;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({STATE_EXPANDED, STATE_COLLAPSED, STATE_DRAGGING, STATE_SETTLING, STATE_HIDDEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    /**
     * Peek at the 16:9 ratio keyline of its parent.
     *
     * <p>This can be used as a parameter for {@link #setPeekHeight(int)}.
     * {@link #getPeekHeight()} will return this when the value is set.</p>
     */
    public static final int PEEK_WIDTH_AUTO = -1;

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;

    private float mMinimumVelocity;

    private float mMaximumVelocity;

    private int mPeekWidth;

    private boolean mPeekWidthAuto;

    private int mPeekWidthMin;

    int mMinOffset;

    int mMaxOffset;

    boolean mHideable;

    private boolean mSkipCollapsed;

    @State
    int mState = STATE_COLLAPSED;

    ViewDragHelper mViewDragHelper;

    private boolean mIgnoreEvents;

    private boolean mNestedScrolled;

    int mParentWidth;

    int mParentHeight;

    WeakReference<V> mViewRef;

    WeakReference<View> mNestedScrollingChildRef;

    @NonNull private final ArrayList<BottomSheetCallback> mCallbacks = new ArrayList<>();

    private VelocityTracker mVelocityTracker;

    int mActivePointerId;

    private int mInitialX;

    boolean mTouchingScrollingChild;

    /**
     * Default constructor for instantiating ViewPagerSideSheetBehaviors.
     */
    public ViewPagerSideSheetBehavior() {
    }

    /**
     * Default constructor for inflating ViewPagerSideSheetBehaviors from layout.
     *
     * @param context The {@link Context}.
     * @param attrs   The {@link AttributeSet}.
     */
    public ViewPagerSideSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout);
        TypedValue value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (value != null && value.data == PEEK_WIDTH_AUTO) {
            setPeekWidth(value.data);
        } else {
            setPeekWidth(a.getDimensionPixelSize(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_WIDTH_AUTO));
        }
        setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setSkipCollapsed(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), mState);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            mState = STATE_COLLAPSED;
        } else {
            mState = ss.state;
        }
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }
        int savedLeft = child.getLeft();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);
        // Offset the bottom sheet
        mParentWidth = parent.getWidth();
        mParentHeight = parent.getHeight();
        if (mPeekWidthAuto) {
            if (mPeekWidthMin == 0) {
                mPeekWidthMin = parent.getResources().getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min);
            }
            mPeekWidth = Math.max(mPeekWidthMin, mParentWidth - mParentHeight * 9 / 16);
        }

        mMinOffset = Math.max(0, mParentWidth - child.getWidth());
        mMaxOffset = Math.max(mParentWidth - mParentHeight, mMinOffset);

        if (mState == STATE_EXPANDED) {
            ViewCompat.offsetLeftAndRight(child, mMinOffset);
        } else if (mHideable && mState == STATE_HIDDEN) {
            ViewCompat.offsetLeftAndRight(child, mParentWidth);
        } else if (mState == STATE_COLLAPSED) {
            ViewCompat.offsetLeftAndRight(child, mMaxOffset);
        } else if (mState == STATE_DRAGGING || mState == STATE_SETTLING) {
            ViewCompat.offsetLeftAndRight(child, savedLeft - child.getLeft());
        }
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }
        mViewRef = new WeakReference<>(child);
        mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            mIgnoreEvents = true;
            return false;
        }
        int action = event.getActionMasked();
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchingScrollingChild = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mInitialX = (int) event.getX();
                int initialY = (int) event.getY();
                View scroll = mNestedScrollingChildRef != null
                        ? mNestedScrollingChildRef.get() : null;
                if (scroll != null && parent.isPointInChildBounds(scroll, mInitialX, initialY)) {
                    mActivePointerId = event.getPointerId(event.getActionIndex());
                    mTouchingScrollingChild = true;
                }
                mIgnoreEvents = mActivePointerId == MotionEvent.INVALID_POINTER_ID &&
                        !parent.isPointInChildBounds(child, mInitialX, initialY);
                break;
        }
        if (!mIgnoreEvents && mViewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = mNestedScrollingChildRef.get();

        return action == MotionEvent.ACTION_MOVE && scroll != null &&
                !mIgnoreEvents && mState != STATE_DRAGGING &&
                !(parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) && scroll.canScrollHorizontally((int) Math.signum(mInitialX - event.getX()))) &&
                Math.abs(mInitialX - event.getX()) > mViewDragHelper.getTouchSlop();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        }
        int action = event.getActionMasked();
        if (mState == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
        }
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !mIgnoreEvents) {
            if (Math.abs(mInitialX - event.getX()) > mViewDragHelper.getTouchSlop()) {
                mViewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
            }
        }
        return !mIgnoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        mNestedScrolled = false;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
        // TODO left/right
        View scrollingChild = mNestedScrollingChildRef.get();
        if (target != scrollingChild) {
            return;
        }
        int currentLeft = child.getLeft();
        int newLeft = currentLeft - dx;
        if (dx > 0) { // Upward
            if (newLeft < mMinOffset) {
                consumed[1] = currentLeft - mMinOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dx < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newLeft <= mMaxOffset || mHideable) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentLeft - mMaxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        }
        dispatchOnSlide(child.getLeft());
        mNestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (child.getLeft() == mMinOffset) {
            setStateInternal(STATE_EXPANDED);
            return;
        }
        if (mNestedScrollingChildRef == null || target != mNestedScrollingChildRef.get()
                || !mNestedScrolled) {
            return;
        }

        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        float xVel = mVelocityTracker.getXVelocity(mActivePointerId);
        float yVel = mVelocityTracker.getYVelocity(mActivePointerId);

        int left;
        int targetState;
        if (xVel < 0 && Math.abs(xVel) > mMinimumVelocity && Math.abs(xVel) > Math.abs(yVel)) {
            left = mMinOffset;
            targetState = STATE_EXPANDED;
        } else if (mHideable && shouldHide(child, xVel)) {
            left = mParentWidth;
            targetState = STATE_HIDDEN;
        } else if (xVel > 0 && Math.abs(xVel) > mMinimumVelocity && Math.abs(xVel) > Math.abs(yVel)) {
            left = mMaxOffset;
            targetState = STATE_COLLAPSED;
        } else {
            // not scrolling much, i.e. stationary
            int currentLeft = child.getLeft();
            if (Math.abs(currentLeft - mMinOffset) < Math.abs(currentLeft - mMaxOffset)) {
                left = mMinOffset;
                targetState = STATE_EXPANDED;
            } else {
                left = mMaxOffset;
                targetState = STATE_COLLAPSED;
            }
        }

        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), left)) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }
        mNestedScrolled = false;
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
        return target == mNestedScrollingChildRef.get() && (mState != STATE_EXPANDED || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    }

    void invalidateScrollingChild() {
        final View scrollingChild = findScrollingChild(mViewRef.get());
        mNestedScrollingChildRef = new WeakReference<>(scrollingChild);
    }

    /**
     * Sets the width of the side sheet when it is collapsed.
     *
     * @param peekWidth The width of the collapsed side sheet in pixels, or
     *                   {@link #PEEK_WIDTH_AUTO} to configure the sheet to peek automatically
     *                   at 16:9 ratio keyline.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    public final void setPeekWidth(int peekWidth) {
        boolean layout = false;
        if (peekWidth == PEEK_WIDTH_AUTO) {
            if (!mPeekWidthAuto) {
                mPeekWidthAuto = true;
                layout = true;
            }
        } else if (mPeekWidthAuto || mPeekWidth != peekWidth) {
            mPeekWidthAuto = false;
            mPeekWidth = Math.max(0, peekWidth);
            mMaxOffset = mParentWidth - peekWidth;
            layout = true;
        }
        if (layout && mState == STATE_COLLAPSED && mViewRef != null) {
            V view = mViewRef.get();
            if (view != null) {
                view.requestLayout();
            }
        }
    }

    /**
     * Gets the width of the side sheet when it is collapsed.
     *
     * @return The width of the collapsed side sheet in pixels, or {@link #PEEK_WIDTH_AUTO}
     *         if the sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    public final int getPeekWidth() {
        return mPeekWidthAuto ? PEEK_WIDTH_AUTO : mPeekWidth;
    }

    /**
     * Sets whether this side sheet can hide when it is swiped right.
     *
     * @param hideable {@code true} to make this side sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public boolean isHideable() {
        return mHideable;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        mSkipCollapsed = skipCollapsed;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public boolean getSkipCollapsed() {
        return mSkipCollapsed;
    }

    /**
     * Sets a callback to be notified of side sheet events.
     *
     * @param callback The callback to notify when side sheet events occur.
     * @deprecated use {@link #addSideSheetCallback(BottomSheetCallback)} and {@link
     *     #removeSideSheetCallback(BottomSheetCallback)} instead
     */
    @Deprecated
    public void setSideSheetCallback(BottomSheetCallback callback) {
        Log.w("ViewPagerSideSheetBehavior",
                "ViewPagerSideSheetBehavior now supports multiple callbacks. `setSideSheetCallback()` removes"
                        + " all existing callbacks, including ones set internally by library authors, which"
                        + " may result in unintended behavior. This may change in the future. Please use"
                        + " `addSideSheetCallback()` and `removeSideSheetCallback()` instead to set your"
                        + " own callbacks.");
        mCallbacks.clear();
        if (callback != null) {
            mCallbacks.add(callback);
        }
    }

    /**
     * Adds a callback to be notified of side sheet events.
     *
     * @param callback The callback to notify when side sheet events occur.
     */
    public void addSideSheetCallback(@NonNull BottomSheetCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * Removes a previously added callback.
     *
     * @param callback The callback to remove.
     */
    public void removeSideSheetCallback(@NonNull BottomSheetCallback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, or
     *              {@link #STATE_HIDDEN}.
     */
    public final void setState(final @State int state) {
        if (state == mState) {
            return;
        }
        if (mViewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED || (mHideable && state == STATE_HIDDEN)) {
                mState = state;
            }
            return;
        }
        final V child = mViewRef.get();
        if (child == null) {
            return;
        }
        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(new Runnable() {
                @Override
                public void run() {
                    startSettlingAnimation(child, state);
                }
            });
        } else {
            startSettlingAnimation(child, state);
        }
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link #STATE_DRAGGING},
     * {@link #STATE_SETTLING}, and {@link #STATE_HIDDEN}.
     */
    @State
    public final int getState() {
        return mState;
    }

    void setStateInternal(@ViewPagerBottomSheetBehavior.State int state) {
        if (mState == state) {
            return;
        }
        mState = state;

        if (mViewRef == null) return;

        View bottomSheet = mViewRef.get();
        if (bottomSheet == null) {
            return;
        }

        for (BottomSheetCallback callback : mCallbacks) {
            callback.onStateChanged(bottomSheet, state);
        }
    }

    private void reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    private int calculatePeekWidth() {
        if (mPeekWidthAuto) {
            return Math.max(mPeekWidthMin, mParentWidth - mParentHeight * 9 / 16);
        }
        return mPeekWidth;
    }

    boolean shouldHide(View child, float xvel) {
        if (mSkipCollapsed) {
            return true;
        }
        if (child.getLeft() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        mPeekWidth = calculatePeekWidth();
        final float newLeft = child.getLeft() + xvel * HIDE_FRICTION;
        return Math.abs(newLeft - mMaxOffset) / (float) mPeekWidth > HIDE_THRESHOLD;
    }

    @VisibleForTesting
    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            if (view.canScrollHorizontally(-1) || view.canScrollHorizontally(1))
                return view;
        }
        if (view instanceof ViewPager2) {
            ViewPager2 viewPager = (ViewPager2) view;
            RecyclerView.Adapter adapter = viewPager.getAdapter();

            if (ViewCompat.isNestedScrollingEnabled(viewPager) && (viewPager.canScrollHorizontally(-1) || viewPager.canScrollHorizontally(1)))
                return viewPager;

            View recyclerViewImpl = viewPager.getChildAt(0);
            if (ViewCompat.isNestedScrollingEnabled(recyclerViewImpl) && (recyclerViewImpl.canScrollHorizontally(-1) || recyclerViewImpl.canScrollHorizontally(1)))
                return recyclerViewImpl;

            if (adapter instanceof ViewPagerSideSheetFragmentStateAdapter) {
                FragmentViewHolder viewHolder = ((ViewPagerSideSheetFragmentStateAdapter) adapter).getFragmentViewHolder(viewPager.getCurrentItem());
                View currentViewPagerChild = viewHolder.itemView;

                if (currentViewPagerChild == null) {
                    return null;
                }

                View scrollingChild = findScrollingChild(currentViewPagerChild);
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            } else {
                return null;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private float getXVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        return mVelocityTracker.getXVelocity(mActivePointerId);
    }

    void startSettlingAnimation(View child, int state) {
        int left;
        if (state == STATE_COLLAPSED) {
            left = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            left = mMinOffset;
        } else if (mHideable && state == STATE_HIDDEN) {
            left = mParentWidth;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        if (mViewDragHelper.smoothSlideViewTo(child, left, child.getTop())) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        } else {
            setStateInternal(state);
        }
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mState == STATE_DRAGGING) {
                return false;
            }
            if (mTouchingScrollingChild) {
                return false;
            }
            if (mState == STATE_EXPANDED && mActivePointerId == pointerId) {
                View scroll = mNestedScrollingChildRef.get();
                if (scroll != null && scroll.canScrollHorizontally(-1)) {
                    // Let the content scroll up
                    return false;
                }
            }
            return mViewRef != null && mViewRef.get() == child;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(left);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int left;
            @State int targetState;
            if (xvel < 0 && Math.abs(xvel) > mMinimumVelocity && Math.abs(xvel) > Math.abs(yvel)) {
                left = mMinOffset;
                targetState = STATE_EXPANDED;
            } else if (mHideable && shouldHide(releasedChild, xvel)) {
                left = mParentWidth;
                targetState = STATE_HIDDEN;
            } else if (xvel > 0 && Math.abs(xvel) > mMinimumVelocity && Math.abs(xvel) > Math.abs(yvel)) {
                left = mMaxOffset;
                targetState = STATE_COLLAPSED;
            } else {
                // not scrolling much, i.e. stationary
                int currentLeft = releasedChild.getLeft();
                if (Math.abs(currentLeft - mMinOffset) < Math.abs(currentLeft - mMaxOffset)) {
                    left = mMinOffset;
                    targetState = STATE_EXPANDED;
                } else {
                    left = mMaxOffset;
                    targetState = STATE_COLLAPSED;
                }
            }

            if (mViewDragHelper.settleCapturedViewAt(left, releasedChild.getTop())) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return MathUtils.clamp(left, mMinOffset, mHideable ? mParentWidth : mMaxOffset);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            if (mHideable) {
                return mParentWidth - mMinOffset;
            } else {
                return mMaxOffset - mMinOffset;
            }
        }
    };

    void dispatchOnSlide(int left) {
        View sideSheet = mViewRef.get();
        if (sideSheet != null && !mCallbacks.isEmpty()) {
            float slideOffset = (left > mMaxOffset)
                    ? (float) (mMaxOffset - left) / (mParentWidth - mMaxOffset)
                    : (float) (mMaxOffset - left) / ((mMaxOffset - mMinOffset));

            for (BottomSheetCallback callback : mCallbacks) {
                callback.onSlide(sideSheet, slideOffset);
            }
        }
    }

    @VisibleForTesting
    int getPeekWidthMin() {
        return mPeekWidthMin;
    }

    private class SettleRunnable implements Runnable {

        private final View mView;

        @State
        private final int mTargetState;

        SettleRunnable(View view, @State int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends AbsSavedState {
        @State
        final int state;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
        }

        public SavedState(Parcelable superState, @State int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * A utility function to get the {@link ViewPagerSideSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link ViewPagerSideSheetBehavior}.
     * @return The {@link ViewPagerSideSheetBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> ViewPagerSideSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof ViewPagerSideSheetBehavior)) {
            throw new IllegalArgumentException("The view is not associated with ViewPagerBottomSheetBehavior");
        }
        return (ViewPagerSideSheetBehavior<V>) behavior;
    }

}

