package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.activities.sheets.event.EventInfoFragment;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.ViewUtils;

public class EventFragment extends Fragment {

    private EventViewModel mEventViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventFragmentArgs args = EventFragmentArgs.fromBundle(getArguments());
        Event event = args.getEvent();

        mEventViewModel = ViewUtils.getViewModelProvider(this).get(EventViewModel.class);
        mEventViewModel.load(event);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SingleFragmentBinding binding = SingleFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindowsBottom(view);

        mEventViewModel.getEvent().observe(getViewLifecycleOwner(), wrapper -> {
            Event value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, value.getTitle());
                ViewUtils.setActionBarColor(this, Themes.colorful(value.getId()));
            }
        });

        getChildFragmentManager().addFragmentOnAttachListener(new FragmentOnAttachListener() {
            @Override
            public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment f) {
                if (f instanceof InfoFragment) {
                    ((InfoFragment) f).hideTitle();
                    getChildFragmentManager().removeFragmentOnAttachListener(this);
                }
            }
        });

        EventInfoFragment fragment = EventInfoFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
    }

    @Override
    public void onDestroyView() {
        ViewUtils.resetActionBarColor(this);
        super.onDestroyView();
    }
}
