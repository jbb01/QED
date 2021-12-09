package com.jonahbauer.qed.activities.sheets.album;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.BindingAdapter;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoAlbumBinding;
import com.jonahbauer.qed.databinding.ListItemBinding;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;

public class AlbumInfoFragment extends InfoFragment {
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
        mAlbumViewModel = getViewModelProvider().get(AlbumViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoAlbumBinding.inflate(inflater, container, false);
        mAlbumViewModel.getAlbum().observe(getViewLifecycleOwner(), albumStatusWrapper -> {
            mBinding.setAlbum(albumStatusWrapper.getValue());
            mBinding.setLoading(albumStatusWrapper.getCode() == StatusWrapper.STATUS_PRELOADED);
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
        return Themes.colorful(getAlbum().getId());
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
    }

    @BindingAdapter("album_categories")
    public static void bindCategories(ViewGroup parent, Collection<String> categories) {
        Context context = parent.getContext();
        parent.removeAllViews();
        categories.forEach((category) -> {
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_album_category));
            String title = category;
            try {
                title = URLDecoder.decode(category, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {}
            item.setTitle(title);
        });
    }

    @BindingAdapter("album_persons")
    public static void bindPersons(ViewGroup parent, Collection<Person> persons) {
        Context context = parent.getContext();
        parent.removeAllViews();
        persons.forEach((person) -> {
            ListItemBinding item = ListItemBinding.inflate(LayoutInflater.from(context), parent, true);
            item.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_album_person));
            item.setTitle(person.getUsername());
        });
    }
}
