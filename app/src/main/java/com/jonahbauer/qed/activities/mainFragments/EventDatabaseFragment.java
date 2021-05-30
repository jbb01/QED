package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.event.EventInfoBottomSheet;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.adapter.EventAdapter;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventDatabaseFragment extends QEDFragment implements QEDPageReceiver<List<Event>> {
    private EventAdapter mEventAdapter;

    private ListView mEventListView;
    private ProgressBar mSearchProgress;
    private TextView mErrorLabel;

    @NonNull
    public static EventDatabaseFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_events_database);

        EventDatabaseFragment fragment = new EventDatabaseFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        mSearchProgress.setVisibility(View.VISIBLE);
        mErrorLabel.setVisibility(View.GONE);
        mEventListView.setVisibility(View.GONE);
        QEDDBPages.getEventList(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mEventListView = view.findViewById(R.id.event_list_view);
        mSearchProgress = view.findViewById(R.id.search_progress);
        mErrorLabel = view.findViewById(R.id.label_error);

        mEventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        mEventListView.setAdapter(mEventAdapter);
        mEventListView.setOnItemClickListener((parent, view1, position, id)
                -> showBottomSheetDialogFragment(Objects.requireNonNull(mEventAdapter.getItem((int) id))));
    }

    private void showBottomSheetDialogFragment(@NonNull Event event) {
        EventInfoBottomSheet sheet = EventInfoBottomSheet.newInstance(event);
        sheet.show(getParentFragmentManager(), sheet.getTag());
    }

    @Override
    public void onPageReceived(List<Event> events) {
        mEventAdapter.clear();
        mEventAdapter.addAll(events);
        mEventAdapter.notifyDataSetChanged();

        if (events.size() == 0) {
            setError(getString(R.string.database_empty));
        } else {
            mSearchProgress.setVisibility(View.GONE);
            mEventListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onError(List<Event> events, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(events, reason, cause);

        final String errorString;

        if (Reason.NETWORK.equals(reason)) {
            errorString = getString(R.string.database_offline);
        } else {
            errorString = getString(R.string.error_unknown);
        }

        setError(errorString);
    }

    private void setError(String error) {
        mHandler.post(() -> {
            if (error == null) {
                mErrorLabel.setVisibility(View.GONE);
            } else {
                mErrorLabel.setText(error);
                mErrorLabel.setVisibility(View.VISIBLE);
                mSearchProgress.setVisibility(View.GONE);
                mEventListView.setVisibility(View.GONE);
            }
        });
    }
}
