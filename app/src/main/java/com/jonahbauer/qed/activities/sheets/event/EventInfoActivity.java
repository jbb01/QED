package com.jonahbauer.qed.activities.sheets.event;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoActivity;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class EventInfoActivity extends AbstractInfoActivity {
    public static final String ARGUMENT_EVENT = "event";

    private EventViewModel mEventViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle args = getIntent().getExtras();
        assert args != null;

        Event event = args.getParcelable(ARGUMENT_EVENT);
        if (event == null) {
            // TODO show toast
            finish();
            return;
        }

        mEventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        mEventViewModel.load(event);

        super.onCreate(savedInstanceState);
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
