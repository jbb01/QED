package eu.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.SingleFragmentBinding;
import eu.jonahbauer.qed.layoutStuff.themes.Theme;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.util.TransitionUtils;
import eu.jonahbauer.qed.util.ViewUtils;

import java.util.concurrent.TimeUnit;

public abstract class MainInfoFragment extends Fragment {
    private @ColorInt int mColor;
    private @ColorInt int mBackgroundColor;
    private SingleFragmentBinding mBinding;

    @Override
    public final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateViewModel();

        TransitionUtils.setupDefaultTransitions(this, mBackgroundColor);
        TransitionUtils.setupEnterContainerTransform(this, mBackgroundColor);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = SingleFragmentBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

        if (fragment.hasMenu()) {
            requireActivity().addMenuProvider(fragment, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    protected void setDesignSeed(long seed) {
        var theme = Theme.getCurrentTheme();
        var context = requireContext();
        setColor(theme.getIconColor(context, seed), theme.getSheetBackgroundColorDark(context, seed));
    }

    protected void setColor(@ColorInt int color, @ColorInt int backgroundColor) {
        mColor = color;
        mBackgroundColor = backgroundColor;
        if (mBinding != null) {
            mBinding.setColor(color);
        }
    }

    protected abstract void onCreateViewModel();

    protected abstract @NonNull InfoFragment createFragment();

    protected abstract @NonNull InfoViewModel<?> getInfoViewModel();
}
