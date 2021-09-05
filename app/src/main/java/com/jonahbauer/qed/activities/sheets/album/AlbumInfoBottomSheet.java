package com.jonahbauer.qed.activities.sheets.album;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class AlbumInfoBottomSheet extends AbstractInfoBottomSheet {
    private static final String ARGUMENT_ALBUM = "album";

    private AlbumViewModel mAlbumViewModel;

    @NonNull
    public static AlbumInfoBottomSheet newInstance(Album album) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_ALBUM, album);
        AlbumInfoBottomSheet sheet = new AlbumInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public AlbumInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;

        Album album = args.getParcelable(ARGUMENT_ALBUM);
        if (album == null) {
            // TODO show toast
            dismiss();
            return;
        }

        mAlbumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        mAlbumViewModel.init(album);
        mAlbumViewModel.load();
    }

    @Override
    public int getColor() {
        return Themes.colorful(getAlbum().getId());
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return AlbumInfoFragment.newInstance();
    }

    @NonNull
    private Album getAlbum() {
        StatusWrapper<Album> wrapper = mAlbumViewModel.getAlbum().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Album album = wrapper.getValue();
        assert album != null : "Event should not be null";
        return album;
    }
}
