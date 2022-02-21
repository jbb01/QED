package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemEventBinding;
import com.jonahbauer.qed.model.Event;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

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

        ListItemEventBinding binding;
        if (convertView != null) {
            binding = (ListItemEventBinding) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            binding = ListItemEventBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
        }

        binding.setEvent(event);
        return binding.getRoot();
    }

    @Override
    public void addAll(@NonNull Collection<? extends Event> collection) {
        mEventList.addAll(collection);
        prepareHeaders();
    }

    public void add(int index, Event event) {
        mEventList.add(index, event);
        mEventList.sort(Comparator.reverseOrder());
    }

    //<editor-fold desc="Section Indexer">
    private String[] mSections;
    private int[] mSectionForPosition;
    private int[] mPositionForSection;

    private final Function<Event, String> mHeaderMap = event -> {
        if (event.getStart() != null)
            return String.valueOf(event.getStart().getYear());
        else
            return "0000";
    };

    private void prepareHeaders() {
        int size = this.mEventList.size();
        this.mEventList.sort(Comparator.reverseOrder());

        List<String> sectionsList = new LinkedList<>();
        this.mSections = null;
        IntList positionForSectionList = new IntArrayList();
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
        this.mPositionForSection = positionForSectionList.intStream().toArray();
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
    //</editor-fold>

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}