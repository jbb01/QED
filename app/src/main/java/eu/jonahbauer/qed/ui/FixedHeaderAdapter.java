package eu.jonahbauer.qed.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.R;

import java.util.*;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Needs to be set as ListAdapter AND as OnScrollListener to work properly
 *
 * The layout used should contain a view with id 'header'.
 *
 * Adding items to this adapter is very costly and should therefore be kept to a minimum.
 *
 * @param <C> the content type
 * @param <H> the header type
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class FixedHeaderAdapter<C, H> extends ArrayAdapter<C> implements SectionIndexer, AbsListView.OnScrollListener {
    protected final @NonNull List<C> mItemList;

    private @Nullable Function<C, H> mHeaderMap;
    private @Nullable Comparator<? super C> mComparator;

    // cache
    private H[] mSections;
    private int[] mSectionForPosition;
    private int[] mPositionForSection;

    private final @NonNull IntSet mHeaderPositions;

    private boolean mNotifyOnChange;

    private final @NonNull View mFixedHeader;
    private final @NonNull Set<View> mInvisibleViews;
    private final @NonNull LayoutInflater mLayoutInflater;

    /**
     * Creates a new fixed header adapter.
     * <p>
     *     For this adapter to work properly all items with the same header should be together as
     *     a group when sorted.
     * </p>
     * <p>
     *     The {@code headerMap} should fulfill the following conditions:
     *     <ul>
     *         <li>{@code null} is returned for no value in this adapter.</li>
     *         <li>The output for a specific argument should stay the same over the whole lifetime of this adapter.</li>
     *         <li>If two outputs are equal then they should be the same object.</li>
     *         <li>
     *             The {@code headerMap} should be compatible with the {@code comparator},
     *             i.e. when the same output is returned for two values then the same output should
     *             be returned for any value between the two.
     *         </li>
     *     </ul>
     * </p>
     *
     * @param context a context for view creation
     * @param itemList a list of items
     * @param fixedHeader a view used for the fixed header
     */
    public FixedHeaderAdapter(@NonNull Context context, @NonNull List<C> itemList, @NonNull View fixedHeader) {
        super(context, 0, itemList);

        this.mItemList = Objects.requireNonNull(itemList);
        this.mFixedHeader = Objects.requireNonNull(fixedHeader);

        this.mHeaderPositions = new IntOpenHashSet();
        this.mInvisibleViews = new HashSet<>();
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mNotifyOnChange = true;

        if (!itemList.isEmpty()) prepareHeaders();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        C item = getItem(position);

        assert item != null;

        boolean isHeader = mHeaderPositions.contains(position);
        H header = isHeader ? getSections()[getSectionForPosition(position)] : null;

        View view = getItemView(item, convertView, parent, mLayoutInflater);
        if (isHeader) setHeader(view, header);
        return view;
    }

    /**
     * This method is called to create a view corresponding to the given object.
     * The view should be configured as if it is no header
     *
     * @param object the item the view should be created for
     * @param convertView a view that should be reused
     * @return a view corresponding the the given object
     */
    @NonNull
    protected abstract View getItemView(C object, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater);

    /**
     * Adds the given header to the given view.
     * <p>
     *     If a view is determined to be a header then it will be passed to this function after being
     *     created in {@link #getItemView(Object, View, ViewGroup, LayoutInflater)}.
     * </p>
     *
     * @param view a view as returned by {@link #getItemView}
     * @param header the header that should be integrated into the view
     */
    protected abstract void setHeader(@NonNull View view, H header);

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(@Nullable C object) {
        if (object != null) {
            mItemList.add(object);
            prepareHeaders();
            if (mNotifyOnChange) notifyDataSetChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAll(@NonNull Collection<? extends C> collection) {
        mItemList.addAll(collection);
        prepareHeaders();
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(@NonNull Collection<? extends C> collection) {
        mItemList.removeAll(collection);
        prepareHeaders();
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        mItemList.clear();
        prepareHeaders();
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Sorts content and calculate positions which have headers
     */
    private void prepareHeaders() {
        if (mHeaderMap == null || mComparator == null) throw new IllegalStateException("not yet initialized");

        int size = this.mItemList.size();
        this.mItemList.sort(this.mComparator);

        this.mHeaderPositions.clear();
        List<H> sectionsList = new LinkedList<>();
        this.mSections = null;
        IntList positionForSectionList = new IntArrayList();
        this.mPositionForSection = null;
        this.mSectionForPosition = new int[size];

        H lastHeader = null;

        Iterator<C> items = this.mItemList.iterator();
        int sectionIndex = -1;
        for (int i = 0; i < size; i++) {
            C item = items.next();
            H header = this.mHeaderMap.apply(item);

            if (!header.equals(lastHeader)) {
                sectionIndex++;
                lastHeader = header;

                this.mHeaderPositions.add(i);
                positionForSectionList.add(i);
                sectionsList.add(header);
            }

            this.mSectionForPosition[i] = sectionIndex;
        }

        //noinspection unchecked
        this.mSections = (H[]) sectionsList.toArray();
        this.mPositionForSection = positionForSectionList.intStream().toArray();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount > 0) {
            View firstView = getViewByPosition(firstVisibleItem, view);

            setHeader(mFixedHeader, getSections()[getSectionForPosition(firstVisibleItem)]);

            mInvisibleViews.forEach(invisibleView -> invisibleView.setVisibility(View.VISIBLE));
            if (firstView.getY() < 0) {
                mInvisibleViews.add(firstView.findViewById(R.id.header));
                firstView.findViewById(R.id.header).setVisibility(View.INVISIBLE);
            }

            // Translate fixed header according to first item
            if (firstVisibleItem + 1 < mItemList.size()) {
                if (getSectionForPosition(firstVisibleItem) != getSectionForPosition(firstVisibleItem + 1)) {
                    mFixedHeader.setTranslationY(firstView.getY());
                } else {
                    mFixedHeader.setTranslationY(0);
                }
            } else {
                mFixedHeader.setTranslationY(0);
            }

            if (firstVisibleItem == 0 && firstView.getY() == 0) {
                mFixedHeader.setVisibility(View.GONE);
            } else {
                mFixedHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public H[] getSections() {
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

    @Override
    public void setNotifyOnChange(boolean notifyOnChange) {
        this.mNotifyOnChange = notifyOnChange;
    }

    protected void setComparator(@NonNull Comparator<? super C> comparator) {
        this.mComparator = Objects.requireNonNull(comparator);
    }

    /**
     * Sets the header map for this adapter.
     * <p>
     *     The {@code headerMap} should fulfill the following conditions:
     *     <ul>
     *         <li>{@code null} is returned for no value in this adapter.</li>
     *         <li>The output for a specific argument should stay the same over the whole lifetime of this adapter.</li>
     *         <li>If two outputs are equal then they should be the same object.</li>
     *         <li>
     *             The {@code headerMap} should be compatible with the {@code comparator},
     *             i.e. when the same output is returned for two values then the same output should
     *             be returned for any value between the two.
     *         </li>
     *     </ul>
     * </p>
     */
    protected void setHeaderMap(@NonNull Function<C, H> headerMap) {
        this.mHeaderMap = Objects.requireNonNull(headerMap);
    }

    public @NonNull List<C> getItemList() {
        return mItemList;
    }

    private static View getViewByPosition(int pos, AbsListView listView) {
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
