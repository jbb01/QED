package eu.jonahbauer.qed.activities.sheets.event;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.viewmodel.EventViewModel;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;

import java.util.Objects;

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
    public long getDesignSeed() {
        return getEvent().getId();
    }

    @Override
    public @NonNull InfoFragment createFragment() {
        return EventInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mEventViewModel;
    }

    @NonNull
    private Event getEvent() {
        return Objects.requireNonNull(mEventViewModel.getValue().getValue());
    }
}
