package com.jonahbauer.qed.activities.sheets.album;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoAlbumBinding;
import com.jonahbauer.qed.layoutStuff.views.ListItem;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

public class AlbumInfoFragment extends InfoFragment {
    private static final String SAVED_TITLE_HIDDEN = "titleHidden";

    private AlbumViewModel mAlbumViewModel;
    private FragmentInfoAlbumBinding mBinding;

    private boolean mHideTitle;

    public static AlbumInfoFragment newInstance() {
        return new AlbumInfoFragment();
    }

    public AlbumInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbumViewModel = getViewModelProvider(0).get(AlbumViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoAlbumBinding.inflate(inflater, container, false);
        mAlbumViewModel.getAlbum().observe(getViewLifecycleOwner(), albumStatusWrapper -> {
            var value = albumStatusWrapper.getValue();
            var code = albumStatusWrapper.getCode();
            mBinding.setAlbum(value);
            mBinding.setLoading(code == StatusWrapper.STATUS_PRELOADED);
            mBinding.setError(code == StatusWrapper.STATUS_ERROR ? getString(R.string.error_incomplete) : null);
        });
        mBinding.setColor(getColor());
        if (mHideTitle) hideTitle();
        return mBinding.getRoot();
    }

    @NonNull
    private Album getAlbum() {
        StatusWrapper<Album> wrapper = mAlbumViewModel.getAlbum().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Album album = wrapper.getValue();
        assert album != null : "Album should not be null";
        return album;
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getAlbum().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getAlbum().getId());
    }

    @Override
    protected String getTitle() {
        return getAlbum().getName();
    }

    @Override
    protected float getTitleBottom() {
        return mBinding.title.getBottom();
    }

    @Override
    public void hideTitle() {
        mHideTitle = true;
        if (mBinding != null) {
            mBinding.titleLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SAVED_TITLE_HIDDEN)) hideTitle();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_TITLE_HIDDEN, mHideTitle);
    }

    @BindingAdapter("album_categories")
    public static void bindCategories(ViewGroup parent, Collection<String> categories) {
        Context context = parent.getContext();
        parent.removeAllViews();
        categories.forEach((category) -> {
            ListItem item = new ListItem(context);
            item.setIcon(R.drawable.ic_album_category);
            item.setTitle(Album.decodeCategory(category));
            parent.addView(item);
        });
    }

    @BindingAdapter("album_persons")
    public static void bindPersons(ViewGroup parent, Collection<Person> persons) {
        Context context = parent.getContext();
        parent.removeAllViews();
        persons.forEach((person) -> {
            ListItem item = new ListItem(context);
            item.setIcon(R.drawable.ic_album_person);
            item.setTitle(person.getUsername());
            parent.addView(item);
        });
    }
}
