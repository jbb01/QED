package com.jonahbauer.qed.activities.sheets.event;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class EventInfoBottomSheet extends AbstractInfoBottomSheet {
    private static final String ARGUMENT_EVENT = "event";

    private EventViewModel mEventViewModel;

    @NonNull
    public static EventInfoBottomSheet newInstance(Event event) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_EVENT, event);
        EventInfoBottomSheet sheet = new EventInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public EventInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;

        Event event = args.getParcelable(ARGUMENT_EVENT);
        if (event == null) {
            // TODO show toast
            dismiss();
            return;
        }

        mEventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        mEventViewModel.load(event);
    }

    @Override
    public int getColor() {
        return Themes.colorful(getEvent().getId());
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return EventInfoFragment.newInstance();
    }

    @NonNull
    private Event getEvent() {
        StatusWrapper<Event> wrapper = mEventViewModel.getEvent().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Event event = wrapper.getValue();
        assert event != null : "Event should not be null";
        return event;
    }
}
