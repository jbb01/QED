package com.jonahbauer.qed.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.QEDGalleryPages.Filter;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;
import com.jonahbauer.qed.qedgallery.image.ImageAdapter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.jonahbauer.qed.DeepLinkingActivity.QEDIntent;

public class GalleryAlbumActivity extends AppCompatActivity implements QEDPageReceiver<Album>, CompoundButton.OnCheckedChangeListener, View.OnClickListener, GalleryDatabaseReceiver, AdapterView.OnItemClickListener {
    public static final String GALLERY_ALBUM_KEY = "galleryAlbum";

    private Application mApplication;

    private Album mAlbum;

    private ImageAdapter mImageAdapter;
    private GridView mImageGridView;
    private ProgressBar mProgressBar;

    private View mExpand;
    private View mSearchFilters;
    private RadioButton mRadioButtonPhotographer;
    private RadioButton mRadioButtonDate;
    private RadioButton mRadioButtonCategory;
    private Spinner mSpinnerPhotographer;
    private Spinner mSpinnerDate;
    private Spinner mSpinnerCategory;
    private Button mSearchButton;
    private ArrayAdapter<String> mAdapterCategory;
    private ArrayAdapter<String> mAdapterDate;
    private ArrayAdapter<String> mAdapterPhotographer;

    private TextView mAlbumErrorText;

    private MenuItem mMenuItemInfo;
    private Toolbar mToolbar;

    // -1: none
    // 0: photographer
    // 1: date
    // 2: category
    private int mActiveRadioButton = -1;
    private GalleryDatabase mGalleryDatabase;

    private boolean mOnline;
    private TextView mOfflineLabel;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (Application) getApplication();

        Intent intent = getIntent();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        setContentView(R.layout.activity_gallery_album);

        mOnline = false;
        mOfflineLabel = findViewById(R.id.label_offline);
        if (!mSharedPreferences.getBoolean(Pref.Gallery.OFFLINE_MODE, false))
            mOfflineLabel.setOnClickListener(v -> switchToOnlineMode());

        mGalleryDatabase = new GalleryDatabase();
        mGalleryDatabase.init(this, this);

