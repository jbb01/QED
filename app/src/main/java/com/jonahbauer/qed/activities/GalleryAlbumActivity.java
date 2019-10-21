package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
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

import static com.jonahbauer.qed.Application.getContext;

public class GalleryAlbumActivity extends AppCompatActivity implements QEDPageReceiver<Album>, CompoundButton.OnCheckedChangeListener, View.OnClickListener, GalleryDatabaseReceiver, AdapterView.OnItemClickListener {
    public static final String GALLERY_ALBUM_KEY = "galleryAlbum";

    private Album album;

    private ImageAdapter imageAdapter;
    private GridView imageGridView;
    private ProgressBar progressBar;

    private View expand;
    private View searchFilters;
    private RadioButton radioButtonPhotographer;
    private RadioButton radioButtonDate;
    private RadioButton radioButtonCategory;
    private Spinner spinnerPhotographer;
    private Spinner spinnerDate;
    private Spinner spinnerCategory;
    private Button searchButton;
    private ArrayAdapter adapterCategory;
    private ArrayAdapter adapterDate;
    private ArrayAdapter adapterPhotographer;

    private TextView emptyGalleryText;

    private MenuItem menuItemInfo;

    // -1: none
    // 0: photographer
    // 1: date
    // 2: category
    private int activeRadioButton = -1;
    private GalleryDatabase galleryDatabase;

