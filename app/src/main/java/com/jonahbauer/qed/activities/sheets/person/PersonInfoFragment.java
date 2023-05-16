package com.jonahbauer.qed.activities.sheets.person;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.navigation.NavDeepLinkBuilder;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.mainFragments.PersonFragmentArgs;
import com.jonahbauer.qed.activities.mainFragments.PersonFragmentDirections;
import com.jonahbauer.qed.activities.mainFragments.RegistrationFragmentArgs;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoPersonBinding;
import com.jonahbauer.qed.layoutStuff.views.ListItem;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.*;

import java.util.*;
import java.util.function.BiConsumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.databinding.BindingAdapter;
import androidx.navigation.Navigation;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PersonInfoFragment extends InfoFragment {
    private static final String SAVED_EXPANDED = "expanded";

    private PersonViewModel mPersonViewModel;
    private FragmentInfoPersonBinding mBinding;

    private boolean mExpanded;

    private static final Object2IntMap<String> CONTACT_ICONS;
    private static final Object2ObjectMap<String, BiConsumer<Context, String>> CONTACT_ACTIONS;

    static {
        Object2IntOpenHashMap<String> icons = new Object2IntOpenHashMap<>();
        icons.put("daheim", R.drawable.ic_person_contact_phone);
        icons.put("discord", R.drawable.ic_person_contact_discord);
        icons.put("email", R.drawable.ic_person_contact_mail);
        icons.put("facebook", R.drawable.ic_person_contact_facebook);
        icons.put("github", R.drawable.ic_person_contact_github);
        icons.put("gpg", R.drawable.ic_person_contact_gpg);
        icons.put("icq", R.drawable.ic_person_contact_icq);
        icons.put("instagram", R.drawable.ic_person_contact_instagram);
        icons.put("irc", R.drawable.ic_person_contact_irc);
        icons.put("jabber", R.drawable.ic_person_contact_jabber);
        icons.put("matrix", R.drawable.ic_person_contact_matrix);
        icons.put("mobil", R.drawable.ic_person_contact_phone);
        icons.put("mumble", R.drawable.ic_person_contact_mumble);
        icons.put("phön", R.drawable.ic_person_contact_phoen);
        icons.put("signal", R.drawable.ic_person_contact_signal);
        icons.put("skype", R.drawable.ic_person_contact_skype);
        icons.put("teamspeak", R.drawable.ic_person_contact_teamspeak);
        icons.put("telegram", R.drawable.ic_person_contact_telegram);
        icons.put("threema", R.drawable.ic_person_contact_threema);
        icons.put("telefon", R.drawable.ic_person_contact_phone);
        icons.put("twitter", R.drawable.ic_person_contact_twitter);
        icons.put("whatsapp", R.drawable.ic_person_contact_whatsapp);
        icons.put("youtube", R.drawable.ic_person_contact_youtube);
        icons.put("xmpp", R.drawable.ic_person_contact_xmpp);
        icons.defaultReturnValue(R.drawable.ic_person_contact);
        CONTACT_ICONS = Object2IntMaps.unmodifiable(icons);

        Object2ObjectOpenHashMap<String, BiConsumer<Context, String>> actions = new Object2ObjectOpenHashMap<>();
        actions.put("daheim", Actions::dial);
        actions.put("email", Actions::sendTo);
        actions.put("facebook", Actions::openFacebook);
        actions.put("github", Actions::openGithub);
        actions.put("instagram", Actions::openInstagram);
        actions.put("jabber", Actions::openXmpp);
        actions.put("matrix", Actions::openMatrix);
        actions.put("mobil", Actions::dial);
        actions.put("phön", Actions::dial);
        actions.put("skype", Actions::openSkype);
        actions.put("telegram", Actions::openTelegram);
        actions.put("telefon", Actions::dial);
        actions.put("threema", Actions::openThreema);
        actions.put("twitter", Actions::openTwitter);
        actions.put("whatsapp", Actions::openWhatsapp);
        actions.put("xmpp", Actions::openXmpp);
        CONTACT_ACTIONS = Object2ObjectMaps.unmodifiable(actions);
    }

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
        mBinding.setColor(getColor());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.toggleEventsButton.setOnClickListener(this::toggleEventsExpanded);
        mBinding.toggleEventsButton.setIconTint(getColor());

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleEventsButton.setTitleTextAppearance(textAppearanceButton);
        mBinding.toggleEventsButton.setTitleTextColor(getColor());

        if (savedInstanceState != null) {
            mExpanded = savedInstanceState.getBoolean(SAVED_EXPANDED);
        }
        setEventsExpanded(mExpanded);
    }

    private @NonNull Person getPerson() {
        return Objects.requireNonNull(mPersonViewModel.getValue().getValue());
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getPerson().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getPerson().getId());
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
    public boolean isOpenInBrowserSupported() {
        return true;
    }

    @Override
    public @NonNull String getOpenInBrowserLink() {
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
            var context = item.getContext();
            item.setIcon(CONTACT_ICONS.getInt(contact.first.toLowerCase()));
            item.setTitle(contact.second);
            item.setSubtitle(contact.first);

            BiConsumer<Context, String> action = CONTACT_ACTIONS.get(contact.first.toLowerCase());
            if (action != null) {
                item.setOnClickListener(v -> action.accept(context, contact.second));
            } else {
                item.setOnClickListener(null);
            }

            item.setOnLongClickListener(v -> {
                var name = person.getFullName();
                Actions.copy(context, parent, context.getString(R.string.person_clip_label_contact, contact.first, name), contact.second);
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
