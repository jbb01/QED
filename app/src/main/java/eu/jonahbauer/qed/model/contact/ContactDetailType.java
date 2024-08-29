package eu.jonahbauer.qed.model.contact;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.Actions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactDetailType {
    PHONE(R.drawable.ic_person_contact_phone, Actions::dial),
    DISCORD(R.drawable.ic_person_contact_discord, null),
    MAIL(R.drawable.ic_person_contact_mail, Actions::sendTo),
    FACEBOOK(R.drawable.ic_person_contact_facebook, Actions::openFacebook),
    GITHUB(R.drawable.ic_person_contact_github, Actions::openGithub),
    GPG(R.drawable.ic_person_contact_gpg, null),
    ICQ(R.drawable.ic_person_contact_icq, null),
    INSTAGRAM(R.drawable.ic_person_contact_instagram, Actions::openInstagram),
    IRC(R.drawable.ic_person_contact_irc, null),
    JABBER(R.drawable.ic_person_contact_jabber, Actions::openXmpp),
    MATRIX(R.drawable.ic_person_contact_matrix, Actions::openMatrix),
    MUMBLE(R.drawable.ic_person_contact_mumble, null),
    PHOEN(R.drawable.ic_person_contact_phoen, Actions::dial),
    SIGNAL(R.drawable.ic_person_contact_signal, null),
    SKYPE(R.drawable.ic_person_contact_skype, Actions::openSkype),
    TEAMSPEAK(R.drawable.ic_person_contact_teamspeak, null),
    TELEGRAM(R.drawable.ic_person_contact_telegram, Actions::openTelegram),
    THREEMA(R.drawable.ic_person_contact_threema, Actions::openThreema),
    TWITTER(R.drawable.ic_person_contact_twitter, Actions::openTwitter),
    WHATSAPP(R.drawable.ic_person_contact_whatsapp, Actions::openWhatsapp),
    YOUTUBE(R.drawable.ic_person_contact_youtube, null),
    XMPP(R.drawable.ic_person_contact_xmpp, Actions::openXmpp),
    UNKNOWN(R.drawable.ic_person_contact, null),
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

    @DrawableRes
    private final int icon;

    @Nullable
    private final BiConsumer<Context, String> action;


    @NonNull
    @SuppressWarnings("DataFlowIssue")
    public static ContactDetailType byLabel(@NonNull String label) {
        return LABEL.getOrDefault(label.toLowerCase(Locale.ROOT), UNKNOWN);
    }
}
