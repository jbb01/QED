package com.jonahbauer.qed.qeddb.event;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class EventAdapter extends ArrayAdapter<Event> {
    private final List<Event> eventList;
    private LayoutInflater inflater;

    public EventAdapter(Context context, List<Event> eventList) {
        super(context, R.layout.list_item_event, eventList);
        this.eventList = eventList;

        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Event event = eventList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_event, parent, false);
        }

        ImageView eventIcon = view.findViewById(R.id.event_icon);
        switch (event.name.chars().sum()%10) {
            case 0:
                eventIcon.setColorFilter(Color.argb(255, 0x33, 0xb5, 0xe5));
                break;
            case 1:
                eventIcon.setColorFilter(Color.argb(255, 0x99, 0xcc, 0x00));
                break;
            case 2:
                eventIcon.setColorFilter(Color.argb(255, 0xff, 0x44, 0x44));
                break;
            case 3:
                eventIcon.setColorFilter(Color.argb(255, 0x00, 0x99, 0xcc));
                break;
            case 4:
                eventIcon.setColorFilter(Color.argb(255, 0x66, 0x99, 0x00));
                break;
            case 5:
                eventIcon.setColorFilter(Color.argb(255, 0xcc, 0x00, 0x00));
                break;
            case 6:
                eventIcon.setColorFilter(Color.argb(255, 0xaa, 0x66, 0xcc));
                break;
            case 7:
                eventIcon.setColorFilter(Color.argb(255, 0xff, 0xbb, 0x33));
                break;
            case 8:
                eventIcon.setColorFilter(Color.argb(255, 0xff, 0x88, 0x00));
                break;
            case 9:
                eventIcon.setColorFilter(Color.argb(255, 0x00, 0xdd, 0xff));
                break;
        }

        String time;
        if (event.start != null && event.end != null)
            time = MessageFormat.format("{0,date,dd.MM.yyyy} - {1,date,dd.MM.yyyy}", event.start, event.end);
        else
            time = event.startString + " - " + event.endString;

        String name = event.name;
        ((TextView)view.findViewById(R.id.event_name)).setText(name);
        ((TextView)view.findViewById(R.id.event_time)).setText(time);

        return view;
    }

    @Override
    public void addAll(@NonNull Collection<? extends Event> collection) {
        eventList.addAll(collection);
        eventList.sort(Comparator.reverseOrder());
    }

    @SuppressWarnings("unused")
    public void add(int index, Event event) {
        eventList.add(index, event);
        eventList.sort(Comparator.reverseOrder());
    }
}