    private boolean online;
    private TextView offlineLabel;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_album);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        online = false;
        offlineLabel = findViewById(R.id.label_offline);
        if (!sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false))
            offlineLabel.setOnClickListener(v -> switchToOnlineMode());

        galleryDatabase = new GalleryDatabase();
        galleryDatabase.init(this, this);

        Intent intent = getIntent();
        handleIntent(intent);

        if (album == null) finish();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(album.name);

        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);

        imageGridView = findViewById(R.id.image_container);
        imageAdapter = new ImageAdapter(this, new ArrayList<>(), false);

        imageGridView.setAdapter(imageAdapter);
        imageGridView.setOnItemClickListener(this);

        progressBar = findViewById(R.id.progress_bar);

        expand = findViewById(R.id.expand);
        searchFilters = findViewById(R.id.album_search_filters);
        radioButtonCategory = findViewById(R.id.album_category_radio_button);
        radioButtonDate = findViewById(R.id.album_date_radio_button);
        radioButtonPhotographer = findViewById(R.id.album_photographer_radio_button);
        spinnerCategory = findViewById(R.id.album_category_spinner);
        spinnerDate = findViewById(R.id.album_date_spinner);
        spinnerPhotographer = findViewById(R.id.album_photographer_spinner);
        emptyGalleryText = findViewById(R.id.empty_album);
        CheckBox expandCheckBox = findViewById(R.id.expand_checkBox);
        searchButton = findViewById(R.id.search_button);

        expandCheckBox.setOnCheckedChangeListener(this);
        radioButtonPhotographer.setOnCheckedChangeListener(this);
        radioButtonDate.setOnCheckedChangeListener(this);
        radioButtonCategory.setOnCheckedChangeListener(this);
        radioButtonPhotographer.setOnClickListener(this);
        radioButtonDate.setOnClickListener(this);
        radioButtonCategory.setOnClickListener(this);

        adapterCategory = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, album.categories);
        spinnerCategory.setAdapter(adapterCategory);
        spinnerCategory.setEnabled(false);

        adapterPhotographer = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        spinnerPhotographer.setAdapter(adapterPhotographer);
        spinnerPhotographer.setEnabled(false);

        adapterDate = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        spinnerDate.setAdapter(adapterDate);
        spinnerDate.setEnabled(false);

        searchButton.setEnabled(false);
        searchButton.setOnClickListener(view -> {
            searchButton.setEnabled(false);

            HashMap<Filter, String> filterData = new HashMap<>();
            if (radioButtonCategory.isChecked()) {
                filterData.put(Filter.BY_CATEGORY, (String) spinnerCategory.getSelectedItem());
            }
            if (radioButtonDate.isChecked()) {
                String[] parts = ((String) spinnerDate.getSelectedItem()).split("\\.");
                try {
                    filterData.put(Filter.BY_DATE, parts[2] + "-" + parts[1] + "-" + parts[0]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                }
            }
            if (radioButtonPhotographer.isChecked()) {
                String personName = (String) spinnerPhotographer.getSelectedItem();
                Optional<Person> personOptional = album.persons.stream().filter(person -> person.firstName.equals(personName)).findFirst();
                personOptional.ifPresent(person -> filterData.put(Filter.BY_PERSON, String.valueOf(person.id)));
            }

            imageGridView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            QEDGalleryPages.getAlbum(getClass().toString(), album, filterData, this);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false))
            switchToOnlineMode();
        else
            switchToOfflineMode();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.album_info) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(album.name);
            View view = LayoutInflater.from(this).inflate(R.layout.alert_dialog_album_info, null);
            ((TextView)view.findViewById(R.id.album_creator)).setText(album.owner);
            ((TextView)view.findViewById(R.id.album_creation_date)).setText(album.creationDate);

            album.dates.sort(Comparator.comparing(d -> d));
            String albumCreationDates = MessageFormat.format("{0,date,dd.MM.yyyy} - {1,date,dd.MM.yyyy}", album.dates.get(0), album.dates.get(album.dates.size() - 1));
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
        menuItemInfo = menu.findItem(R.id.album_info);
        menuItemInfo.setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onPageReceived(String tag, Album album) {
        online = true;
        imageAdapter.setOfflineMode(false);
        if (album.images == null || album.images.isEmpty()) {
            emptyGalleryText.post(() -> emptyGalleryText.setVisibility(View.VISIBLE));
            progressBar.setVisibility(View.GONE);
            imageAdapter.clear();
            imageAdapter.notifyDataSetChanged();
            searchButton.setEnabled(true);
            return;
        }
        emptyGalleryText.setVisibility(View.GONE);

        imageAdapter.clear();
        imageAdapter.addAll(album.images);
        imageAdapter.notifyDataSetChanged();

        adapterCategory.notifyDataSetChanged();

        adapterPhotographer.clear();
        this.album.persons.stream().map(person -> person.firstName).sorted().forEach(adapterPhotographer::add);
        adapterPhotographer.notifyDataSetChanged();

        adapterDate.clear();
        this.album.dates.stream().sorted().map(date -> MessageFormat.format("{0,date,dd.MM.yyyy}", date)).forEach(adapterDate::add);
        adapterDate.notifyDataSetChanged();

        if (this.album.images != null && !this.album.images.isEmpty()) {
            this.album.imageListDownloaded = true;
        }

        galleryDatabase.insert(this.album, true);
        galleryDatabase.insertAllImages(this.album.images, false);

        menuItemInfo.setEnabled(true);
        searchButton.setEnabled(true);

        progressBar.post(() -> {
            progressBar.setVisibility(View.GONE);
            imageGridView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onNetworkError(String tag) {
        switchToOfflineMode();
    }

    @Override
    public void onClick(View v) {
        if (!(v instanceof RadioButton)) return;

        RadioButton radioButton = (RadioButton) v;

        switch (v.getId()) {
            case R.id.album_photographer_radio_button:
                if (activeRadioButton == 0) {
                    radioButton.setChecked(false);
                    activeRadioButton = -1;
                } else {
                    activeRadioButton = 0;
                }
                break;
            case R.id.album_date_radio_button:
                if (activeRadioButton == 1) {
                    radioButton.setChecked(false);
                    activeRadioButton = -1;
                } else {
                    activeRadioButton = 1;
                }
                break;
            case R.id.album_category_radio_button:
                if (activeRadioButton == 2) {
                    radioButton.setChecked(false);
                    activeRadioButton = -1;
                } else {
                    activeRadioButton = 2;
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
                    expand(expand);
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(expand);
                }
                break;
            case R.id.album_photographer_radio_button:
                if (isChecked) {
                    radioButtonCategory.setChecked(false);
                    radioButtonDate.setChecked(false);
                }
                spinnerPhotographer.setEnabled(isChecked);
                break;
            case R.id.album_category_radio_button:
                if (isChecked) {
                    radioButtonDate.setChecked(false);
                    radioButtonPhotographer.setChecked(false);
                }
                spinnerCategory.setEnabled(isChecked);
                break;
            case R.id.album_date_radio_button:
                if (isChecked) {
                    radioButtonCategory.setChecked(false);
                    radioButtonPhotographer.setChecked(false);
                }
                spinnerDate.setEnabled(isChecked);
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
        galleryDatabase.close();
    }

    @Override
    public void onReceiveResult(List items) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Image image = imageAdapter.getItem(position);
        if (image == null || (!online && !image.available)) {
            Toast.makeText(getContext(), getString(R.string.image_not_downloaded), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra(ImageActivity.GALLERY_IMAGE_KEY, image);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void switchToOfflineMode() {
        online = false;
        offlineLabel.post(() -> {
            offlineLabel.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), offlineLabel.getContext().getString(R.string.login_failed_switching_to_offline), Toast.LENGTH_SHORT).show();
        });

        searchFilters.post(() -> searchFilters.setVisibility(View.GONE));

        imageGridView.post(() -> {
            imageAdapter.clear();
            imageAdapter.addAll(galleryDatabase.getImageList(album));
            imageAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            imageGridView.setVisibility(View.VISIBLE);
        });

        imageAdapter.setOfflineMode(true);

    }

    private void switchToOnlineMode() {
        offlineLabel.post(() -> offlineLabel.setVisibility(View.GONE));
        searchFilters.post(() -> searchFilters.setVisibility(View.VISIBLE));
        offlineLabel.postDelayed(() -> ImageAdapter.receivedError = false, 5000);

        QEDGalleryPages.getAlbum(getClass().toString(), album, null, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Object obj = intent.getSerializableExtra(GALLERY_ALBUM_KEY);
        if (obj instanceof Album) {
            album = (Album) obj;
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
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
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_gallery).apply();

                        String albumIdStr = queries.getOrDefault("albumid", null);
                        if (albumIdStr != null) {
                            try {
                                int id = Integer.parseInt(albumIdStr);
                                album = new Album();
                                album.id = id;
                            } catch (NumberFormatException ignored) {
                                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }
    }
}
