package com.jonahbauer.qed.activities.sheets.person;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoPersonBinding;
import com.jonahbauer.qed.layoutStuff.views.ListItem;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.util.Pair;
import androidx.databinding.BindingAdapter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PersonInfoFragment extends InfoFragment {
    private PersonViewModel mPersonViewModel;
    private FragmentInfoPersonBinding mBinding;

    private boolean mHideTitle;

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
        icons.put("signal", R.drawable.ic_person_contact_signal);
        icons.put("skype", R.drawable.ic_person_contact_skype);
        icons.put("teamspeak", R.drawable.ic_person_contact_teamspeak);
        icons.put("telegram", R.drawable.ic_person_contact_telegram);
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
        actions.put("skype", Actions::openSkype);
        actions.put("telegram", Actions::openTelegram);
        actions.put("telefon", Actions::dial);
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
        mPersonViewModel = getViewModelProvider().get(PersonViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoPersonBinding.inflate(inflater, container, false);
        mPersonViewModel.getPerson().observe(getViewLifecycleOwner(), personStatusWrapper -> {
            mBinding.setPerson(personStatusWrapper.getValue());
            mBinding.setLoading(personStatusWrapper.getCode() == StatusWrapper.STATUS_PRELOADED);
        });
        mBinding.setColor(getColor());
        if (mHideTitle) hideTitle();
        return mBinding.getRoot();
    }

    @NonNull
    private Person getPerson() {
        StatusWrapper<Person> wrapper = mPersonViewModel.getPerson().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Person person = wrapper.getValue();
        assert person != null : "Person should not be null";
        return person;
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getPerson().getId());
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
        return mBinding.title.getBottom();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.toggleEventsButton.setOnClickListener(this::toggleEvents);
        mBinding.toggleEventsButton.setIconTint(getColor());

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textAppearanceButton, typedValue, true);
        @StyleRes int textAppearanceButton = typedValue.data;

        mBinding.toggleEventsButton.setTitleTextAppearance(textAppearanceButton);
        mBinding.toggleEventsButton.setTitleTextColor(getColor());
    }

    public void toggleEvents(View v) {
        LinearLayout list = mBinding.registrationList;
        ListItem button = mBinding.toggleEventsButton;

        boolean visible = list.getVisibility() == View.VISIBLE;
        list.setVisibility(visible ? View.GONE : View.VISIBLE);
        button.setIcon(visible ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
        button.setTitle(visible ? R.string.event_show_more : R.string.event_show_less);
    }

    @Override
    public void hideTitle() {
        mHideTitle = true;
        if (mBinding != null) {
            mBinding.titleLayout.setVisibility(View.GONE);
        }
    }

    @BindingAdapter("person_addresses")
    public static void bindAddresses(ViewGroup parent, Set<String> addresses) {
        Context context = parent.getContext();
        parent.removeAllViews();
        addresses.forEach((address) -> {
            ListItem item = new ListItem(context);
            item.setIcon(R.drawable.ic_person_location);
            item.setTitle(address);
            item.setOnClickListener(v -> Actions.showOnMap(context, address));
            item.setOnLongClickListener(v -> {
                Actions.copy(context, parent.getRootView(), address, address);
                return true;
            });
            parent.addView(item);
        });
    }

    @BindingAdapter("person_contacts")
    public static void bindContacts(ViewGroup parent, Set<Pair<String, String>> contacts) {
        Context context = parent.getContext();
        parent.removeAllViews();
        contacts.forEach((contact) -> {
            ListItem item = new ListItem(context);
            item.setIcon(CONTACT_ICONS.getInt(contact.first.toLowerCase()));
            item.setTitle(contact.second);
            item.setSubtitle(contact.first);

            BiConsumer<Context, String> action = CONTACT_ACTIONS.get(contact.first.toLowerCase());
            if (action != null) {
                item.setOnClickListener(v -> action.accept(context, contact.second));
            }

            item.setOnLongClickListener(v -> {
                Actions.copy(context, parent.getRootView(), contact.first, contact.second);
                return true;
            });

            parent.addView(item);
        });
    }

    @BindingAdapter("person_registrations")
    public static void bindRegistrations(ViewGroup parent, Map<String, Registration> registrations) {
        Context context = parent.getContext();
        parent.removeAllViews();
        registrations.forEach((title, registration) -> {
            ListItem item = new ListItem(context);
            item.setIcon(R.drawable.ic_person_event);
            item.setTitle(title);
            item.setSubtitle(registration.getStatus().toStringRes());
            parent.addView(item);
            // TODO subtitle orga
        });
    }
}
