package eu.jonahbauer.qed.activities.sheets.person;

import android.app.PendingIntent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.databinding.BindingAdapter;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;
import eu.jonahbauer.qed.activities.mainFragments.PersonFragmentArgs;
import eu.jonahbauer.qed.activities.mainFragments.PersonFragmentDirections;
import eu.jonahbauer.qed.activities.mainFragments.RegistrationFragmentArgs;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.FragmentInfoPersonBinding;
import eu.jonahbauer.qed.ui.views.ListItem;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.viewmodel.PersonViewModel;
import eu.jonahbauer.qed.network.util.NetworkConstants;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.TextUtils;
import eu.jonahbauer.qed.util.TimeUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.lang.ref.WeakReference;
import java.util.*;

public class PersonInfoFragment extends InfoFragment {
    private static final String SAVED_EXPANDED = "expanded";

    private PersonViewModel mPersonViewModel;
    private FragmentInfoPersonBinding mBinding;

    private boolean mExpanded;
    private final List<WeakReference<MenuItem>> items = new ArrayList<>();

    public static PersonInfoFragment newInstance() {
        return new PersonInfoFragment();
    }

    public PersonInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPersonViewModel = getViewModelProvider(R.id.nav_person).get(PersonViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoPersonBinding.inflate(inflater, container, false);
        mPersonViewModel.getValue().observe(getViewLifecycleOwner(), mBinding::setPerson);
        mPersonViewModel.getFavorite().observe(getViewLifecycleOwner(), favorite -> {
            var title = favorite ? R.string.person_favorite_remove : R.string.person_favorite_add;
            var icon = favorite ? R.drawable.ic_menu_favorite_yes : R.drawable.ic_menu_favorite_no;

            var it = items.iterator();
            while (it.hasNext()) {
                var item = it.next().get();
                if (item == null) {
                    it.remove();
                } else {
                    item.setTitle(title);
                    item.setIcon(icon);
                }
            }
        });
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var color = getColor();

        mBinding.toggleEventsButton.setOnClickListener(this::toggleEventsExpanded);
        mBinding.toggleEventsButton.setIconTint(color);

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleEventsButton.setTitleTextAppearance(textAppearanceButton);
        mBinding.toggleEventsButton.setTitleTextColor(color);

        if (savedInstanceState != null) {
            mExpanded = savedInstanceState.getBoolean(SAVED_EXPANDED);
        }
        setEventsExpanded(mExpanded);
    }

    private @NonNull Person getPerson() {
        return Objects.requireNonNull(mPersonViewModel.getValue().getValue());
    }


    @Override
    protected long getDesignSeed() {
        return getPerson().getId();
    }

    public void toggleEventsExpanded(@Nullable View view) {
        setEventsExpanded(!mExpanded);
    }

