package eu.jonahbauer.qed.layoutStuff.views;

import android.view.MotionEvent;

public interface InterceptingView {

    void setOnInterceptTouchListener(OnInterceptTouchListener onInterceptTouchListener);

    interface OnInterceptTouchListener {
        boolean onInterceptTouchEvent(MotionEvent event);
    }
}
