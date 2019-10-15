package com.jonahbauer.qed.layoutStuff;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import com.jonahbauer.qed.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class FixedHeaderAdapter<T, S> extends ArrayAdapter<T> implements SectionIndexer, AbsListView.OnScrollListener {
    protected List<T> itemList;
    private Function<T,S> headerMap;
    private Comparator<? super T> comparator;
    private Object[] sections;

    private boolean notifyOnChange;
    private List<Integer> headerPositions;

    private View fixedHeader;
    private Set<View> invisibleViews;

    private LayoutInflater layoutInflater;

    public FixedHeaderAdapter(Context context, @NonNull List<T> itemList, @NonNull Function<T,S> headerMap, Comparator<? super T> comparator, View fixedHeader) {
        super(context, 0, itemList);
        this.itemList = itemList;
        setComparator(comparator);
        this.headerMap = headerMap;
        headerPositions = new ArrayList<>();
        invisibleViews = new HashSet<>();
        layoutInflater = LayoutInflater.from(context);
        this.fixedHeader = fixedHeader;
        notifyOnChange = true;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        T item = getItem(position);

        assert item != null;

        boolean isHeader = headerPositions.contains(position);
        S header = isHeader ? (S) getSections()[getSectionForPosition(position)] : null;

        View view = getItemView(item, convertView, parent, layoutInflater);
        if (isHeader) setHeader(view, header);
        return view;
    }

    @NonNull
    protected abstract View getItemView(T object, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater);

    protected abstract void setHeader(@NonNull View view, S header);

    public void add(@Nullable T object) {
        if (object != null) {
            itemList.add(object);
            prepareHeaders();
            if (notifyOnChange) notifyDataSetChanged();
        }
    }

    public void addAll(@NonNull Collection<? extends T> collection) {
        itemList.addAll(collection);
        prepareHeaders();
        if (notifyOnChange) notifyDataSetChanged();
    }

    public void removeAll(@NonNull Collection<? extends T> collection) {
        itemList.removeAll(collection);
        prepareHeaders();
        if (notifyOnChange) notifyDataSetChanged();
    }

    public void clear() {
        itemList.clear();
        prepareHeaders();
        if (notifyOnChange) notifyDataSetChanged();
    }

    private void prepareHeaders() {
        sections = null;
        headerPositions.clear();
        itemList.sort(comparator);

        S lastHeader = null;
        for (int i = 0; i < itemList.size(); i++) {
            if (!headerMap.apply(itemList.get(i)).equals(lastHeader)) {
                lastHeader = headerMap.apply(itemList.get(i));
                headerPositions.add(i);
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    @SuppressWarnings("unchecked")
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount > 0) {
            View firstView = getViewByPosition(firstVisibleItem, view);

            setHeader(fixedHeader, (S) getSections()[getSectionForPosition(firstVisibleItem)]);

            invisibleViews.forEach(invisibleView -> invisibleView.setVisibility(View.VISIBLE));
            if (firstView.getY() < 0) {
                invisibleViews.add(firstView.findViewById(R.id.header));
                firstView.findViewById(R.id.header).setVisibility(View.INVISIBLE);
            }

            // Translate fixed header according to first item
            if (firstVisibleItem + 1 < itemList.size()) {
                if (getSectionForPosition(firstVisibleItem) != getSectionForPosition(firstVisibleItem + 1)) {
                    fixedHeader.setTranslationY(firstView.getY());
                } else {
                    fixedHeader.setTranslationY(0);
                }
            } else {
                fixedHeader.setTranslationY(0);
            }
        }
    }

    @Override
    public Object[] getSections() {
        if (sections == null) sections = itemList.stream().sorted(comparator).map(item -> headerMap.apply(item)).distinct().toArray();
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        int headerIndex = 0;
        for (int i = 0; i < itemList.size(); i++) {
            if (headerPositions.contains(i)) headerIndex ++;
            if (headerIndex - 1 == sectionIndex) return i;
        }
        return itemList.size();
    }

    @Override
    public int getSectionForPosition(int position) {
        return Arrays.asList(getSections()).indexOf(headerMap.apply(itemList.get(position)));
    }

    @Override
    public void setNotifyOnChange(boolean notifyOnChange) {
        this.notifyOnChange = notifyOnChange;
    }

    public void setComparator(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    public void setHeaderMap(Function<T,S> headerMap) {
        this.headerMap = headerMap;
    }

    public List<T> getItemList() {
        return itemList;
    }

    private View getViewByPosition(int pos, AbsListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
}
