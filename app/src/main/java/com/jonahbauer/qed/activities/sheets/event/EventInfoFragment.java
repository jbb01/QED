package com.jonahbauer.qed.activities.sheets.event;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoEventBinding;
import com.jonahbauer.qed.databinding.ListItemBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.EventViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.util.Map;

public class EventInfoFragment extends AbstractInfoFragment {
    private EventViewModel mEventViewModel;
    private FragmentInfoEventBinding mBinding;

    public static EventInfoFragment newInstance() {
        return new EventInfoFragment();
    }

    public EventInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewModelStoreOwner owner = getParentFragment();
        if (owner == null) owner = requireActivity();
        mEventViewModel = new ViewModelProvider(owner).get(EventViewModel.class);
    }

    @Override
    protected ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoEventBinding.inflate(inflater, container, true);
        mEventViewModel.getEvent().observe(getViewLifecycleOwner(), eventStatusWrapper -> {
            mBinding.setEvent(eventStatusWrapper.getValue());
            mBinding.setLoading(eventStatusWrapper.getCode() == StatusWrapper.STATUS_PRELOADED);
        });
        mBinding.setColor(getColor());
        return mBinding;
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
    protected int getColor() {
        return Themes.colorful(getEvent().getId());
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.toggleParticipantsButton.setOnClick(this::toggleParticipants);
        mBinding.toggleParticipantsButton.icon.setColorFilter(getColor());

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleParticipantsButton.title.setTextAppearance(textAppearanceButton);
        mBinding.toggleParticipantsButton.title.setTextColor(getColor());
    }

    public void toggleParticipants(View v) {
        LinearLayout list = mBinding.participantList;
        ListItemBinding button = mBinding.toggleParticipantsButton;

        boolean visible = list.getVisibility() == View.VISIBLE;
        list.setVisibility(visible ? View.GONE : View.VISIBLE);
        button.setIcon(AppCompatResources.getDrawable(v.getContext(), visible ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up));
        button.setTitle(v.getContext().getString(visible ? R.string.event_show_more : R.string.event_show_less));
    }

    @BindingAdapter("event_organizers")
    public static void bindOrganizers(ViewGroup parent, Map<String, Registration> organizers) {
        Context context = parent.getContext();
        parent.removeAllViews();
        organizers.forEach((name, registration) -> {
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_event_orga));
            item.setTitle(name);
            item.setSubtitle(context.getString(R.string.registration_orga));
        });
    }

    @BindingAdapter("event_participants")
    public static void bindParticipants(ViewGroup parent, Map<String, Registration> participants) {
        Context context = parent.getContext();
        parent.removeAllViews();
        participants.forEach((name, registration) -> {
            if (registration.isOrganizer()) return;
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, registration.getStatus().toDrawableRes()));
            item.setTitle(name);
            item.setSubtitle(context.getString(registration.getStatus().toStringRes()));
        });
    }
}
