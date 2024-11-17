package eu.jonahbauer.qed.activities.main;

import androidx.annotation.NonNull;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.activities.sheets.event.EventInfoFragment;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.viewmodel.EventViewModel;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.util.ViewUtils;

public class EventFragment extends MainInfoFragment {

    private EventViewModel mEventViewModel;

    @Override
    protected void onCreateViewModel() {
        EventFragmentArgs args = EventFragmentArgs.fromBundle(getArguments());
        Event event = args.getEvent();
        if (event == null) event = new Event(args.getId());

        mEventViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_event).get(EventViewModel.class);
        mEventViewModel.load(event);

        setDesignSeed(event.getId());
    }

    @Override
    protected @NonNull InfoFragment createFragment() {
        return EventInfoFragment.newInstance();
    }

    @Override
    protected @NonNull InfoViewModel<?> getInfoViewModel() {
        return mEventViewModel;
    }
}
