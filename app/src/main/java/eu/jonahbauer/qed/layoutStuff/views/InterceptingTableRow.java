package eu.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TableRow;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InterceptingTableRow extends TableRow implements InterceptingView {
    private OnInterceptTouchListener mOnInterceptTouchListener;

    public InterceptingTableRow(@NonNull Context context) {
        super(context);
    }

    public InterceptingTableRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mOnInterceptTouchListener == null
                ? super.onInterceptTouchEvent(ev)
                : mOnInterceptTouchListener.onInterceptTouchEvent(ev);
    }

    @Override
    public void setOnInterceptTouchListener(OnInterceptTouchListener onInterceptTouchListener) {
        this.mOnInterceptTouchListener = onInterceptTouchListener;
    }
}
