package com.jonahbauer.qed.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;

import com.jonahbauer.qed.model.Event;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Intents {
    public static boolean sendTo(Context context, String...emails) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_EMAIL, emails);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static boolean showOnMap(Context context, String location) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + location));

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static boolean recordToCalendar(Context context, Event event) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getStart().getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getEnd().getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getHotelAddress())
                .putExtra(Intent.EXTRA_EMAIL, event.getEmailOrga());

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static boolean dial(Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }
}
