package com.jonahbauer.qed.activities.sheets.album;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoActivity;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class AlbumInfoActivity extends AbstractInfoActivity {
    public static final String ARGUMENT_ALBUM = "album";

    private AlbumViewModel mAlbumViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle args = getIntent().getExtras();
        assert args != null;

        Album album = args.getParcelable(ARGUMENT_ALBUM);
        if (album == null) {
            // TODO show toast
            finish();
            return;
        }

        mAlbumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        mAlbumViewModel.load(album, null);

        super.onCreate(savedInstanceState);
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
        assert album != null : "Album should not be null";
        return album;
    }
}
