package com.jonahbauer.qed.activities.sheets.person;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Pair;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoPersonBinding;
import com.jonahbauer.qed.databinding.ListItemBinding;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.util.Intents;
import com.jonahbauer.qed.util.Themes;

import java.util.List;
import java.util.Map;

public class PersonInfoFragment extends AbstractInfoFragment {
    private PersonViewModel personViewModel;
    private FragmentInfoPersonBinding binding;

    public static PersonInfoFragment newInstance() {
        return new PersonInfoFragment();
    }

    public PersonInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        personViewModel = new ViewModelProvider(requireActivity()).get(PersonViewModel.class);
    }

    @Nullable
    @Override
    public ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInfoPersonBinding.inflate(inflater, container, true);
        personViewModel.getPerson().observe(getViewLifecycleOwner(), eventStatusWrapper -> binding.setPerson(eventStatusWrapper.getValue()));
        return binding;
    }

    private Person getPerson() {
        return personViewModel.getPerson().getValue().getValue();
    }

    @Override
    protected int getColor() {
        return Themes.colorful(getPerson().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getPerson().getId());
    }

    @Override
    protected String getTitle() {
        return getPerson().getFullName();
    }

    @Override
    protected float getTitleBottom() {
        return binding.title.getBottom();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toggleEventsButton.setOnClick(this::toggleEvents);
        binding.toggleEventsButton.icon.setColorFilter(getColor());
        binding.toggleEventsButton.title.setTextAppearance(R.style.AppTheme_TextAppearance_Button);
        binding.toggleEventsButton.title.setTextColor(getColor());
    }

    public void toggleEvents(View v) {
        LinearLayout list = binding.registrationList;
        ListItemBinding button = binding.toggleEventsButton;

        boolean visible = list.getVisibility() == View.VISIBLE;
        list.setVisibility(visible ? View.GONE : View.VISIBLE);
        button.setIcon(AppCompatResources.getDrawable(v.getContext(), visible ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up));
        button.setTitle(v.getContext().getString(visible ? R.string.event_show_more : R.string.event_show_less));
    }


    @BindingAdapter("person_contacts")
    public static void bindContacts(ViewGroup parent, List<Pair<String, String>> contacts) {
        Context context = parent.getContext();
        parent.removeAllViews();
        contacts.forEach((contact) -> {
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_person_contact));
            item.setTitle(contact.second);
            item.setSubtitle(contact.first);
            item.setOnClick(v -> Intents.dial(context, contact.second));
        });
    }

    @BindingAdapter("person_registrations")
    public static void bindRegistrations(ViewGroup parent, Map<String, Registration> registrations) {
        Context context = parent.getContext();
        parent.removeAllViews();
        registrations.forEach((title, registration) -> {
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_person_event));
            item.setTitle(title);
            item.setSubtitle(context.getString(registration.getStatus().toStringRes()));
            // TODO subtitle orga
        });
    }
}
