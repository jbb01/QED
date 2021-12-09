package com.jonahbauer.qed.activities;

import static com.jonahbauer.qed.activities.DeepLinkingActivity.QEDIntent;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.imageActivity.ImageActivity;
import com.jonahbauer.qed.activities.sheets.album.AlbumInfoBottomSheet;
import com.jonahbauer.qed.databinding.ActivityGalleryAlbumBinding;
import com.jonahbauer.qed.layoutStuff.CustomArrayAdapter;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.adapter.ImageAdapter;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TimeUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GalleryAlbumActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener {
    public static final String GALLERY_ALBUM_KEY = "galleryAlbum";

    private Album mAlbum;
    private Album.Filter mFilter;

    private AlbumViewModel mAlbumViewModel;
    private ActivityGalleryAlbumBinding mBinding;

    private ImageAdapter mImageAdapter;
    private ArrayAdapter<String> mAdapterCategory;
    private CustomArrayAdapter<LocalDate> mAdapterDate;
    private CustomArrayAdapter<Person> mAdapterPhotographer;

    private ActivityResultLauncher<Intent> mImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        onNewIntent(intent);
        if (isFinishing()) return;

        mAlbumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);

        mBinding = ActivityGalleryAlbumBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // setup toolbar
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);

        mImageAdapter = new ImageAdapter(this, new ArrayList<>());
        mBinding.imageContainer.setAdapter(mImageAdapter);
        mBinding.imageContainer.setOnItemClickListener(this);

        mBinding.expandCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumPhotographerCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumDateCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumCategoryCheckBox.setOnCheckedChangeListener(this);

        mAdapterCategory = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterCategory.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumCategorySpinner.setAdapter(mAdapterCategory);
        mBinding.albumCategorySpinner.setEnabled(false);

        mAdapterPhotographer = new CustomArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterPhotographer.setToString(Person::getUsername);
        mAdapterPhotographer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumPhotographerSpinner.setAdapter(mAdapterPhotographer);
        mBinding.albumPhotographerSpinner.setEnabled(false);

        mAdapterDate = new CustomArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterDate.setToString(TimeUtils::format);
        mAdapterDate.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumDateSpinner.setAdapter(mAdapterDate);
        mBinding.albumDateSpinner.setEnabled(false);

        mBinding.searchButton.setOnClickListener(view -> search());

        mBinding.setOnOfflineClick(v -> {
            if (Preferences.gallery().isOfflineMode()) {
                Preferences.gallery().edit().setOfflineMode(false).apply();
            }
            search();
        });

        adjustColumnCount(getResources().getConfiguration());

        mAlbumViewModel.getAlbum().observe(this, this::updateView);
        mAlbumViewModel.getOffline().observe(this, offline -> {
            mBinding.setOffline(offline);
            mBinding.setForcedOfflineMode(Preferences.gallery().isOfflineMode());
        });

        mImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data != null) {
                        Image image = data.getParcelableExtra(ImageActivity.GALLERY_IMAGE_KEY);
                        int index = mImageAdapter.getImages().indexOf(image);
                        if (index != -1) {
                            mBinding.imageContainer.smoothScrollToPosition(index);
                        }
                    }
                }
        );

        load();
    }

    @NonNull
    private Album getAlbum() {
        StatusWrapper<Album> wrapper = mAlbumViewModel.getAlbum().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Album album = wrapper.getValue();
        assert album != null : "Album should not be null";
        return album;
    }

    private void loadFilters() {
        Album.Filter.Builder builder = Album.Filter.builder();

        if (mBinding.albumCategoryCheckBox.isChecked()) {
            String category = (String) mBinding.albumCategorySpinner.getSelectedItem();
            builder.setCategory(category);
        }
        if (mBinding.albumDateCheckBox.isChecked()) {
            LocalDate date = (LocalDate) mBinding.albumDateSpinner.getSelectedItem();
            builder.setDay(date);
        }
        if (mBinding.albumPhotographerCheckBox.isChecked()) {
            Person person = (Person) mBinding.albumPhotographerSpinner.getSelectedItem();
            builder.setOwner(person);
        }

        mFilter = builder.build();
    }

    private void search() {
        loadFilters();
        load();
    }

    private void load() {
        boolean offline = Preferences.gallery().isOfflineMode();
        mAlbumViewModel.load(mAlbum, offline ? null : mFilter, offline);
    }

    private void updateView(StatusWrapper<Album> albumStatusWrapper) {
        Album album = albumStatusWrapper.getValue();
        List<Image> images = album != null ? album.getImages() : Collections.emptyList();

        mBinding.setAlbum(album);
        mBinding.setStatus(albumStatusWrapper.getCode());
        mBinding.setError(getString(albumStatusWrapper.getErrorMessage()));

        boolean successOrEmpty = albumStatusWrapper.getCode() == StatusWrapper.STATUS_LOADED;
        successOrEmpty |= albumStatusWrapper.getReason() == Reason.EMPTY && album != null;
        if (successOrEmpty) {
            assert album != null;

            Objects.requireNonNull(getSupportActionBar()).setTitle(album.getName());

            mAdapterCategory.clear();
            mAdapterCategory.addAll(album.getCategories());
            mAdapterCategory.notifyDataSetChanged();

            mAdapterPhotographer.clear();
            mAdapterPhotographer.addAll(album.getPersons());
            mAdapterPhotographer.notifyDataSetChanged();

            mAdapterDate.clear();
            mAdapterDate.addAll(album.getDates());
            mAdapterDate.notifyDataSetChanged();

            mImageAdapter.clear();
            mImageAdapter.addAll(images);
            mImageAdapter.notifyDataSetChanged();
        }

        if (albumStatusWrapper.getCode() == StatusWrapper.STATUS_ERROR) {
            mImageAdapter.clear();
            Reason reason = albumStatusWrapper.getReason();
            mBinding.setError(getString(reason == Reason.EMPTY ? R.string.album_empty : reason.getStringRes()));
        }

        int hits = mImageAdapter.getImages().size();
        if (hits > 0) {
            mBinding.setHits(getString(R.string.hits, hits));
        } else {
            mBinding.setHits("");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustColumnCount(newConfig);
    }

    private void adjustColumnCount(@NonNull Configuration configuration) {
        double width = configuration.screenWidthDp;
        int columnCount = Double.valueOf(Math.round(width / 150d)).intValue();

        mBinding.imageContainer.setNumColumns(columnCount);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.album_info) {
            AlbumInfoBottomSheet albumInfoBottomSheet = AlbumInfoBottomSheet.newInstance(getAlbum());
            albumInfoBottomSheet.show(getSupportFragmentManager(), albumInfoBottomSheet.getTag());
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.expand_checkBox) {
            if (isChecked) {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.expand(mBinding.expandable);
            } else {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.collapse(mBinding.expandable);
            }
        } else if (id == R.id.album_photographer_check_box) {
            mBinding.albumPhotographerSpinner.setEnabled(isChecked);
        } else if (id == R.id.album_category_check_box) {
            mBinding.albumCategorySpinner.setEnabled(isChecked);
        } else if (id == R.id.album_date_check_box) {
            mBinding.albumDateSpinner.setEnabled(isChecked);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Image image = mImageAdapter.getItem(position);

        if (image == null) {
            Snackbar.make(mBinding.getRoot(), R.string.image_not_downloaded, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra(ImageActivity.GALLERY_IMAGE_KEY, image);
        intent.putParcelableArrayListExtra(ImageActivity.GALLERY_IMAGES_KEY, new ArrayList<>(mImageAdapter.getImages()));
        mImageLauncher.launch(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (handleIntent(intent, this)) {
            super.onNewIntent(intent);
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public static boolean handleIntent(@NonNull Intent intent, @Nullable GalleryAlbumActivity activity) {
        Album album = intent.getParcelableExtra(GALLERY_ALBUM_KEY);

        // internal intent
        if (album != null) {
            if (activity != null) {
                activity.mAlbum = album;
            }

            return true;
        }

        // external intent via deep link
        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW_ALBUM.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data != null) {
                String host = data.getHost();
                if (host == null || !host.equals("qedgallery.qed-verein.de")) return false;

                String path = data.getPath();
                if (path == null || !path.startsWith("/album_view.php")) return false;

                String idStr = data.getQueryParameter("albumid");
                if (idStr == null) return false;

                // TODO apply filters
                try {
                    long id = Long.parseLong(idStr);

                    if (activity != null) {
                        Preferences.general().edit().setDrawerSelection(MainActivity.DrawerSelection.GALLERY).apply();
                        activity.mAlbum = new Album(id);
                    }

                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return false;
    }
}
