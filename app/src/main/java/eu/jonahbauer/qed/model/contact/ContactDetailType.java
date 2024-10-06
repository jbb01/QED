package eu.jonahbauer.qed.model.contact;

import android.content.Context;

import android.provider.ContactsContract.CommonDataKinds;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.Actions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public enum ContactDetailType {
    PHONE(R.drawable.ic_person_contact_phone, Actions::dial, null),
    DISCORD(R.drawable.ic_person_contact_discord, null, null),
    MAIL(R.drawable.ic_person_contact_mail, Actions::sendTo, null),
    FACEBOOK(R.drawable.ic_person_contact_facebook, Actions::openFacebook, null),
    GITHUB(R.drawable.ic_person_contact_github, Actions::openGithub, null),
    GPG(R.drawable.ic_person_contact_gpg, null, null),
    ICQ(R.drawable.ic_person_contact_icq, null, CommonDataKinds.Im.PROTOCOL_ICQ),
    INSTAGRAM(R.drawable.ic_person_contact_instagram, Actions::openInstagram, null),
    IRC(R.drawable.ic_person_contact_irc, null, null),
    JABBER(R.drawable.ic_person_contact_jabber, Actions::openXmpp, CommonDataKinds.Im.PROTOCOL_JABBER),
    MATRIX(R.drawable.ic_person_contact_matrix, Actions::openMatrix, null),
    MUMBLE(R.drawable.ic_person_contact_mumble, null, null),
    PHOEN(R.drawable.ic_person_contact_phoen, Actions::dial, null),
    SIGNAL(R.drawable.ic_person_contact_signal, null, null),
    SKYPE(R.drawable.ic_person_contact_skype, Actions::openSkype, CommonDataKinds.Im.PROTOCOL_SKYPE),
    TEAMSPEAK(R.drawable.ic_person_contact_teamspeak, null, null),
    TELEGRAM(R.drawable.ic_person_contact_telegram, Actions::openTelegram, null),
    THREEMA(R.drawable.ic_person_contact_threema, Actions::openThreema, null),
    TWITTER(R.drawable.ic_person_contact_twitter, Actions::openTwitter, null),
    WHATSAPP(R.drawable.ic_person_contact_whatsapp, Actions::openWhatsapp, null),
    YOUTUBE(R.drawable.ic_person_contact_youtube, null, null),
    XMPP(R.drawable.ic_person_contact_xmpp, Actions::openXmpp, null),
    UNKNOWN(R.drawable.ic_person_contact, null, null),
    ;


    public static final String MOBILE_PHONE_LABEL = "mobil";
    private static final Map<String, ContactDetailType> LABEL;
    static {
        LABEL = Map.ofEntries(
                Map.entry("telefon", PHONE),
                Map.entry(MOBILE_PHONE_LABEL, PHONE),
                Map.entry("daheim", PHONE),
                Map.entry("discord", DISCORD),
                Map.entry("email", MAIL),
                Map.entry("facebook", FACEBOOK),
                Map.entry("github", GITHUB),
                Map.entry("gpg", GPG),
                Map.entry("icq", ICQ),
                Map.entry("instagram", INSTAGRAM),
                Map.entry("irc", IRC),
                Map.entry("jabber", JABBER),
                Map.entry("matrix", MATRIX),
                Map.entry("mumble", MUMBLE),
                Map.entry("phön", PHOEN),
                Map.entry("signal", SIGNAL),
                Map.entry("skype", SKYPE),
                Map.entry("teamspeak", TEAMSPEAK),
                Map.entry("telegram", TELEGRAM),
                Map.entry("threema", THREEMA),
                Map.entry("twitter", TWITTER),
                Map.entry("whatsapp", WHATSAPP),
                Map.entry("youtube", YOUTUBE),
                Map.entry("xmpp", XMPP)
        );
    }

    /**
     * the contact detail type icon
     */
    @DrawableRes
    private final int icon;

    /**
     * The action to be performed when tapping a contact detail of this type.
     * @see Actions
     * @see ContactDetail#getValue()
     */
    private final @Nullable BiConsumer<Context, String> action;

    /**
     * The {@linkplain CommonDataKinds.Im#PROTOCOL im protocol} of this contact detail type
     * used when exporting contact details. When {@code null}, the {@linkplain ContactDetail#getLabel() label} is
     * used as a {@linkplain CommonDataKinds.Im#PROTOCOL_CUSTOM custom im protocol}.
     */
    private final @Nullable Integer imProtocol;

    @NonNull
    @SuppressWarnings("DataFlowIssue")
    public static ContactDetailType byLabel(@NonNull String label) {
        return LABEL.getOrDefault(label.toLowerCase(Locale.ROOT), UNKNOWN);
    }
}
