package com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet;

import android.util.SparseArray;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import java.util.List;

@SuppressWarnings("unused")
public abstract class ViewPagerBottomSheetFragmentStateAdapter extends FragmentStateAdapter {
    private final SparseArray<FragmentViewHolder> fragments = new SparseArray<>();

    public ViewPagerBottomSheetFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ViewPagerBottomSheetFragmentStateAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public ViewPagerBottomSheetFragmentStateAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @Override
    @CallSuper
    public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        fragments.put(position, holder);
    }

    public FragmentViewHolder getFragmentViewHolder(int position) {
        return fragments.get(position);
    }
}
