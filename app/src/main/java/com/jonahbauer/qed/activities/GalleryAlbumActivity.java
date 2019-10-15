package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.GridView;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qedgallery.Gallery;
import com.jonahbauer.qed.qedgallery.Image;
import com.jonahbauer.qed.qedgallery.ImageAdapter;
import com.jonahbauer.qed.qedgallery.QEDGalleryAlbum;
import com.jonahbauer.qed.qedgallery.QEDGalleryAlbumReceiver;

import java.util.ArrayList;
import java.util.List;

public class GalleryAlbumActivity extends AppCompatActivity implements QEDGalleryAlbumReceiver {
    public static final String GALLERY_ALBUM_KEY = "galleryAlbum";
    public static final String GALLERY_PWHASH_KEY = "galleryPwhash";
    public static final String GALLERY_USERID_KEY = "galleryUserid";
    public static final String GALLERY_SESSIONID_KEY = "gallerySessionId";

    private Gallery album;

    private GridView imageGridView;
    private ImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_album);

        Intent intent = getIntent();
        album = (Gallery) intent.getSerializableExtra(GALLERY_ALBUM_KEY);
        char[] pwhash = intent.getCharArrayExtra(GALLERY_PWHASH_KEY);
        char[] userid = intent.getCharArrayExtra(GALLERY_USERID_KEY);
        char[] sessionid = intent.getCharArrayExtra(GALLERY_SESSIONID_KEY);

        if (album == null) finish();
        else if (pwhash == null || userid == null || sessionid == null) {
            Intent intent2 = new Intent(this, LoginActivity.class);
            intent2.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.gallery_login_failed));
            startActivity(intent2);
            finish();
        }


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(album.name);

        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);

        imageGridView = findViewById(R.id.image_container);
        imageAdapter = new ImageAdapter(this, new ArrayList<>(), userid, pwhash, sessionid);

        imageGridView.setAdapter(imageAdapter);

        QEDGalleryAlbum qedGalleryAlbum = new QEDGalleryAlbum();
        qedGalleryAlbum.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionid, pwhash, userid, album);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onGalleryAlbumReceived(List<Image> images) {
        imageAdapter.addAll(images);
        imageAdapter.notifyDataSetChanged();
    }
}
