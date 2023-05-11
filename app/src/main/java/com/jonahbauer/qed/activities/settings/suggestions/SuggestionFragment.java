package com.jonahbauer.qed.activities.settings.suggestions;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentPreferenceSuggestionsBinding;
import com.jonahbauer.qed.databinding.ListItemSuggestionBinding;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.ViewUtils;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public abstract class SuggestionFragment extends Fragment {
    private static final int SNACKBAR_DURATION_MILLIS = 2750;
    private final @PluralsRes int mDeletedMessage;
    private final @StringRes int mClipLabel;

    private FragmentPreferenceSuggestionsBinding mBinding;
    private ItemAdapter mItemAdapter;

    private @Nullable Set<String> mOldValues;
    private @Nullable Instant mOldValuesTimestamp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentPreferenceSuggestionsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mItemAdapter = new ItemAdapter(requireContext());
        submitList(getValues());

        mBinding.list.setAdapter(mItemAdapter);
        mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    protected abstract @NonNull Set<String> getValues();

    protected abstract void setValues(@NonNull Set<String> values);

    protected abstract @NonNull ViewUtils.ChipItem createChip(@NonNull String value);


    private void submitList(@NonNull Collection<String> values) {
        var chips = Arrays.asList(new ViewUtils.ChipItem[values.size()]);

        int position = values.size();
        for (var value : values) {
            chips.set(--position, createChip(value));
        }

        mItemAdapter.submitList(chips);
    }

    private void update(@NonNull Set<String> values) {
        setValues(values);
        submitList(values);
    }

    private void delete(@NonNull String value) {
        // delete from preferences and view
        var oldValues = getValues();
        var newValues = new LinkedHashSet<>(oldValues);
        if (!newValues.remove(value)) return;
        update(newValues);

        // debounce old values
        var now = Instant.now();
        if (mOldValues == null || mOldValuesTimestamp == null || Duration.between(mOldValuesTimestamp, now).toMillis() > SNACKBAR_DURATION_MILLIS) {
            mOldValues = oldValues;
        }
        mOldValuesTimestamp = now;

        // show snackbar
        var count = mOldValues.size() - newValues.size();
        var message = getResources().getQuantityString(mDeletedMessage, count, count);
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> update(mOldValues))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mItemAdapter = null;
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final @NonNull ListItemSuggestionBinding mBinding;
        private ViewUtils.ChipItem mItem;

        public ItemViewHolder(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent) {
            this(ListItemSuggestionBinding.inflate(inflater, parent, false));
        }

        private ItemViewHolder(@NonNull ListItemSuggestionBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            this.mBinding.getRoot().setOnLongClickListener(this);
            this.mBinding.deleteButton.setOnClickListener(this);
        }

        public void setItem(@NonNull ViewUtils.ChipItem item) {
            this.mItem = item;
            this.mBinding.setTitle(item.getLabel());
        }

        @Override
        public void onClick(View v) {
            if (mItem != null) {
                delete(mItem.getValue());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return Actions.copy(requireContext(), requireView(), getString(mClipLabel), mItem.getValue());
        }
    }

    private class ItemAdapter extends ListAdapter<ViewUtils.ChipItem, ItemViewHolder> {
        private final @NonNull LayoutInflater mLayoutInflater;

        public ItemAdapter(@NonNull Context context) {
            super(ChipItemCallback.INSTANCE);
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ItemViewHolder(mLayoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            var item = getItem(position);
            holder.setItem(item);
        }
    }

    private static class ChipItemCallback extends DiffUtil.ItemCallback<ViewUtils.ChipItem> {
        private static final ChipItemCallback INSTANCE = new ChipItemCallback();

        @Override
        public boolean areItemsTheSame(@NonNull ViewUtils.ChipItem oldItem, @NonNull ViewUtils.ChipItem newItem) {
            return Objects.equals(oldItem.getValue(), newItem.getValue());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ViewUtils.ChipItem oldItem, @NonNull ViewUtils.ChipItem newItem) {
            return Objects.equals(oldItem.getLabel(), newItem.getLabel());
        }
    }
}
