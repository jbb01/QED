package com.jonahbauer.qed.activities.mainFragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.databinding.FragmentGalleryBinding;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.adapter.AlbumAdapter;
import com.jonahbauer.qed.model.viewmodel.AlbumListViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.ArrayList;

public class GalleryFragment extends QEDFragment implements AdapterView.OnItemClickListener {
    private AlbumAdapter mAlbumAdapter;
    private FragmentGalleryBinding mBinding;

    private AlbumListViewModel mAlbumListViewModel;

    public static GalleryFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_gallery);

        GalleryFragment fragment = new GalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentGalleryBinding.bind(view);
        mAlbumListViewModel = new ViewModelProvider(this).get(AlbumListViewModel.class);

        mAlbumAdapter = new AlbumAdapter(getContext(), new ArrayList<>());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setAdapter(mAlbumAdapter);

        mBinding.setOnOfflineClick(v -> {
            if (Preferences.gallery().isOfflineMode()) {
                Preferences.gallery().edit().setOfflineMode(false).apply();
            }
            mAlbumListViewModel.load();
        });

        mAlbumListViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            mBinding.setStatus(albums.getCode());

            mAlbumAdapter.clear();
            if (albums.getCode() == StatusWrapper.STATUS_LOADED) {
                mAlbumAdapter.addAll(albums.getValue());
            } else if (albums.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = albums.getReason();
                mBinding.setError(getString(reason == Reason.EMPTY ? R.string.gallery_empty : reason.getStringRes()));
            }
            mAlbumAdapter.notifyDataSetChanged();
        });

        mAlbumListViewModel.getOffline().observe(getViewLifecycleOwner(), offline -> {
            mBinding.setOffline(offline);

            mBinding.setForcedOfflineMode(Preferences.gallery().isOfflineMode());
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mAlbumListViewModel.load();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album album = mAlbumAdapter.getItem((int) id);

        if (album == null) return;
        if (isOnline() || album.getImageListDownloaded() != null) {
            Intent intent = new Intent(GalleryFragment.this.getContext(), GalleryAlbumActivity.class);
            intent.putExtra(GalleryAlbumActivity.GALLERY_ALBUM_KEY, album);
            startActivity(intent);
        } else {
            Snackbar.make(mBinding.getRoot(), getString(R.string.album_not_downloaded), Snackbar.LENGTH_SHORT).show();
        }
    }

    private boolean isOnline() {
        Boolean offline = mAlbumListViewModel.getOffline().getValue();
        return offline != null && !offline;
    }
}
