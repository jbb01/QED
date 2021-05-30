package com.jonahbauer.qed.activities.sheets;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.BottomSheetWithFragmentBinding;
import com.jonahbauer.qed.layoutStuff.ColorfulBottomSheetCallback;

public abstract class AbstractInfoBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetBehavior.BottomSheetCallback sheetCallback;

    public AbstractInfoBottomSheet() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        BottomSheetWithFragmentBinding binding = BottomSheetWithFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        view.setOnClickListener(v -> dismiss());
        adjustHeight(view);

        return view;
    }

    void adjustHeight() {
        adjustHeight(getView());
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

                if (sheetCallback == null) {
                    sheetCallback = new ColorfulBottomSheetCallback(AbstractInfoBottomSheet.this, rootWindow, touchOutside, getColor());
                }

                mBehavior = BottomSheetBehavior.from((View) view.getParent());
                mBehavior.addBottomSheetCallback(sheetCallback);
            }

            @Override
            public void onWindowDetached() {
                view.getViewTreeObserver().removeOnWindowAttachListener(this);

                if (mBehavior != null)
                    mBehavior.removeBottomSheetCallback(sheetCallback);
            }
        });

        // instantiate fragment
        Fragment fragment = createFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, fragment, fragment.getTag()).commit();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        View view = getView();

        // inflate new version of fragment so that layout matches orientation
        if (view != null) view.post(() -> {
            View common = view.findViewById(R.id.common);

            Fragment fragment = createFragment();

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, fragment, fragment.getTag());
            transaction.addSharedElement(common, "common");
            transaction.commit();

            adjustHeight(getView());
        });
    }

    @ColorInt
    public abstract int getColor();

    public abstract AbstractInfoFragment createFragment();
}
