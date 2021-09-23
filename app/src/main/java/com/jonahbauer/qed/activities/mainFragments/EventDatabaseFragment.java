package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentEventsDatabaseBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.adapter.EventAdapter;
import com.jonahbauer.qed.model.viewmodel.EventListViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.ArrayList;

public class EventDatabaseFragment extends QEDFragment implements AdapterView.OnItemClickListener {
    private EventAdapter mEventAdapter;
    private FragmentEventsDatabaseBinding mBinding;

    private EventListViewModel mEventListViewModel;

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
        mEventListViewModel.load();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEventsDatabaseBinding.bind(view);
        mEventListViewModel = new ViewModelProvider(this).get(EventListViewModel.class);

        mEventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setAdapter(mEventAdapter);

        mEventListViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            mBinding.setStatus(events.getCode());

            mEventAdapter.clear();
            if (events.getCode() == StatusWrapper.STATUS_LOADED) {
                mEventAdapter.addAll(events.getValue());
            } else if (events.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = events.getReason();
                mBinding.setError(getString(reason == Reason.EMPTY ? R.string.database_empty : reason.getStringRes()));
            }
            mEventAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Event event = mEventAdapter.getItem(position);
        if (event != null) {
            Actions.showInfoSheet(this, event);
        }
    }
}
