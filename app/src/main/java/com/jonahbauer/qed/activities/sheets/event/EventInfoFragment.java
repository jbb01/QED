package com.jonahbauer.qed.activities.sheets.event;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoEventBinding;
import com.jonahbauer.qed.databinding.ListItemBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.util.Themes;

import java.util.Map;

public class EventInfoFragment extends AbstractInfoFragment {
    private EventViewModel eventViewModel;
    private FragmentInfoEventBinding binding;

    public static EventInfoFragment newInstance() {
        return new EventInfoFragment();
    }

    public EventInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    @Override
    protected ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInfoEventBinding.inflate(inflater, container, true);
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), eventStatusWrapper -> binding.setEvent(eventStatusWrapper.getValue()));
        return binding;
    }

    private Event getEvent() {
        return eventViewModel.getEvent().getValue().getValue();
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
        return binding.title.getBottom();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toggleParticipantsButton.setOnClick(this::toggleParticipants);
        binding.toggleParticipantsButton.icon.setColorFilter(getColor());
        binding.toggleParticipantsButton.title.setTextAppearance(R.style.AppTheme_TextAppearance_Button);
        binding.toggleParticipantsButton.title.setTextColor(getColor());
    }

    public void toggleParticipants(View v) {
        LinearLayout list = binding.participantList;
        ListItemBinding button = binding.toggleParticipantsButton;

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
