package com.jonahbauer.qed.activities.mainFragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentAlbumBinding;
import com.jonahbauer.qed.layoutStuff.CustomArrayAdapter;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Album.Filter;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.adapter.ImageAdapter;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TimeUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AlbumFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener {
    public static final String IMAGE_ID_KEY = "imageId";

    private Album mAlbum;
    private Filter mFilter;

    private AlbumViewModel mAlbumViewModel;
    private FragmentAlbumBinding mBinding;

    private ImageAdapter mImageAdapter;
    private ArrayAdapter<String> mAdapterCategory;
    private CustomArrayAdapter<LocalDate> mAdapterDate;
    private CustomArrayAdapter<LocalDate> mAdapterUpload;
    private CustomArrayAdapter<Person> mAdapterPhotographer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle arguments = getArguments();
        if (arguments != null) {
            // handle deep link
            if (arguments.containsKey(NavController.KEY_DEEP_LINK_INTENT)) {
                Intent intent = (Intent) arguments.get(NavController.KEY_DEEP_LINK_INTENT);

                Pair<Album, Filter> data = parseIntent(intent);
                if (data != null) {
                    mAlbum = data.first;
                    mFilter = data.second;
                }
            }

            if (mAlbum == null) {
                mAlbum = AlbumFragmentArgs.fromBundle(arguments).getAlbum();
            }
        }

        if (mAlbum == null) {
            mAlbum = new Album(0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentAlbumBinding.inflate(inflater, container, false);
        mAlbumViewModel = ViewUtils.getViewModelProvider(this).get(AlbumViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mImageAdapter = new ImageAdapter(requireContext(), new ArrayList<>());
        mBinding.imageContainer.setAdapter(mImageAdapter);
        mBinding.imageContainer.setOnItemClickListener(this);

        mBinding.expandCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumPhotographerCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumDateCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumUploadCheckBox.setOnCheckedChangeListener(this);
        mBinding.albumCategoryCheckBox.setOnCheckedChangeListener(this);

        mAdapterCategory = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterCategory.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumCategorySpinner.setAdapter(mAdapterCategory);
        mBinding.albumCategorySpinner.setEnabled(false);

        mAdapterPhotographer = new CustomArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterPhotographer.setToString(Person::getUsername);
        mAdapterPhotographer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumPhotographerSpinner.setAdapter(mAdapterPhotographer);
        mBinding.albumPhotographerSpinner.setEnabled(false);

        mAdapterDate = new CustomArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterDate.setToString(TimeUtils::format);
        mAdapterDate.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumDateSpinner.setAdapter(mAdapterDate);
        mBinding.albumDateSpinner.setEnabled(false);

        mAdapterUpload = new CustomArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterUpload.setToString(TimeUtils::format);
        mAdapterUpload.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mBinding.albumUploadSpinner.setAdapter(mAdapterUpload);
        mBinding.albumUploadSpinner.setEnabled(false);

        mBinding.searchButton.setOnClickListener(v -> search());

        mBinding.setOnOfflineClick(v -> {
            if (Preferences.gallery().isOfflineMode()) {
                Preferences.gallery().edit().setOfflineMode(false).apply();
            }
            search();
        });

        adjustColumnCount(getResources().getConfiguration());

        mAlbumViewModel.getAlbum().observe(getViewLifecycleOwner(), this::updateView);
        mAlbumViewModel.getOffline().observe(getViewLifecycleOwner(), offline -> {
            mBinding.setOffline(offline);
            mBinding.setForcedOfflineMode(Preferences.gallery().isOfflineMode());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAlbumViewModel.getAlbum().getValue() == null) {
            load();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry entry = navController.getCurrentBackStackEntry();
        if (entry != null) {
            Long imageId = entry.getSavedStateHandle()
                                .get(IMAGE_ID_KEY);
            if (imageId != null) {
                int index = mImageAdapter.getImages().indexOf(new Image(imageId));
                if (index != -1) {
                    mBinding.imageContainer.smoothScrollToPosition(index);
                }
            }
        }
    }

    private void loadFilters() {
        Filter.Builder builder = Album.Filter.builder();

        if (mBinding.albumCategoryCheckBox.isChecked()) {
            String category = (String) mBinding.albumCategorySpinner.getSelectedItem();
            builder.setCategory(category);
        }
        if (mBinding.albumDateCheckBox.isChecked()) {
            LocalDate date = (LocalDate) mBinding.albumDateSpinner.getSelectedItem();
            builder.setDay(date);
        }
        if (mBinding.albumUploadCheckBox.isChecked()) {
            LocalDate date = (LocalDate) mBinding.albumUploadSpinner.getSelectedItem();
            builder.setUpload(date);
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

        if (album != null && album.getName() != null) {
            ViewUtils.setActionBarText(this, album.getName());
        }

        boolean successOrEmpty = albumStatusWrapper.getCode() == StatusWrapper.STATUS_LOADED;
        successOrEmpty |= albumStatusWrapper.getReason() == Reason.EMPTY && album != null;
        if (successOrEmpty) {
            assert album != null;

            mAdapterCategory.clear();
            mAdapterCategory.addAll(album.getCategories());
            mAdapterCategory.notifyDataSetChanged();

            mAdapterPhotographer.clear();
            mAdapterPhotographer.addAll(album.getPersons());
            mAdapterPhotographer.notifyDataSetChanged();

            mAdapterDate.clear();
            mAdapterDate.addAll(album.getDates());
            mAdapterDate.notifyDataSetChanged();

            mAdapterUpload.clear();
            mAdapterUpload.addAll(album.getUploadDates());
            mAdapterUpload.notifyDataSetChanged();

            mImageAdapter.clear();
            mImageAdapter.addAll(images);
            mImageAdapter.notifyDataSetChanged();

            // apply filters to ui
            if (mFilter != null && !mFilter.isEmpty()) {
                // category
                String category = mFilter.getCategory();
                mBinding.albumCategoryCheckBox.setChecked(category != null);
                if (category != null) {
                    int index = album.getCategories().indexOf(category);
                    if (index != -1) {
                        mBinding.albumCategorySpinner.setSelection(index);
                    } else {
                        mAdapterCategory.add(category);
                        mBinding.albumCategorySpinner.setSelection(album.getCategories().size());
                    }
                }

                // owner
                Person owner = mFilter.getOwner();
                mBinding.albumPhotographerCheckBox.setChecked(owner != null);
                if (owner != null) {
                    int index = album.getPersons().indexOf(owner);
                    if (index != -1) {
                        mBinding.albumPhotographerSpinner.setSelection(index);
                    } else {
                        mAdapterPhotographer.add(owner);
                        mBinding.albumPhotographerSpinner.setSelection(album.getPersons().size());
                    }
                }

                // day
                LocalDate day = mFilter.getDay();
                mBinding.albumDateCheckBox.setChecked(day != null);
                if (day != null) {
                    int index = album.getDates().indexOf(day);
                    if (index != -1) {
                        mBinding.albumDateSpinner.setSelection(index);
                    } else {
                        mAdapterDate.add(day);
                        mBinding.albumDateSpinner.setSelection(album.getDates().size());
                    }
                }

                // upload
                LocalDate upload = mFilter.getUpload();
                mBinding.albumUploadCheckBox.setChecked(upload != null);
                if (upload != null) {
                    int index = album.getUploadDates().indexOf(upload);
                    if (index != -1) {
                        mBinding.albumUploadSpinner.setSelection(index);
                    } else {
                        mAdapterUpload.add(upload);
                        mBinding.albumUploadSpinner.setSelection(album.getUploadDates().size());
                    }
                }
            }
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
        if (item.getItemId() == R.id.album_info) {
            var action = AlbumFragmentDirections.showAlbumInfo(mAlbum);
            Navigation.findNavController(mBinding.getRoot()).navigate(action);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_album, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
        } else if (id == R.id.album_upload_check_box) {
            mBinding.albumUploadSpinner.setEnabled(isChecked);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Image image = mImageAdapter.getItem(position);

        if (image == null) {
            Snackbar.make(mBinding.getRoot(), R.string.image_not_downloaded, Snackbar.LENGTH_SHORT).show();
            return;
        }

        NavHostFragment.findNavController(this)
                       .navigate(AlbumFragmentDirections.showImage(image, mAlbum));
    }

    private Pair<Album, Filter> parseIntent(Intent intent) {
        if (intent == null) return null;

        Uri uri = intent.getData();
        if (uri == null) return null;

        Album album = null;

        Filter.Builder builder = Filter.builder();

        String idStr = uri.getQueryParameter("albumid");
        String dateStr = uri.getQueryParameter("byday");
        String uploadStr = uri.getQueryParameter("byupload");
        String ownerStr = uri.getQueryParameter("byowner");
        String categoryStr = uri.getQueryParameter("bycategory");

        if (idStr != null) {
            try {
                long id = Long.parseLong(idStr);
                album = new Album(id);
            } catch (NumberFormatException ignored) {}
        }

        if (dateStr != null) {
            try {
                LocalDate date = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(dateStr));
                builder.setDay(date);
            } catch (DateTimeParseException ignored) {}
        }

        if (uploadStr != null) {
            try {
                LocalDate date = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(uploadStr));
                builder.setUpload(date);
            } catch (DateTimeParseException ignored) {}
        }

        if (ownerStr != null) {
            try {
                long owner = Long.parseLong(ownerStr);
                Person person = new Person(owner);
                person.setUsername(getString(R.string.album_photographer_unknown));
                builder.setOwner(person);
            } catch (NumberFormatException ignored) {}
        }

        if (categoryStr != null) {
            if ("".equals(categoryStr)) {
                builder.setCategory(Album.CATEGORY_ETC);
            } else {
                try {
                    builder.setCategory(URLDecoder.decode(categoryStr, "UTF-8"));
                } catch (UnsupportedEncodingException ignored) {}
            }
        }

        if (album == null) {
            return null;
        } else {
            return Pair.create(album, builder.build());
        }
    }
}
