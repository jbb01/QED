package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.event.EventInfoFragment;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.concurrent.TimeUnit;

public class EventFragment extends Fragment {

    private EventViewModel mEventViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventFragmentArgs args = EventFragmentArgs.fromBundle(getArguments());
        Event event = args.getEvent();

        mEventViewModel = ViewUtils.getViewModelProvider(this).get(EventViewModel.class);
        mEventViewModel.load(event);

        var color = Themes.colorful(requireContext(), event.getId());
        TransitionUtils.setupDefaultTransitions(this, color);
        TransitionUtils.setupEnterContainerTransform(this, color);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SingleFragmentBinding binding = SingleFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindows(this);
        postponeEnterTransition(200, TimeUnit.MILLISECONDS);

        mEventViewModel.getEvent().observe(getViewLifecycleOwner(), wrapper -> {
            Event value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, value.getTitle());
            }

            if (wrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
                startPostponedEnterTransition();
            }
        });

        var manager = getChildFragmentManager();
        if (!(manager.findFragmentById(R.id.fragment) instanceof EventInfoFragment)) {
            EventInfoFragment fragment = EventInfoFragment.newInstance();
            fragment.hideTitle();
            manager.beginTransaction().replace(R.id.fragment, fragment).commit();
        }
    }
}