        if (mAlbum == null) finish();

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(mAlbum.name);

        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);

        mImageGridView = findViewById(R.id.image_container);
        mImageAdapter = new ImageAdapter(this, new ArrayList<>(), false);

        mImageGridView.setAdapter(mImageAdapter);
        mImageGridView.setOnItemClickListener(this);

        mProgressBar = findViewById(R.id.progress_bar);

        mExpand = findViewById(R.id.expandable);
        mSearchFilters = findViewById(R.id.album_search_filters);
        mRadioButtonCategory = findViewById(R.id.album_category_radio_button);
        mRadioButtonDate = findViewById(R.id.album_date_radio_button);
        mRadioButtonPhotographer = findViewById(R.id.album_photographer_radio_button);
        mSpinnerCategory = findViewById(R.id.album_category_spinner);
        mSpinnerDate = findViewById(R.id.album_date_spinner);
        mSpinnerPhotographer = findViewById(R.id.album_photographer_spinner);
        mAlbumErrorText = findViewById(R.id.error_album);
        CheckBox expandCheckBox = findViewById(R.id.expand_checkBox);
        mSearchButton = findViewById(R.id.search_button);

        expandCheckBox.setOnCheckedChangeListener(this);
        mRadioButtonPhotographer.setOnCheckedChangeListener(this);
        mRadioButtonDate.setOnCheckedChangeListener(this);
        mRadioButtonCategory.setOnCheckedChangeListener(this);
        mRadioButtonPhotographer.setOnClickListener(this);
        mRadioButtonDate.setOnClickListener(this);
        mRadioButtonCategory.setOnClickListener(this);

        mAdapterCategory = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mAlbum.categories);
        mSpinnerCategory.setAdapter(mAdapterCategory);
        mSpinnerCategory.setEnabled(false);

        mAdapterPhotographer = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        mSpinnerPhotographer.setAdapter(mAdapterPhotographer);
        mSpinnerPhotographer.setEnabled(false);

        mAdapterDate = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        mSpinnerDate.setAdapter(mAdapterDate);
        mSpinnerDate.setEnabled(false);

        mSearchButton.setEnabled(false);
        mSearchButton.setOnClickListener(view -> {
            mSearchButton.setEnabled(false);

            HashMap<Filter, String> filterData = new HashMap<>();
            if (mRadioButtonCategory.isChecked()) {
                filterData.put(Filter.BY_CATEGORY, (String) mSpinnerCategory.getSelectedItem());
            }
            if (mRadioButtonDate.isChecked()) {
                String[] parts = ((String) mSpinnerDate.getSelectedItem()).split("\\.");
                try {
                    filterData.put(Filter.BY_DATE, parts[2] + "-" + parts[1] + "-" + parts[0]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                }
            }
            if (mRadioButtonPhotographer.isChecked()) {
                String personName = (String) mSpinnerPhotographer.getSelectedItem();
                Optional<Person> personOptional = mAlbum.persons.stream().filter(person -> person.firstName.equals(personName)).findFirst();
                personOptional.ifPresent(person -> filterData.put(Filter.BY_PERSON, String.valueOf(person.id)));
            }

            mImageGridView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            QEDGalleryPages.getAlbum(getClass().toString(), mAlbum, filterData, this);
        });


        double width = getResources().getConfiguration().screenWidthDp;
        int columnCount = Double.valueOf(Math.round(width / 150d)).intValue();

        mImageGridView.setNumColumns(columnCount);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mSharedPreferences.getBoolean(Pref.Gallery.OFFLINE_MODE, false))
            switchToOnlineMode();
        else
            switchToOfflineMode();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        double width = newConfig.screenWidthDp;
        int columnCount = Double.valueOf(Math.round(width / 150d)).intValue();

        mImageGridView.setNumColumns(columnCount);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.album_info) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(mAlbum.name);
            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(this).inflate(R.layout.alert_dialog_album_info, null);
            ((TextView)view.findViewById(R.id.album_creator)).setText(mAlbum.owner);
            ((TextView)view.findViewById(R.id.album_creation_date)).setText(mAlbum.creationDate);

            mAlbum.dates.sort(Comparator.comparing(d -> d));
            String albumCreationDates = MessageFormat.format("{0,date,dd.MM.yyyy} - {1,date,dd.MM.yyyy}", mAlbum.dates.get(0), mAlbum.dates.get(mAlbum.dates.size() - 1));
            ((TextView)view.findViewById(R.id.album_creation_dates)).setText(albumCreationDates);

            alertDialogBuilder.setView(view);
            alertDialogBuilder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

            alertDialogBuilder.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album, menu);
        mMenuItemInfo = menu.findItem(R.id.album_info);
        mMenuItemInfo.setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onPageReceived(String tag, @Nullable Album album) {
        mOnline = true;
        mImageAdapter.setOfflineMode(false);

        if (album == null) {
            mAlbumErrorText.setText(R.string.album_invalid);
            mAlbumErrorText.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            return;
        }

        if (album.name != null && !album.name.equals("")) mToolbar.setTitle(album.name);

        if (album.images.isEmpty()) {
            mAlbumErrorText.post(() -> {
                mAlbumErrorText.setText(R.string.album_empty);
                mAlbumErrorText.setVisibility(View.VISIBLE);
            });
            mProgressBar.setVisibility(View.GONE);
            mImageAdapter.clear();
            mImageAdapter.notifyDataSetChanged();
            mSearchButton.setEnabled(true);
            return;
        }

        mAlbumErrorText.setVisibility(View.GONE);

        mImageAdapter.clear();
        mImageAdapter.addAll(album.images);
        mImageAdapter.notifyDataSetChanged();

        mAdapterCategory.notifyDataSetChanged();

        mAdapterPhotographer.clear();
        this.mAlbum.persons.stream().map(person -> person.firstName).sorted().forEach(mAdapterPhotographer::add);
        mAdapterPhotographer.notifyDataSetChanged();

        mAdapterDate.clear();
        this.mAlbum.dates.stream().sorted().map(date -> MessageFormat.format("{0,date,dd.MM.yyyy}", date)).forEach(mAdapterDate::add);
        mAdapterDate.notifyDataSetChanged();

        if (!this.mAlbum.images.isEmpty()) {
            this.mAlbum.imageListDownloaded = true;
        }

        mGalleryDatabase.insert(this.mAlbum, true);
        mGalleryDatabase.insertAllImages(this.mAlbum.images, false);

        mMenuItemInfo.setEnabled(true);
        mSearchButton.setEnabled(true);

        mProgressBar.post(() -> {
            mProgressBar.setVisibility(View.GONE);
            mImageGridView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(tag, reason, cause);

        if (REASON_NETWORK.equals(reason))
            switchToOfflineMode();
    }

    @Override
    public void onClick(View v) {
        if (!(v instanceof RadioButton)) return;

        RadioButton radioButton = (RadioButton) v;

        switch (v.getId()) {
            case R.id.album_photographer_radio_button:
                if (mActiveRadioButton == 0) {
                    radioButton.setChecked(false);
                    mActiveRadioButton = -1;
                } else {
                    mActiveRadioButton = 0;
                }
                break;
            case R.id.album_date_radio_button:
                if (mActiveRadioButton == 1) {
                    radioButton.setChecked(false);
                    mActiveRadioButton = -1;
                } else {
                    mActiveRadioButton = 1;
                }
                break;
            case R.id.album_category_radio_button:
                if (mActiveRadioButton == 2) {
                    radioButton.setChecked(false);
                    mActiveRadioButton = -1;
                } else {
                    mActiveRadioButton = 2;
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.expand_checkBox:
                if (isChecked) {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    expand(mExpand);
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(mExpand);
                }
                break;
            case R.id.album_photographer_radio_button:
                if (isChecked) {
                    mRadioButtonCategory.setChecked(false);
                    mRadioButtonDate.setChecked(false);
                }
                mSpinnerPhotographer.setEnabled(isChecked);
                break;
            case R.id.album_category_radio_button:
                if (isChecked) {
                    mRadioButtonDate.setChecked(false);
                    mRadioButtonPhotographer.setChecked(false);
                }
                mSpinnerCategory.setEnabled(isChecked);
                break;
            case R.id.album_date_radio_button:
                if (isChecked) {
                    mRadioButtonCategory.setChecked(false);
                    mRadioButtonPhotographer.setChecked(false);
                }
                mSpinnerDate.setEnabled(isChecked);
                break;
        }
    }

    private static void expand(final View v) {
        v.setVisibility(View.VISIBLE);
    }

    private static void collapse(final View v) {
        v.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGalleryDatabase.close();
        mImageAdapter.clearCache();
    }

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Image image = mImageAdapter.getItem(position);
        if (image == null || (!mOnline && !image.available)) {
            Toast.makeText(mApplication, getString(R.string.image_not_downloaded), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra(ImageActivity.GALLERY_IMAGE_KEY, image);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void switchToOfflineMode() {
        mOnline = false;
        mOfflineLabel.post(() -> {
            mOfflineLabel.setVisibility(View.VISIBLE);

            if (!mSharedPreferences.getBoolean(Pref.Gallery.OFFLINE_MODE, false))
                Toast.makeText(mApplication, mOfflineLabel.getContext().getString(R.string.login_failed_switching_to_offline), Toast.LENGTH_SHORT).show();
        });

        mSearchFilters.post(() -> mSearchFilters.setVisibility(View.GONE));

        mImageGridView.post(() -> {
            mImageAdapter.clear();

            mGalleryDatabase.getAlbumData(mAlbum);

            if (mAlbum.name != null && !mAlbum.name.equals("")) mToolbar.setTitle(mAlbum.name);

            boolean unknownAlbum = mGalleryDatabase.getAlbums().stream().noneMatch(album1 -> album1.id == mAlbum.id);
            if (unknownAlbum) {
                mAlbumErrorText.setText(R.string.album_not_downloaded);
                mAlbumErrorText.setVisibility(View.VISIBLE);
                mImageGridView.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                return;
            }

            List<Image> images = mGalleryDatabase.getImageList(mAlbum);
            if (images == null || images.isEmpty()) {
                mAlbumErrorText.setText(R.string.album_empty);
                mAlbumErrorText.setVisibility(View.GONE);
                mImageGridView.setVisibility(View.GONE);
            } else {
                mImageAdapter.addAll(mGalleryDatabase.getImageList(mAlbum));
                mImageAdapter.notifyDataSetChanged();
                mImageGridView.setVisibility(View.VISIBLE);
                mAlbumErrorText.setVisibility(View.GONE);
            }

            mProgressBar.setVisibility(View.GONE);
        });

        mImageAdapter.setOfflineMode(true);

    }

    private void switchToOnlineMode() {
        mOfflineLabel.post(() -> mOfflineLabel.setVisibility(View.GONE));
        mSearchFilters.post(() -> mSearchFilters.setVisibility(View.VISIBLE));
        mOfflineLabel.postDelayed(() -> ImageAdapter.sReceivedError = false, 5000);

        QEDGalleryPages.getAlbum(getClass().toString(), mAlbum, null, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onNewIntent(intent);
    }

    public static boolean handleIntent(@NonNull Intent intent, @Nullable GalleryAlbumActivity activity) {
        Object obj = intent.getSerializableExtra(GALLERY_ALBUM_KEY);
        if (obj instanceof Album) {
            if (activity != null) activity.mAlbum = (Album) obj;
            return true;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW_ALBUM.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String host = data.getHost();
                String path = data.getPath();

                String query = data.getQuery();
                Map<String, String> queries = new HashMap<>();
                if (query != null) for (String q : query.split("&")) {
                    String[] parts = q.split("=");
                    if (parts.length > 1) queries.put(parts[0], parts[1]);
                    else if (parts.length > 0) queries.put(parts[0], "");
                }
                if (host != null) if (host.equals("qedgallery.qed-verein.de")) {
                    if (path != null) if (path.startsWith("/album_view.php")) {
                        if (activity != null) {
                            SharedPreferences.Editor editor = activity.mSharedPreferences.edit();
                            editor.putInt(Pref.General.DRAWER_SELECTION, R.id.nav_gallery).apply();
                        }

                        String albumIdStr = queries.getOrDefault("albumid", null);
                        if (albumIdStr != null && albumIdStr.matches("\\d*")) {
                            try {
                                int id = Integer.parseInt(albumIdStr);
                                if (activity != null) {
                                    activity.mAlbum = new Album();
                                    activity.mAlbum.id = id;
                                }
                                return true;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return false;
    }
}
