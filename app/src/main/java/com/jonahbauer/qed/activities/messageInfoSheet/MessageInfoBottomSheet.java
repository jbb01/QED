package com.jonahbauer.qed.activities.messageInfoSheet;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.ColorfulBottomSheetCallback;
import com.jonahbauer.qed.model.Message;

public class MessageInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String ARGUMENT_MESSAGE = "message";

    private Message mMessage;

    private BottomSheetCallback mSheetCallback;

    @NonNull
    public static MessageInfoBottomSheet newInstance(Message message) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_MESSAGE, message);
        MessageInfoBottomSheet sheet = new MessageInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public MessageInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        mMessage = args.getParcelable(ARGUMENT_MESSAGE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_with_fragment, container, false);
        view.setOnClickListener(v -> dismiss());

        adjustHeight(view);

        return view;
    }

    private void adjustHeight(View view) {
        int contentHeight = 0;
        Activity activity = getActivity();
        if (activity != null) {
            Rect rectangle = new Rect();
            Window window = activity.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            contentHeight = rectangle.height();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add bottom sheet callback
        view.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
            BottomSheetBehavior<?> mBehavior;

            @Override
            public void onWindowAttached() {
                View touchOutside = view.getRootView().findViewById(R.id.touch_outside);

                Activity activity = requireActivity();
                Window rootWindow = activity.getWindow();

                mBehavior = BottomSheetBehavior.from((View) view.getParent());
                mBehavior.addBottomSheetCallback(mSheetCallback != null ? mSheetCallback : (mSheetCallback = new ColorfulBottomSheetCallback(MessageInfoBottomSheet.this, rootWindow, touchOutside, mMessage.getTransformedColor())));
            }

            @Override
            public void onWindowDetached() {
                view.getViewTreeObserver().removeOnWindowAttachListener(this);

                if (mBehavior != null)
                    mBehavior.removeBottomSheetCallback(mSheetCallback);
            }
        });

        // instantiate fragment
        MessageInfoFragment fragment = MessageInfoFragment.newInstance(mMessage);
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, fragment, fragment.getTag()).commit();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        View view = getView();

        // inflate new version of fragment so that layout matches orientation
        if (view != null) view.post(() -> {
            View content = view.findViewById(R.id.content_holder);

            Fragment fragment = MessageInfoFragment.newInstance(mMessage);

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, fragment, fragment.getTag());
            transaction.addSharedElement(content, "content");
            transaction.commit();

            adjustHeight(getView());
        });
    }
}
