package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.QEDDBEventsList;
import com.jonahbauer.qed.qeddb.QEDDBEventsListReceiver;
import com.jonahbauer.qed.qeddb.QEDDBLogin;
import com.jonahbauer.qed.qeddb.QEDDBLoginReceiver;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.event.EventAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventDatabaseFragment extends Fragment implements QEDDBLoginReceiver, QEDDBEventsListReceiver {
    private EventAdapter eventAdapter;

    private ListView eventListView;
    private ProgressBar searchProgress;
    private char[] sessionId;
    private char[] cookie;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QEDDBLogin qeddbLogin = new QEDDBLogin();
        qeddbLogin.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_events_database, container, false);

        eventListView = view.findViewById(R.id.event_list_view);
        searchProgress = view.findViewById(R.id.search_progress);

        eventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        eventListView.setAdapter(eventAdapter);
        eventListView.setOnItemClickListener((parent, view1, position, id) -> showBottomSheetDialogFragment(eventAdapter.getItem((int) id)));

        return view;
    }

    @Override
    public void onDestroy() {
        if (sessionId != null) for (int i = 0; i < sessionId.length; i++) sessionId[i] = 0;
        if (cookie != null) for (int i = 0; i < cookie.length; i++) cookie[i] = 0;
        super.onDestroy();
    }

    @Override
    public void onReceiveSessionId(char[] sessionId, char[] cookie) {
        if (sessionId == null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.database_login_failed));
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        } else {
            this.sessionId = Arrays.copyOf(sessionId, sessionId.length);
            this.cookie = Arrays.copyOf(cookie, cookie.length);
            QEDDBEventsList qeddbEvents = new QEDDBEventsList();
            qeddbEvents.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionId, cookie);
        }
    }

    @SuppressWarnings("unused")
    public void showBottomSheetDialogFragment(String eventId) {
        assert getFragmentManager() != null;

        EventBottomSheet eventBottomSheet = EventBottomSheet.newInstance(sessionId, cookie, eventId);
        eventBottomSheet.show(getFragmentManager(), eventBottomSheet.getTag());
    }

    public void showBottomSheetDialogFragment(Event event) {
        assert getFragmentManager() != null;

        EventBottomSheet eventBottomSheet = EventBottomSheet.newInstance(sessionId, cookie, event);
        eventBottomSheet.show(getFragmentManager(), eventBottomSheet.getTag());
    }

    @Override
    public void onEventsListReceived(List<Event> events) {
        eventAdapter.addAll(events);
        eventAdapter.notifyDataSetChanged();
        searchProgress.setVisibility(View.GONE);
        eventListView.setVisibility(View.VISIBLE);
    }
}
