package com.jonahbauer.qed.activities.eventSheet;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Configuration;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.viewPagerSideSheet.ViewPagerSideSheetBehavior;
import com.jonahbauer.qed.layoutStuff.viewPagerSideSheet.ViewPagerSideSheetDialogFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;

import static com.jonahbauer.qed.activities.eventSheet.EventFragment.ARG_EVENT;
import static com.jonahbauer.qed.activities.eventSheet.EventFragment.ARG_FETCH_DATA;
import static com.jonahbauer.qed.activities.eventSheet.EventFragment.TAG_EVENT_FRAGMENT;

public class EventSideSheet extends ViewPagerSideSheetDialogFragment implements QEDPageReceiver<Event> {
    private boolean mReceivedError;
    private boolean mDismissed;

    private ProgressBar mProgressBar;
    private Toolbar mToolbar;

    private Event mEvent;

    @NonNull
    public static EventSideSheet newInstance(@NonNull Event event) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_EVENT, event);
        EventSideSheet fragment = new EventSideSheet();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        mEvent = args.getParcelable(ARG_EVENT);
        assert mEvent != null;

        boolean fetch = args.getBoolean(EventFragment.ARG_FETCH_DATA, true);
        if (fetch) {
            QEDDBPages.getEvent(mEvent, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_with_toolbar, container);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        int statusBarHeight = 0;
        Activity activity = getActivity();
        if (activity != null) {
            Rect rectangle = new Rect();
            Window window = activity.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            statusBarHeight = rectangle.top;
        }

        @SuppressWarnings("SuspiciousNameCombination")
        int width = dm.heightPixels;
        int height = dm.heightPixels - statusBarHeight;

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new LinearLayout.LayoutParams(width, height);
        } else {
            layoutParams.width = width;
            layoutParams.height = height;
        }
        view.setLayoutParams(layoutParams);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.progress_bar);

        mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_close));
        mToolbar.setNavigationOnClickListener(v -> dismiss());
        mToolbar.setTitleTextColor(Color.BLACK);
        mToolbar.setVisibility(View.VISIBLE);

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ViewPagerSideSheetBehavior<?> behavior = ViewPagerSideSheetBehavior.from((FrameLayout) v.getParent());
                behavior.setState(ViewPagerSideSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                view.removeOnAttachStateChangeListener(this);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {}
        });

        if (mEvent != null) {
            onPageReceived(mEvent);
        }
    }

    @Override
    public void onPageReceived(Event event) {
        if (mDismissed) return;

        mEvent = event;

        if (mEvent == null) {
            Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            this.dismiss();
            return;
        }

        mToolbar.setTitle(event.getTitle());
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
    public void onError(Event event, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(event, reason, cause);
        if (mDismissed) return;

        final String errorString;
        if (Reason.NETWORK.equals(reason) && getContext() != null)
            errorString = getContext().getString(R.string.cant_connect);
        else if (getContext() != null)
            errorString = getContext().getString(R.string.error_unknown);
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

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Bundle args = getArguments();
            args = args != null ? args : new Bundle();
            args.putParcelable(ARG_EVENT, mEvent);
            args.putBoolean(ARG_FETCH_DATA, false);
            EventBottomSheet eventBottomSheet = new EventBottomSheet();
            eventBottomSheet.setArguments(args);
            eventBottomSheet.show(getParentFragmentManager(), eventBottomSheet.getTag());
            this.dismiss();
        }
    }
}