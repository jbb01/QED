package com.jonahbauer.qed.activities.mainFragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.adapter.AlbumAdapter;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.Preferences;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends QEDFragment implements QEDPageReceiver<List<Album>>, GalleryDatabaseReceiver {
    private ListView mGalleryListView;
    private ProgressBar mSearchProgress;
    private TextView mOfflineLabel;

    private boolean mOnline;

    private AlbumAdapter mGalleryAdapter;
    private GalleryDatabase mGalleryDatabase;


    public static GalleryFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_gallery);

        GalleryFragment fragment = new GalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOnline = false;

        mGalleryDatabase = new GalleryDatabase();
        mGalleryDatabase.init(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mGalleryAdapter = new AlbumAdapter(getContext(), new ArrayList<>());

        mGalleryListView = view.findViewById(R.id.gallery_list_container);
        mGalleryListView.setAdapter(mGalleryAdapter);
        mGalleryListView.setOnItemClickListener((parent, view1, position, id) -> {
            Album album = mGalleryAdapter.getItem((int) id);

            if (album == null) return;
            if (mOnline || album.isImageListDownloaded()) {
                Intent intent = new Intent(GalleryFragment.this.getContext(), GalleryAlbumActivity.class);
                intent.putExtra(GalleryAlbumActivity.GALLERY_ALBUM_KEY, album);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), getString(R.string.album_not_downloaded), Toast.LENGTH_SHORT).show();
            }
        });

        mSearchProgress = view.findViewById(R.id.search_progress);
        mOfflineLabel = view.findViewById(R.id.label_offline);

        if (!Preferences.gallery().isOfflineMode()) {
            mOfflineLabel.setOnClickListener(v -> switchToOnlineMode());
        } else {
            mOfflineLabel.setOnClickListener(null);
        }

        mGalleryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Preferences.gallery().isOfflineMode())
            switchToOnlineMode();
        else
            switchToOfflineMode();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGalleryDatabase.close();
    }

    @Override
    public void onPageReceived(@NonNull List<Album> albums) {
        mGalleryAdapter.clear();
        mGalleryAdapter.addAll(albums);
        mGalleryAdapter.notifyDataSetChanged();
        mSearchProgress.setVisibility(View.GONE);
        mGalleryListView.setVisibility(View.VISIBLE);

        mGalleryDatabase.insertAllAlbums(albums, false, this);
        mOnline = true;
    }

    @Override
    public void onError(List<Album> albums, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(albums, reason, cause);

        // TODO error handling

        if (Reason.NETWORK.equals(reason))
            switchToOfflineMode();
    }

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    private void switchToOfflineMode() {
        mOnline = false;
        mOfflineLabel.post(() -> {
            mOfflineLabel.setVisibility(View.VISIBLE);

            if (!Preferences.gallery().isOfflineMode())
                // offline mode was chosen as a last resort
                Toast.makeText(getContext(), mOfflineLabel.getContext().getString(R.string.login_failed_switching_to_offline), Toast.LENGTH_SHORT).show();
        });

        mGalleryListView.post(() -> {
            mGalleryAdapter.clear();
            mGalleryAdapter.addAll(mGalleryDatabase.getAlbums());
            mGalleryAdapter.notifyDataSetChanged();
            mGalleryListView.setVisibility(View.VISIBLE);
        });

        mSearchProgress.post(() -> mSearchProgress.setVisibility(View.GONE));
    }

    private void switchToOnlineMode() {
        mHandler.post(() -> {
            mSearchProgress.setVisibility(View.VISIBLE);
            mGalleryListView.setVisibility(View.GONE);
            mOfflineLabel.setVisibility(View.GONE);
        });

        QEDGalleryPages.getAlbumList(this);
    }
}