    public void setEventsExpanded(boolean expanded) {
        mExpanded = expanded;
        LinearLayout list = mBinding.registrationList;
        ListItem button = mBinding.toggleEventsButton;

        list.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
        button.setIcon(mExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        button.setTitle(mExpanded ? R.string.event_show_less : R.string.event_show_more);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_EXPANDED, mExpanded);
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        var favorites = Preferences.getDatabase().getFavorites();
        var isFavorite = favorites.contains(getPerson().getId());
        var item = menu.add(
                Menu.NONE, R.id.person_favorite, Menu.NONE,
                isFavorite ? R.string.person_favorite_remove : R.string.person_favorite_add
        );
        item.setIcon(isFavorite ? R.drawable.ic_menu_favorite_yes : R.drawable.ic_menu_favorite_no);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        items.add(new WeakReference<>(item));
        super.onCreateMenu(menu, inflater);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.person_favorite) {
            var id = getPerson().getId();
            var favorites = new LongOpenHashSet(Preferences.getDatabase().getFavorites());

            // update shared prefs
            var isFavorite = favorites.contains(id);
            if (isFavorite) {
                favorites.remove(id);
            } else {
                favorites.add(id);
            }
            Preferences.getDatabase().setFavorites(favorites);

            return true;
        } else {
            return super.onMenuItemSelected(item);
        }
    }

    @Override
    protected boolean isOpenInBrowserSupported() {
        return true;
    }

    @Override
    protected @NonNull String getOpenInBrowserLink() {
        return String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_PERSON, getPerson().getId());
    }

    @BindingAdapter("person_addresses")
    public static void bindAddresses(ViewGroup parent, Person person) {
        var addresses = person.getAddresses();
        bindList(parent, addresses, (address, item) -> {
            var context = item.getContext();
            item.setIcon(R.drawable.ic_person_location);
            item.setTitle(address);
            item.setOnClickListener(v -> Actions.showOnMap(context, address));
            item.setOnLongClickListener(v -> {
                var name = person.getFullName();
                Actions.copy(context, parent, context.getString(R.string.person_clip_label_address, name), address);
                return true;
            });
        });
    }

    @BindingAdapter("person_contacts")
    public static void bindContacts(ViewGroup parent, Person person) {
        var contacts = person.getContacts();
        bindList(parent, contacts, (contact, item) -> {
            var type = contact.getType();
            var context = item.getContext();
            item.setIcon(type.getIcon());
            item.setTitle(contact.getValue());
            item.setSubtitle(contact.getLabel());

            var action = type.getAction();
            if (action != null) {
                item.setOnClickListener(v -> action.accept(context, contact.getValue()));
            } else {
                item.setOnClickListener(null);
            }

            item.setOnLongClickListener(v -> {
                var name = person.getFullName();
                Actions.copy(context, parent, context.getString(R.string.person_clip_label_contact, contact.getLabel(), name), contact.getValue());
                return true;
            });
        });
    }

    @BindingAdapter("person_payments")
    public static void bindPayments(ViewGroup parent, Collection<Person.Payment> payments) {
        bindList(parent, payments, (payment, item) -> {
            var context = item.getContext();
            var type = payment.getType();
            item.setIcon(R.drawable.ic_person_payment);
            item.setTitle(TextUtils.formatRange(context, TimeUtils::format, payment.getStart(), payment.getEnd()));
            item.setSubtitle(type != null ? type.getStringRes() : R.string.empty);
        });
    }

    @BindingAdapter("person_registrations")
    public static void bindRegistrations(ViewGroup parent, Collection<Registration> registrations) {
        bindList(parent, registrations, (registration, item) -> {
            var status = registration.getStatus();
            item.setIcon(R.drawable.ic_person_event);
            item.setTitle(registration.getEventTitle());
            item.setSubtitle(status != null ? status.getStringRes() : R.string.empty);
            item.setOnClickListener(v -> showRegistration(v, registration));
        });
    }

    @BindingAdapter("person_privacy")
    public static void bindPrivacy(ListItem item, EnumSet<Person.Privacy> privacy) {
        if (privacy == null) {
            item.setTitle(null);
            return;
        }

        var context = item.getContext();
        var joiner = new StringJoiner(", ");
        joiner.setEmptyValue(context.getText(R.string.person_privacy_none));
        for (var p : privacy) {
            joiner.add(context.getString(p.getStringRes()));
        }
        item.setTitle(joiner.toString());
    }

    private static void showRegistration(View view, Registration registration) {
        try {
            var navController = Navigation.findNavController(view);

            var action = PersonFragmentDirections.showRegistration(registration.getId());
            action.setRegistration(registration);
            navController.navigate(action);
        } catch (IllegalStateException e) {
            try {
                var intent = new NavDeepLinkBuilder(view.getContext())
                        .setComponentName(MainActivity.class)
                        .setGraph(R.navigation.main)
                        .addDestination(R.id.nav_database_persons)
                        .addDestination(R.id.nav_person, new PersonFragmentArgs.Builder(registration.getPersonId()).setPerson(registration.getPerson()).build().toBundle())
                        .addDestination(R.id.nav_registration, new RegistrationFragmentArgs.Builder(registration.getId()).setRegistration(registration).build().toBundle())
                        .createPendingIntent();
                intent.send();
            } catch (PendingIntent.CanceledException ignored) {}
        }
    }
}
