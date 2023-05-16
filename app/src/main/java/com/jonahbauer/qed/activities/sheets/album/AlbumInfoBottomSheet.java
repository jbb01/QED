package com.jonahbauer.qed.activities.sheets.album;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.model.viewmodel.InfoViewModel;

public class AlbumInfoBottomSheet extends InfoBottomSheet {
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
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mAlbumViewModel = getViewModelProvider().get(AlbumViewModel.class);
        mAlbumViewModel.load(album);
    }

    @Override
    public long getDesignSeed() {
        return getAlbum().getId();
    }

    @Override
    public @NonNull InfoFragment createFragment() {
        return AlbumInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mAlbumViewModel;
    }

    @NonNull
    private Album getAlbum() {
        return mAlbumViewModel.getAlbumValue();
    }
}
