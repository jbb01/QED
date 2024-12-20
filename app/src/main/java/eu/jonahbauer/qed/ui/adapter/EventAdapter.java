package eu.jonahbauer.qed.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.ui.themes.Theme;
import eu.jonahbauer.qed.ui.views.MaterialListItem;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.util.TextUtils;
import eu.jonahbauer.qed.util.TimeUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.*;
import java.util.function.Function;

public class EventAdapter extends ArrayAdapter<Event> implements SectionIndexer {
    private final List<Event> mEventList;

    public EventAdapter(Context context, List<Event> eventList) {
        super(context, 0, eventList);
        this.mEventList = eventList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        var context = getContext();
        final Event event = mEventList.get(position);

        MaterialListItem item;
        if (convertView instanceof MaterialListItem) {
            item = (MaterialListItem) convertView;
        } else {
            item = new MaterialListItem(context);
            item.setIcon(R.drawable.ic_event_icon);
        }

        String subtitle = TextUtils.formatRange(context, TimeUtils.PARSED_LOCAL_DATE_FORMATTER, event.getStart(), event.getEnd());

        item.setTitle(event.getTitle());
        item.setSubtitle(subtitle);
        item.setIconTint(Theme.getCurrentTheme().getIconColor(context, event.getId()));
        item.setTransitionName(context.getString(R.string.transition_name_event, event.getId()));

        return item;
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
        var start = event.getStart();
        if (start == null) return "0000";
        var startDate = start.getLocalDate();
        if (startDate == null) return "0000";
        return String.valueOf(startDate.getYear());
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


    @Nullable
    @Override
    public Event getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : Event.NO_ID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}