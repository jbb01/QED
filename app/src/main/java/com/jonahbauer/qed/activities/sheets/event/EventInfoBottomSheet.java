package com.jonahbauer.qed.activities.sheets.event;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class EventInfoBottomSheet extends InfoBottomSheet {
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
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mEventViewModel = getViewModelProvider().get(EventViewModel.class);
        mEventViewModel.load(event);
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getEvent().getId());
    }

    @Override
    public int getBackground() {
        return Themes.pattern(getEvent().getId());
    }

    @NonNull
    @Override
    public InfoFragment createFragment() {
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
