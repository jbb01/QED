package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.album.AlbumAdapter;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment implements QEDPageReceiver<List<Album>>, GalleryDatabaseReceiver {
    private ListView galleryListView;
    private ProgressBar searchProgress;
    private TextView offlineLabel;

    private boolean online;

    private SharedPreferences sharedPreferences;

    private AlbumAdapter galleryAdapter;
    private GalleryDatabase galleryDatabase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assert getContext() != null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        online = false;

        galleryDatabase = new GalleryDatabase();
        galleryDatabase.init(getContext(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        galleryAdapter = new AlbumAdapter(getContext(), new ArrayList<>());

        galleryListView = view.findViewById(R.id.gallery_list_container);
        galleryListView.setAdapter(galleryAdapter);
        galleryListView.setOnItemClickListener((parent, view1, position, id) -> {
            Album album = galleryAdapter.getItem((int) id);

            if (album == null) return;
            if (online || album.imageListDownloaded) {
                Intent intent = new Intent(GalleryFragment.this.getContext(), GalleryAlbumActivity.class);
                intent.putExtra(GalleryAlbumActivity.GALLERY_ALBUM_KEY, album);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), getString(R.string.album_not_downloaded), Toast.LENGTH_SHORT).show();
            }
        });

        searchProgress = view.findViewById(R.id.search_progress);
        offlineLabel = view.findViewById(R.id.label_offline);

        if (!sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false))
            offlineLabel.setOnClickListener(v -> switchToOnlineMode());

        galleryAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false))
            switchToOnlineMode();
        else
            switchToOfflineMode();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
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
        galleryDatabase.close();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onPageReceived(String tag, List<Album> albums) {
        galleryAdapter.clear();
        galleryAdapter.addAll(albums);
        galleryAdapter.notifyDataSetChanged();
        searchProgress.setVisibility(View.GONE);
        galleryListView.setVisibility(View.VISIBLE);

        galleryDatabase.insertAllAlbums(albums, false);
        online = true;
    }

    @Override
    public void onNetworkError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "networkError at: " + tag);
        switchToOfflineMode();

    }

    @Override
    public void onReceiveResult(List items) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    private void switchToOfflineMode() {
        online = false;
        offlineLabel.post(() -> {
            offlineLabel.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), offlineLabel.getContext().getString(R.string.login_failed_switching_to_offline), Toast.LENGTH_SHORT).show();
        });

        galleryListView.post(() -> {
            galleryAdapter.clear();
            galleryAdapter.addAll(galleryDatabase.getAlbums());
            galleryAdapter.notifyDataSetChanged();
        });

        searchProgress.post(() -> {
            searchProgress.setVisibility(View.GONE);
            galleryListView.setVisibility(View.VISIBLE);
        });
    }

    private void switchToOnlineMode() {
        searchProgress.post(() -> {
            searchProgress.setVisibility(View.VISIBLE);
            galleryListView.setVisibility(View.GONE);
            offlineLabel.setVisibility(View.GONE);
        });

        QEDGalleryPages.getAlbumList(getClass().toString(), this);
    }
}
