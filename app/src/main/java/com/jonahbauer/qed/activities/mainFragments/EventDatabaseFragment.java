package com.jonahbauer.qed.activities.mainFragments;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentEventsDatabaseBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.adapter.EventAdapter;
import com.jonahbauer.qed.model.viewmodel.EventListViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;

public class EventDatabaseFragment extends Fragment implements AdapterView.OnItemClickListener {
    private EventAdapter mEventAdapter;
    private FragmentEventsDatabaseBinding mBinding;
    private MenuItem mRefresh;

    private EventListViewModel mEventListViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEventsDatabaseBinding.inflate(inflater, container, false);
        mEventListViewModel = ViewUtils.getViewModelProvider(this).get(EventListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindows(this);
        TransitionUtils.postponeEnterAnimationToPreDraw(this, view);

        mEventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setAdapter(mEventAdapter);

        mEventListViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            mBinding.setStatus(events.getCode());

            if (mRefresh != null) {
                mRefresh.setEnabled(events.getCode() != StatusWrapper.STATUS_PRELOADED);
            }

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
            var extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(view, getString(R.string.transition_name_single_fragment))
                    .build();
            var action = EventDatabaseFragmentDirections.showEvent(event);
            Navigation.findNavController(view).navigate(action, extras);

            TransitionUtils.setupReenterElevationScale(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_database, menu);
        mRefresh = menu.findItem(R.id.menu_refresh);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mEventListViewModel.load();

            Drawable icon = mRefresh.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            return true;
        }
        return false;
    }
}
