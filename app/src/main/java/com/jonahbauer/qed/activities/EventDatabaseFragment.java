package com.jonahbauer.qed.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.event.EventAdapter;

import java.util.ArrayList;
import java.util.List;

public class EventDatabaseFragment extends QEDFragment implements QEDPageReceiver<List<Event>> {
    private EventAdapter eventAdapter;

    private ListView eventListView;
    private ProgressBar searchProgress;
    private TextView errorLabel;

    static String showEvent;
    static int showEventId;
    static boolean shownEvent = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        searchProgress.setVisibility(View.VISIBLE);
        errorLabel.setVisibility(View.GONE);
        eventListView.setVisibility(View.GONE);
        QEDDBPages.getEventList(getClass().toString(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_events_database, container, false);

        eventListView = view.findViewById(R.id.event_list_view);
        searchProgress = view.findViewById(R.id.search_progress);
        errorLabel = view.findViewById(R.id.label_error);

        eventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        eventListView.setAdapter(eventAdapter);
        eventListView.setOnItemClickListener((parent, view1, position, id) -> showBottomSheetDialogFragment(eventAdapter.getItem((int) id)));

        return view;
    }

    @SuppressWarnings("unused")
    private void showBottomSheetDialogFragment(String eventId) {
        assert getFragmentManager() != null;

        EventBottomSheet eventBottomSheet = EventBottomSheet.newInstance(eventId);
        eventBottomSheet.show(getFragmentManager(), eventBottomSheet.getTag());
    }

    private void showBottomSheetDialogFragment(Event event) {
        assert getFragmentManager() != null;

        EventBottomSheet eventBottomSheet = EventBottomSheet.newInstance(event);
        eventBottomSheet.show(getFragmentManager(), eventBottomSheet.getTag());
    }

    @Override
    public void onPageReceived(String tag, List<Event> events) {
        eventAdapter.clear();
        eventAdapter.addAll(events);
        eventAdapter.notifyDataSetChanged();
        searchProgress.setVisibility(View.GONE);
        eventListView.setVisibility(View.VISIBLE);

        if ((showEvent != null || showEventId != 0) && !shownEvent) {
            if (showEventId != 0) showBottomSheetDialogFragment(String.valueOf(showEventId));
            else {
                for (Event event : events) {
                    if (event.name.equals(showEvent)) {
                        showBottomSheetDialogFragment(event);
                        break;
                    }
                }
            }
            shownEvent = true;
        }
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(tag, reason, cause);

        final String errorString;

        if (REASON_NETWORK.equals(reason))
            errorString = getString(R.string.database_offline);
        else
            errorString = getString(R.string.unknown_error);

        handler.post(() -> {
            errorLabel.setText(errorString);
            errorLabel.setVisibility(View.VISIBLE);
            searchProgress.setVisibility(View.GONE);
            eventListView.setVisibility(View.GONE);
        });
    }
}
