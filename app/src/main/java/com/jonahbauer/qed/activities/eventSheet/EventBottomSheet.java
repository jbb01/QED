package com.jonahbauer.qed.activities.eventSheet;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.ColorfulBottomSheetCallback;
import com.jonahbauer.qed.layoutStuff.ToolbarBottomSheetCallback;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetBehavior;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetDialogFragment;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.event.Event;

import static com.jonahbauer.qed.activities.eventSheet.EventFragment.ARG_EVENT;
import static com.jonahbauer.qed.activities.eventSheet.EventFragment.ARG_EVENT_ID;
import static com.jonahbauer.qed.activities.eventSheet.EventFragment.TAG_EVENT_FRAGMENT;

public class EventBottomSheet extends ViewPagerBottomSheetDialogFragment implements QEDPageReceiver<Event> {
    private boolean mReceivedError;
    private boolean mDismissed;

    private ProgressBar mProgressBar;
    private Toolbar mToolbar;

    private Event mEvent;


    @NonNull
    public static EventBottomSheet newInstance(long eventId) {
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        EventBottomSheet fragment = new EventBottomSheet();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static EventBottomSheet newInstance(@NonNull Event event) {
        return newInstance(event.id);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        long eventId = args.getLong(ARG_EVENT_ID);
        mEvent = args.getParcelable(ARG_EVENT);

        if (mEvent == null) {
            QEDDBPages.getEvent(getClass().toString(), eventId, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_with_toolbar, container);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        int toolbarHeight = 0;
        Context context = getContext();
        if (context != null) {
            TypedArray typedArray = context.obtainStyledAttributes(new int[] {android.R.attr.actionBarSize});
            toolbarHeight = typedArray.getDimensionPixelSize(0, 0);
            typedArray.recycle();
        }

        int statusBarHeight = 0;
        Activity activity = getActivity();
        if (activity != null) {
            Rect rectangle = new Rect();
            Window window = activity.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            statusBarHeight = rectangle.top;
        }

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) layoutParams = new LinearLayout.LayoutParams(dm.widthPixels, dm.heightPixels);
        else layoutParams.height = dm.heightPixels;
        view.setLayoutParams(layoutParams);

        View rest = view.findViewById(R.id.content_frame);
        ViewGroup.LayoutParams restLayoutParams = rest.getLayoutParams();
        if (restLayoutParams == null) restLayoutParams = new LinearLayout.LayoutParams(dm.widthPixels, dm.heightPixels - toolbarHeight - statusBarHeight);
        else restLayoutParams.height = dm.heightPixels - toolbarHeight - statusBarHeight;
        rest.setLayoutParams(restLayoutParams);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.progress_bar);

        mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_close, null));
        mToolbar.setNavigationOnClickListener(v -> dismiss());
        mToolbar.setTitleTextColor(Color.BLACK);

        ViewGroup rest = view.findViewById(R.id.content_frame);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            ViewPagerBottomSheetBehavior<?> mBehavior;

            @Override
            public void onViewAttachedToWindow(View v) {
                mBehavior = ViewPagerBottomSheetBehavior.from((FrameLayout) v.getParent());
                mBehavior.addBottomSheetCallback(new ToolbarBottomSheetCallback(mToolbar, rest, EventBottomSheet.this));
                mBehavior.addBottomSheetCallback(new ColorfulBottomSheetCallback(EventBottomSheet.this, requireActivity().getWindow(), view.getRootView().findViewById(R.id.touch_outside), Color.WHITE));
            }

            @Override
            public void onViewDetachedFromWindow(View v) {}
        });

        if (mEvent != null) {
            onPageReceived(null, mEvent);
        }
    }

    @Override
    public void onPageReceived(String tag, Event event) {
        if (mDismissed) return;

        mEvent = event;

        if (mEvent == null) {
            Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            this.dismiss();
            return;
        }

        mToolbar.setTitle(event.title);
        mProgressBar.setVisibility(View.GONE);

        openFragment();
    }

    private void openFragment() {
        EventFragment fragment = new EventFragment(R.style.AppTheme_BottomSheetDialog);
        Bundle args = getArguments();
        args = args != null ? args : new Bundle();
        args.putParcelable(ARG_EVENT, mEvent);
        fragment.setArguments(args);
        getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, TAG_EVENT_FRAGMENT).commitAllowingStateLoss();
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(tag, reason, cause);
        if (mDismissed) return;

        final String errorString;
        if (REASON_NETWORK.equals(reason) && getContext() != null)
            errorString = getContext().getString(R.string.cant_connect);
        else if (getContext() != null)
            errorString = getContext().getString(R.string.unknown_error);
        else
            errorString = "Severe Error!";

        if (!mReceivedError) {
            mReceivedError = true;
            mProgressBar.post(() -> Toast.makeText(getContext(), errorString, Toast.LENGTH_SHORT).show());
            mProgressBar.postDelayed(() -> mReceivedError = false, 5000);
            dismiss();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        mDismissed = true;
        super.onDismiss(dialog);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Bundle args = getArguments();
            args = args != null ? args : new Bundle();
            args.putParcelable(ARG_EVENT, mEvent);
            EventSideSheet eventSideSheet = new EventSideSheet();
            eventSideSheet.setArguments(args);
            eventSideSheet.show(getParentFragmentManager(), eventSideSheet.getTag());
            this.dismiss();
        }
    }
}