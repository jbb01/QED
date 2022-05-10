package com.jonahbauer.qed.util;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.event.EventInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.message.MessageInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.person.PersonInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.registration.RegistrationInfoBottomSheet;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.NetworkConstants;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"UnusedReturnValue", "JavadocLinkAsPlainText"})
public class Actions {
    public static boolean sendTo(@NonNull Context context, String...emails) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        return tryStartActivity(context, intent);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code geo:0,0?g=${location}}
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean showOnMap(@NonNull Context context, String location) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + location));
        return tryStartActivity(context, intent);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_INSERT insert} the given event into the calendar.
     * The following attributes are included:
     * <ul>
     *     <li>the {@linkplain Event#getTitle() event title}</li>
     *     <li>
     *         the {@linkplain Event#getStart() begin time} converted from
     *         {@linkplain NetworkConstants#SERVER_TIME_ZONE server time zone} to epoch millis
     *     </li>
     *     <li>
     *         the {@linkplain Event#getEnd() end time} converted from
     *         {@linkplain NetworkConstants#SERVER_TIME_ZONE server time zone} to epoch millis
     *     </li>
     *     <li>the {@linkplain Event#getHotelAddress() location}</li>
     *     <li>the {@linkplain Event#getEmailOrga() organizer mail address}.</li>
     * </ul>
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean recordToCalendar(@NonNull Context context, @NonNull Event event) {
        var parsedStart = event.getStart();
        var parsedEnd = event.getEnd();
        var start = parsedStart != null ? parsedStart.getLocalDate() : null;
        var end = parsedEnd != null ? parsedEnd.getLocalDate() : null;
        if (start == null || end == null) return false;

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                .putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        ZonedDateTime.of(start, LocalTime.MIN, NetworkConstants.SERVER_TIME_ZONE).toInstant().toEpochMilli()
                )
                .putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        ZonedDateTime.of(end, LocalTime.MAX, NetworkConstants.SERVER_TIME_ZONE).toInstant().toEpochMilli()
                )
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getHotelAddress())
                .putExtra(Intent.EXTRA_EMAIL, event.getEmailOrga());

        return tryStartActivity(context, intent);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_DIAL dial} {@code tel:${phoneNumber}}.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean dial(@NonNull Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        return tryStartActivity(context, intent);
    }

    /**
     * Copies the given text to the clipboard.
     * @param context a context
     * @param view a view for showing a snackbar
     * @param label the clipboard label
     * @param text the text
     * @return {@code true} for consistency with other actions
     */
    public static boolean copy(@NonNull Context context, @Nullable View view, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        if (view != null) {
            var snackbar = Snackbar.make(view, R.string.copied, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
        return true;
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view} the given url
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean open(@NonNull Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        return tryStartActivity(context, intent);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view} the given uri granting read permission to
     * the receiving activity.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openContent(@NonNull Context context, Uri uri, String type) {
        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, type);

        var infos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (var info : infos) {
            var packageName = info.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return tryStartActivity(context, intent);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://www.facebook.com/${username}/}
     * <br>
     * If {@code username} with an @ sign it will not be included in the URI.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openFacebook(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://www.facebook.com/%s/", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://www.github.com/${username}/}
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openGithub(@NonNull Context context, String username) {
        String uri = String.format("https://github.com/%s/", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://www.instagram.com/${username}/}
     * <br>
     * If {@code username} with an @ sign it will not be included in the URI.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openInstagram(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://www.instagram.com/%s", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://matrix.to/#/${username}}
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openMatrix(@NonNull Context context, String username) {
        String uri = String.format("https://matrix.to/#/%s", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code skype:${username}?chat}
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openSkype(@NonNull Context context, String username) {
        String uri = String.format("skype:%s?chat", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://t.me/${username}}
     * <br>
     * If {@code username} with an @ sign it will not be included in the URI.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openTelegram(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://t.me/%s", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://threema.id/${username}}
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openThreema(@NonNull Context context, String id) {
        String uri = String.format("https://threema.id/%s", id);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://twitter.com/${username}}
     * <br>
     * If {@code username} with an @ sign it will not be included in the URI.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openTwitter(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://twitter.com/%s", username);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code https://wa.me/${number}}.
     * <br>
     * Before composing the URI all non digit characters of the number are stripped.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openWhatsapp(@NonNull Context context, String number) {
        number = number.replaceAll("\\D", "");
        String uri = String.format("https://wa.me/%s", number);
        return open(context, uri);
    }

    /**
     * Launches an {@link Intent} to {@linkplain Intent#ACTION_VIEW view}
     * {@code xmpp:${number}}.
     * @return {@code true} when a suitable activity was found and started
     */
    public static boolean openXmpp(@NonNull Context context, String username) {
        String uri = String.format("xmpp:%s", username);
        return open(context, uri);
    }

    /**
     * Shows an {@link EventInfoBottomSheet} using the
     * {@linkplain Fragment#getParentFragmentManager() parent fragment manager} of the given fragment.
     */
    public static EventInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Event event) {
        var sheet = EventInfoBottomSheet.newInstance(event);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    /**
     * Shows an {@link EventInfoBottomSheet} using the
     * {@linkplain FragmentActivity#getSupportFragmentManager() support fragment manager} of the given activity.
     */
    public static EventInfoBottomSheet showInfoSheet(@NonNull FragmentActivity activity, @NonNull Event event, @Nullable String tag) {
        var sheet = EventInfoBottomSheet.newInstance(event);
        sheet.show(activity.getSupportFragmentManager(), tag);
        return sheet;
    }

    /**
     * Shows a {@link PersonInfoBottomSheet} using the
     * {@linkplain Fragment#getParentFragmentManager() parent fragment manager} of the given fragment.
     */
    public static PersonInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Person person) {
        var sheet = PersonInfoBottomSheet.newInstance(person);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    /**
     * Shows an {@link PersonInfoBottomSheet} using the
     * {@linkplain FragmentActivity#getSupportFragmentManager() support fragment manager} of the given activity.
     */
    public static PersonInfoBottomSheet showInfoSheet(@NonNull FragmentActivity activity, @NonNull Person person, @Nullable String tag) {
        var sheet = PersonInfoBottomSheet.newInstance(person);
        sheet.show(activity.getSupportFragmentManager(), tag);
        return sheet;
    }

    /**
     * Shows a {@link RegistrationInfoBottomSheet} using the
     * {@linkplain Fragment#getParentFragmentManager() parent fragment manager} of the given fragment.
     */
    public static RegistrationInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Registration registration) {
        var sheet = RegistrationInfoBottomSheet.newInstance(registration);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    /**
     * Shows an {@link RegistrationInfoBottomSheet} using the
     * {@linkplain FragmentActivity#getSupportFragmentManager() support fragment manager} of the given activity.
     */
    public static RegistrationInfoBottomSheet showInfoSheet(@NonNull FragmentActivity activity, @NonNull Registration registration, @Nullable String tag) {
        var sheet = RegistrationInfoBottomSheet.newInstance(registration);
        sheet.show(activity.getSupportFragmentManager(), tag);
        return sheet;
    }

    /**
     * Shows a {@link MessageInfoBottomSheet} using the
     * {@linkplain Fragment#getParentFragmentManager() parent fragment manager} of the given fragment.
     */
    public static MessageInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Message message) {
        var sheet = MessageInfoBottomSheet.newInstance(message);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    private static boolean tryStartActivity(@NonNull Context context, Intent intent) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }
}
