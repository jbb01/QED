package eu.jonahbauer.qed.ui.views;

import android.view.MotionEvent;

public interface InterceptingView {

    void setOnInterceptTouchListener(OnInterceptTouchListener onInterceptTouchListener);

    interface OnInterceptTouchListener {
        boolean onInterceptTouchEvent(MotionEvent event);
    }
}
