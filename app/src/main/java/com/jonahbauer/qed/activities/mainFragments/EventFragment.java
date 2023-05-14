package com.jonahbauer.qed.activities.mainFragments;

import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.activities.sheets.event.EventInfoFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.ViewUtils;

public class EventFragment extends MainInfoFragment {

    private EventViewModel mEventViewModel;

    @Override
    public void onCreateViewModel() {
        EventFragmentArgs args = EventFragmentArgs.fromBundle(getArguments());
        Event event = args.getEvent();
        if (event == null) event = new Event(args.getId());

        mEventViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_event).get(EventViewModel.class);
        mEventViewModel.load(event);

        setColor(Themes.colorful(requireContext(), event.getId()));
    }

    @Override
    public void onObserveViewModel() {
        mEventViewModel.getEvent().observe(getViewLifecycleOwner(), wrapper -> {
            Event value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, value.getTitle());
            }

            if (wrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
                startPostponedEnterTransition();
            }
        });
    }

    @NonNull
    @Override
    public InfoFragment createFragment() {
        return EventInfoFragment.newInstance();
    }
}
