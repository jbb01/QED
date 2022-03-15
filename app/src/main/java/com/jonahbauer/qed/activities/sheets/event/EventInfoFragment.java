package com.jonahbauer.qed.activities.sheets.event;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoEventBinding;
import com.jonahbauer.qed.layoutStuff.views.ListItem;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.databinding.BindingAdapter;

public class EventInfoFragment extends InfoFragment {
    private EventViewModel mEventViewModel;
    private FragmentInfoEventBinding mBinding;

    private boolean mHideTitle;

    public static EventInfoFragment newInstance() {
        return new EventInfoFragment();
    }

    public EventInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventViewModel = getViewModelProvider().get(EventViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoEventBinding.inflate(inflater, container, false);
        mEventViewModel.getEvent().observe(getViewLifecycleOwner(), eventStatusWrapper -> {
            mBinding.setEvent(eventStatusWrapper.getValue());
            mBinding.setLoading(eventStatusWrapper.getCode() == StatusWrapper.STATUS_PRELOADED);
        });
        mBinding.setColor(getColor());
        if (mHideTitle) hideTitle();
        return mBinding.getRoot();
    }

    @NonNull
    private Event getEvent() {
        StatusWrapper<Event> wrapper = mEventViewModel.getEvent().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Event event = wrapper.getValue();
        assert event != null : "Event should not be null";
        return event;
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getEvent().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getEvent().getId());
    }

    @Override
    protected String getTitle() {
        return getEvent().getTitle();
    }

    @Override
    protected float getTitleBottom() {
        return mBinding.title.getBottom();
    }

    @Override
    public void hideTitle() {
        mHideTitle = true;
        if (mBinding != null) {
            mBinding.titleLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.toggleParticipantsButton.setOnClickListener(this::toggleParticipants);
        mBinding.toggleParticipantsButton.setIconTint(getColor());

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleParticipantsButton.setTitleTextAppearance(textAppearanceButton);
        mBinding.toggleParticipantsButton.setTitleTextColor(getColor());
    }

    public void toggleParticipants(View v) {
        LinearLayout list = mBinding.participantList;
        ListItem button = mBinding.toggleParticipantsButton;

        boolean visible = list.getVisibility() == View.VISIBLE;
        list.setVisibility(visible ? View.GONE : View.VISIBLE);
        button.setIcon(visible ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
        button.setTitle(visible ? R.string.event_show_more : R.string.event_show_less);
    }

    @BindingAdapter("event_organizers")
    public static void bindOrganizers(ViewGroup parent, Map<String, Registration> organizers) {
        Context context = parent.getContext();
        parent.removeAllViews();
        organizers.forEach((name, registration) -> {
            ListItem item = new ListItem(context);
            item.setIcon(R.drawable.ic_event_orga);
            item.setTitle(name);
            item.setSubtitle(R.string.registration_orga);
            parent.addView(item);
        });
    }

    @BindingAdapter("event_participants")
    public static void bindParticipants(ViewGroup parent, Map<String, Registration> participants) {
        Context context = parent.getContext();
        parent.removeAllViews();
        participants.forEach((name, registration) -> {
            if (registration.isOrganizer()) return;
            ListItem item = new ListItem(context);
            item.setIcon(registration.getStatus().toDrawableRes());
            item.setTitle(name);
            item.setSubtitle(registration.getStatus().toStringRes());
            parent.addView(item);
        });
    }
}
