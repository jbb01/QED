package eu.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.SingleFragmentBinding;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.TransitionUtils;
import eu.jonahbauer.qed.util.ViewUtils;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class MainInfoFragment extends Fragment {
    private @ColorInt int mColor;
    private SingleFragmentBinding mBinding;

    @Override
    public final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateViewModel();

        TransitionUtils.setupDefaultTransitions(this, mColor);
        TransitionUtils.setupEnterContainerTransform(this, mColor);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = SingleFragmentBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        postponeEnterTransition(200, TimeUnit.MILLISECONDS);

        var manager = getChildFragmentManager();

        var fragment = (InfoFragment) manager.findFragmentById(R.id.fragment);
        if (fragment == null) {
            fragment = createFragment();
            manager.beginTransaction().replace(R.id.fragment, fragment).commit();
        }

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
        model.getToolbarTitle().observe(getViewLifecycleOwner(), title -> ViewUtils.setActionBarText(this, title));
        mBinding.setColor(mColor);

        if (fragment.isOpenInBrowserSupported()) {
            var menuProvider = new OpenInBrowserMenuProvider(fragment::getOpenInBrowserLink);
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    protected void setColor(@ColorInt int color) {
        mColor = color;
        if (mBinding != null) {
            mBinding.setColor(color);
        }
    }

    public abstract void onCreateViewModel();

    public abstract @NonNull InfoFragment createFragment();

    public abstract @NonNull InfoViewModel<?> getInfoViewModel();

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
