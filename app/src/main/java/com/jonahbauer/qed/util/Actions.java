package com.jonahbauer.qed.util;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.NetworkConstants;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("UnusedReturnValue")
public class Actions {
    public static boolean sendTo(@NonNull Context context, String...emails) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        return tryStartActivity(context, intent);
    }

    public static boolean showOnMap(@NonNull Context context, String location) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + location));
        return tryStartActivity(context, intent);
    }

    public static boolean recordToCalendar(@NonNull Context context, @NonNull Event event) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                .putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        ZonedDateTime.of(event.getStart(), LocalTime.MIN, NetworkConstants.SERVER_TIME_ZONE).toInstant().toEpochMilli()
                )
                .putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        ZonedDateTime.of(event.getEnd(), LocalTime.MAX, NetworkConstants.SERVER_TIME_ZONE).toInstant().toEpochMilli()
                )
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getHotelAddress())
                .putExtra(Intent.EXTRA_EMAIL, event.getEmailOrga());

        return tryStartActivity(context, intent);
    }

    public static boolean dial(@NonNull Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        return tryStartActivity(context, intent);
    }

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

    public static boolean open(@NonNull Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        return tryStartActivity(context, intent);
    }

    public static boolean openFacebook(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://www.facebook.com/%s/", username);
        return open(context, uri);
    }

    public static boolean openGithub(@NonNull Context context, String username) {
        String uri = String.format("https://github.com/%s/", username);
        return open(context, uri);
    }

    public static boolean openInstagram(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://www.instagram.com/%s", username);
        return open(context, uri);
    }

    public static boolean openMatrix(@NonNull Context context, String username) {
        if (username.matches("@.+:(?:[^.]+\\.)+[^.]+")) {
            String uri = String.format("https://matrix.to/#/%s", username);
            return open(context, uri);
        } else {
            return false;
        }
    }

    public static boolean openSkype(@NonNull Context context, String username) {
        String uri = String.format("skype:%s?chat", username);
        return open(context, uri);
    }

    public static boolean openTelegram(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://t.me/%s", username);
        return open(context, uri);
    }

    public static boolean openThreema(@NonNull Context context, String id) {
        String uri = String.format("https://threema.id/%s", id);
        return open(context, uri);
    }

    public static boolean openTwitter(@NonNull Context context, String username) {
        username = (username.startsWith("@") ? username.substring(1) : username);
        String uri = String.format("https://twitter.com/%s", username);
        return open(context, uri);
    }

    public static boolean openWhatsapp(@NonNull Context context, String number) {
        number = number.replaceAll("[^0-9]", "");
        String uri = String.format("https://wa.me/%s", number);
        return open(context, uri);
    }

    public static boolean openXmpp(@NonNull Context context, String username) {
        String uri = String.format("xmpp:%s", username);
        return open(context, uri);
    }

    public static EventInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Event event) {
        EventInfoBottomSheet sheet = EventInfoBottomSheet.newInstance(event);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    public static EventInfoBottomSheet showInfoSheet(@NonNull FragmentActivity activity, @NonNull Event event, @Nullable String tag) {
        EventInfoBottomSheet sheet = EventInfoBottomSheet.newInstance(event);
        sheet.show(activity.getSupportFragmentManager(), tag);
        return sheet;
    }

    public static PersonInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Person person) {
        PersonInfoBottomSheet sheet = PersonInfoBottomSheet.newInstance(person);
        sheet.show(fragment.getParentFragmentManager(), sheet.getTag());
        return sheet;
    }

    public static PersonInfoBottomSheet showInfoSheet(@NonNull FragmentActivity activity, @NonNull Person person, @Nullable String tag) {
        PersonInfoBottomSheet sheet = PersonInfoBottomSheet.newInstance(person);
        sheet.show(activity.getSupportFragmentManager(), tag);
        return sheet;
    }

    public static MessageInfoBottomSheet showInfoSheet(@NonNull Fragment fragment, @NonNull Message message) {
        MessageInfoBottomSheet sheet = MessageInfoBottomSheet.newInstance(message);
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
