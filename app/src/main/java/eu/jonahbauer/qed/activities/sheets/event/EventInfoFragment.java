package eu.jonahbauer.qed.activities.sheets.event;

import android.app.PendingIntent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.LinearLayout;

import androidx.navigation.NavDeepLinkBuilder;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;
import eu.jonahbauer.qed.activities.mainFragments.EventFragmentArgs;
import eu.jonahbauer.qed.activities.mainFragments.EventFragmentDirections;
import eu.jonahbauer.qed.activities.mainFragments.RegistrationFragmentArgs;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.FragmentInfoEventBinding;
import eu.jonahbauer.qed.layoutStuff.views.ListItem;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.viewmodel.EventViewModel;
import eu.jonahbauer.qed.networking.NetworkConstants;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.databinding.BindingAdapter;
import androidx.navigation.Navigation;

public class EventInfoFragment extends InfoFragment {
    private static final String SAVED_EXPANDED = "expanded";

    private EventViewModel mEventViewModel;
    private FragmentInfoEventBinding mBinding;

    private boolean mExpanded;

    public static EventInfoFragment newInstance() {
        return new EventInfoFragment();
    }

    public EventInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventViewModel = getViewModelProvider(R.id.nav_event).get(EventViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoEventBinding.inflate(inflater, container, false);
        mEventViewModel.getValue().observe(getViewLifecycleOwner(), mBinding::setEvent);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var color = getColor();

        mBinding.toggleParticipantsButton.setOnClickListener(this::toggleParticipantsExpanded);
        mBinding.toggleParticipantsButton.setIconTint(color);

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleParticipantsButton.setTitleTextAppearance(textAppearanceButton);
        mBinding.toggleParticipantsButton.setTitleTextColor(color);

        if (savedInstanceState != null) {
            mExpanded = savedInstanceState.getBoolean(SAVED_EXPANDED);
        }
        setParticipantsExpanded(mExpanded);
    }

    @NonNull
    private Event getEvent() {
        return Objects.requireNonNull(mEventViewModel.getValue().getValue());
    }

    @Override
    protected long getDesignSeed() {
        return getEvent().getId();
    }

    @Override
    protected boolean isOpenInBrowserSupported() {
        return true;
    }

    @Override
    protected @NonNull String getOpenInBrowserLink() {
        return String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_EVENT, getEvent().getId());
    }

    public void toggleParticipantsExpanded(@Nullable View view) {
        setParticipantsExpanded(!mExpanded);
    }

    public void setParticipantsExpanded(boolean expanded) {
        mExpanded = expanded;
        LinearLayout list = mBinding.participantList;
        ListItem button = mBinding.toggleParticipantsButton;

        list.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
        button.setIcon(mExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        button.setTitle(mExpanded ? R.string.event_show_less : R.string.event_show_more);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_EXPANDED, mExpanded);
    }

    @BindingAdapter("event_organizers")
    public static void bindOrganizers(ViewGroup parent, Collection<Registration> organizers) {
        bindList(parent, organizers, (registration, item) -> {
            item.setIcon(R.drawable.ic_event_orga);
            item.setTitle(registration.getPersonName());
            item.setSubtitle(R.string.registration_orga);
            item.setOnClickListener(v -> showRegistration(parent, registration));
        });
    }

    @BindingAdapter("event_participants")
    public static void bindParticipants(ViewGroup parent, Collection<Registration> participants) {
        var nonOrganizers = participants.stream().filter(r -> !r.isOrganizer()).collect(Collectors.toList());
        bindList(parent, nonOrganizers, (registration, item) -> {
            var status = Objects.requireNonNullElse(registration.getStatus(), Registration.Status.PENDING);
            item.setIcon(status.getDrawableRes());
            item.setTitle(registration.getPersonName());
            item.setSubtitle(status.getStringRes());
            item.setOnClickListener(v -> showRegistration(parent, registration));
        });
    }

    private static void showRegistration(View view, Registration registration) {
        try {
            var navController = Navigation.findNavController(view);

            var action = EventFragmentDirections.showRegistration(registration.getId());
            action.setRegistration(registration);
            navController.navigate(action);
        } catch (IllegalStateException e) {
            try {
                var intent = new NavDeepLinkBuilder(view.getContext())
                        .setComponentName(MainActivity.class)
                        .setGraph(R.navigation.main)
                        .addDestination(R.id.nav_database_events)
                        .addDestination(R.id.nav_event, new EventFragmentArgs.Builder(registration.getEventId()).setEvent(registration.getEvent()).build().toBundle())
                        .addDestination(R.id.nav_registration, new RegistrationFragmentArgs.Builder(registration.getId()).setRegistration(registration).build().toBundle())
                        .createPendingIntent();
                intent.send();
            } catch (PendingIntent.CanceledException ignored) {}
        }
    }
}
