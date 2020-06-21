package com.jonahbauer.qed.qeddb.event;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class EventAdapter extends ArrayAdapter<Event> implements SectionIndexer {
    private final Context mContext;
    private final List<Event> mEventList;

    public EventAdapter(Context context, List<Event> eventList) {
        super(context, R.layout.list_item_event, eventList);
        this.mContext = context;
        this.mEventList = eventList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Event event = mEventList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_event, parent, false);
        }

        ImageView eventIcon = view.findViewById(R.id.event_icon);
        eventIcon.setColorFilter(Application.colorful(event.title.chars().sum()));

        String time;
        if (event.start != null && event.end != null)
            time = MessageFormat.format("{0,date,dd.MM.yyyy} - {1,date,dd.MM.yyyy}", event.start, event.end);
        else
            time = event.startString + " - " + event.endString;

        String name = event.title;
        ((TextView)view.findViewById(R.id.event_name)).setText(name);
        ((TextView)view.findViewById(R.id.event_time)).setText(time);

        return view;
    }

    @Override
    public void addAll(@NonNull Collection<? extends Event> collection) {
        mEventList.addAll(collection);
        prepareHeaders();
    }

    @SuppressWarnings("unused")
    public void add(int index, Event event) {
        mEventList.add(index, event);
        mEventList.sort(Comparator.reverseOrder());
    }



    /*
        Section Indexer Stuff
     */
    private String[] mSections;
    private int[] mSectionForPosition;
    private int[] mPositionForSection;

    private Function<Event, String> mHeaderMap = event -> {
        if (event.start != null)
            return String.format("%1$tY", event.start);
        else
            return "0000";
    };

    private void prepareHeaders() {
        int size = this.mEventList.size();
        this.mEventList.sort(Comparator.reverseOrder());

        List<String> sectionsList = new LinkedList<>();
        this.mSections = null;
        List<Integer> positionForSectionList = new LinkedList<>();
        this.mPositionForSection = null;
        this.mSectionForPosition = new int[size];

        String lastHeader = null;

        Iterator<Event> items = this.mEventList.iterator();
        int sectionIndex = -1;
        for (int i = 0; i < size; i++) {
            Event item = items.next();
            String header = this.mHeaderMap.apply(item);

            if (!header.equals(lastHeader)) {
                sectionIndex++;
                lastHeader = header;

                positionForSectionList.add(i);
                sectionsList.add(header);
            }

            this.mSectionForPosition[i] = sectionIndex;
        }

        this.mSections = sectionsList.toArray(new String[0]);
        this.mPositionForSection = positionForSectionList.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mPositionForSection[sectionIndex];
    }

    @Override
    public int getSectionForPosition(int position) {
        return mSectionForPosition[position];
    }
}