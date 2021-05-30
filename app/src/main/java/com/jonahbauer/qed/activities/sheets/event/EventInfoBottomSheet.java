package com.jonahbauer.qed.activities.sheets.event;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.util.Themes;

public class EventInfoBottomSheet extends AbstractInfoBottomSheet {
    private static final String ARGUMENT_EVENT = "event";

    private EventViewModel eventViewModel;

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

        eventViewModel = new ViewModelProvider(getActivity()).get(EventViewModel.class);
        eventViewModel.load(args.getParcelable(ARGUMENT_EVENT));
    }

    @Override
    public int getColor() {
        return Themes.colorful(getEvent().getId());
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return EventInfoFragment.newInstance();
    }

    private Event getEvent() {
        return eventViewModel.getEvent().getValue().getValue();
    }
}
