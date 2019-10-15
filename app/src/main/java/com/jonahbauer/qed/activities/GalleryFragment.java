package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qedgallery.Gallery;
import com.jonahbauer.qed.qedgallery.GalleryAdapter;
import com.jonahbauer.qed.qedgallery.QEDGalleryList;
import com.jonahbauer.qed.qedgallery.QEDGalleryListReceiver;
import com.jonahbauer.qed.qedgallery.QEDGalleryLogin;
import com.jonahbauer.qed.qedgallery.QEDGalleryLoginReceiver;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment implements QEDGalleryLoginReceiver, QEDGalleryListReceiver {
    private ListView galleryListView;
    private ProgressBar searchProgress;
    private char[] userid;
    private char[] sessionid;
    private char[] pwhash;

    private GalleryAdapter galleryAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QEDGalleryLogin qedGalleryLogin = new QEDGalleryLogin();
        qedGalleryLogin.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        galleryAdapter = new GalleryAdapter(getContext(), new ArrayList<>());

        galleryListView = view.findViewById(R.id.gallery_list_container);
        galleryListView.setAdapter(galleryAdapter);
        galleryListView.setOnItemClickListener((parent, view1, position, id) -> {
            Gallery gallery = galleryAdapter.getItem((int) id);

            Intent intent = new Intent(GalleryFragment.this.getContext(), GalleryAlbumActivity.class);
            intent.putExtra(GalleryAlbumActivity.GALLERY_ALBUM_KEY, gallery);
            intent.putExtra(GalleryAlbumActivity.GALLERY_PWHASH_KEY, pwhash);
            intent.putExtra(GalleryAlbumActivity.GALLERY_SESSIONID_KEY, sessionid);
            intent.putExtra(GalleryAlbumActivity.GALLERY_USERID_KEY, userid);
            startActivity(intent);
        });

        searchProgress = view.findViewById(R.id.search_progress);

        galleryAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onReceiveSessionId(char[] sessionId, char[] pwhash, char[] userid) {
        if (sessionId == null || pwhash == null || userid == null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.gallery_login_failed));
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        } else {
            this.sessionid = sessionId;
            this.pwhash = pwhash;
            this.userid = userid;
            QEDGalleryList qedGalleryList = new QEDGalleryList();
            qedGalleryList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionId, pwhash, userid);
        }
    }

    @Override
    public void onGalleryListReceived(List<Gallery> galleries) {
        galleryAdapter.addAll(galleries);
        galleryAdapter.notifyDataSetChanged();
        searchProgress.setVisibility(View.GONE);
        galleryListView.setVisibility(View.VISIBLE);
    }
}
