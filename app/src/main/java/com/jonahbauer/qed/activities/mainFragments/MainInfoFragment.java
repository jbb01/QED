package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.TransitionUtils;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class MainInfoFragment extends Fragment {
    private @ColorInt int mColor;

    @Override
    public final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateViewModel();

        TransitionUtils.setupDefaultTransitions(this, mColor);
        TransitionUtils.setupEnterContainerTransform(this, mColor);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SingleFragmentBinding binding = SingleFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        postponeEnterTransition(200, TimeUnit.MILLISECONDS);

        onObserveViewModel();

        var manager = getChildFragmentManager();

        var fragment = (InfoFragment) manager.findFragmentById(R.id.fragment);
        if (fragment == null) {
            fragment = createFragment();
            fragment.hideTitle();
            manager.beginTransaction().replace(R.id.fragment, fragment).commit();
        }

        if (fragment.isOpenInBrowserSupported()) {
            var menuProvider = new OpenInBrowserMenuProvider(fragment::getOpenInBrowserLink);
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    protected void setColor(@ColorInt int color) {
        this.mColor = color;
    }

    public abstract void onCreateViewModel();

    public abstract void onObserveViewModel();

    @NonNull
    public abstract InfoFragment createFragment();

    @RequiredArgsConstructor
    private class OpenInBrowserMenuProvider implements MenuProvider {
        private final Supplier<String> mUrl;

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_open_in_browser, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.open_in_browser) {
                Actions.openInBrowser(requireContext(), mUrl.get());
                return true;
            }
            return false;
        }
    }
}
