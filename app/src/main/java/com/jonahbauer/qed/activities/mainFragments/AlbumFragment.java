package com.jonahbauer.qed.activities.mainFragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.imageActivity.ImageActivity;
import com.jonahbauer.qed.activities.imageActivity.ImageFragmentArgs;
import com.jonahbauer.qed.databinding.FragmentAlbumBinding;
import com.jonahbauer.qed.layoutStuff.CustomArrayAdapter;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.AlbumFilter;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.adapter.ImageAdapter;
import com.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AlbumFragment extends Fragment implements AdapterView.OnItemClickListener, OnActivityReenterListener {
    private static final String SAVED_SELECTED_ITEM_ID = "selectedItemId";

    private AlbumViewModel mAlbumViewModel;
    private FragmentAlbumBinding mBinding;

    private ImageAdapter mImageAdapter;
    private boolean mDummiesLoaded = false;
    private CustomArrayAdapter<String> mAdapterCategory;
    private CustomArrayAdapter<LocalDate> mAdapterDate;
    private CustomArrayAdapter<LocalDate> mAdapterUpload;
    private CustomArrayAdapter<Person> mAdapterPhotographer;

    private Long mSelectedItemId;
    private ActivityResultLauncher<Intent> mImageActivityLauncher;

    private boolean mShouldApplyFiltersToView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle arguments = getArguments();

        AlbumFragmentArgs args = AlbumFragmentArgs.fromBundle(arguments);
        Album album = args.getAlbum();
        if (album == null) album = new Album(args.getId());

        AlbumFilter filter = null;
        if (savedInstanceState == null && arguments != null && arguments.containsKey(NavController.KEY_DEEP_LINK_INTENT)) {
            Intent intent = (Intent) arguments.get(NavController.KEY_DEEP_LINK_INTENT);
            filter = parseFilters(intent);
            mShouldApplyFiltersToView = true;
        }

        mAlbumViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_album).get(AlbumViewModel.class);
        if (filter == null) mAlbumViewModel.load(album);
        else mAlbumViewModel.load(album, filter);

        TransitionUtils.setupDefaultTransitions(this);
        TransitionUtils.setupEnterContainerTransform(this, Colors.getPrimaryColor(requireContext()));

        mImageActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {});

        if (savedInstanceState != null) {
            mSelectedItemId = savedInstanceState.containsKey(SAVED_SELECTED_ITEM_ID)
                    ? savedInstanceState.getLong(SAVED_SELECTED_ITEM_ID)
                    : null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentAlbumBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        postponeEnterTransition(200, TimeUnit.MILLISECONDS);

        mImageAdapter = new ImageAdapter(requireContext(), new ArrayList<>());
        mBinding.imageContainer.setAdapter(mImageAdapter);
        mBinding.imageContainer.setOnItemClickListener(this);
        mBinding.imageContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mBinding.imageContainer.removeOnLayoutChangeListener(this);

                // scroll selected image into view
                var selectedImageId = getSelectedItemId();
                if (selectedImageId != null) scrollToId(selectedImageId);
            }
        });

        ViewUtils.setupExpandable(mBinding.expandCheckBox, mBinding.expandable, mAlbumViewModel.getExpanded());
        ViewUtils.link(mBinding.albumPhotographerCheckBox, mBinding.albumPhotographerSpinner);
        ViewUtils.link(mBinding.albumDateCheckBox, mBinding.albumDateSpinner);
        ViewUtils.link(mBinding.albumUploadCheckBox, mBinding.albumUploadSpinner);
        ViewUtils.link(mBinding.albumCategoryCheckBox, mBinding.albumCategorySpinner);

        mAdapterCategory = setupSpinner(mBinding.albumCategorySpinner, Album::decodeCategory);
        mAdapterPhotographer = setupSpinner(mBinding.albumPhotographerSpinner, Person::getUsername);
        mAdapterDate = setupSpinner(mBinding.albumDateSpinner, TimeUtils::format);
        mAdapterUpload = setupSpinner(mBinding.albumUploadSpinner, TimeUtils::format);

        mBinding.searchButton.setOnClickListener(v -> search());

        mBinding.setOnOfflineClick(v -> {
            if (Preferences.gallery().isOfflineMode()) {
                Preferences.gallery().edit().setOfflineMode(false).apply();
                mImageAdapter.setOfflineMode(false);
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
    public void onActivityReenter(int resultCode, Intent data) {
        if (resultCode == ImageActivity.RESULT_OK && data != null) {
            var id = data.getLongExtra(ImageActivity.RESULT_EXTRA_IMAGE_ID, -1L);
            if (id != -1) {
                mSelectedItemId = id;

                var activity = requireActivity();
                activity.setExitSharedElementCallback(new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        var selectedImageId = getSelectedItemId();
                        if (selectedImageId != null) {
                            var itemView = mBinding.imageContainer.findViewWithTag(selectedImageId);
                            if (itemView != null) {
                                var thumbnail = itemView.findViewById(R.id.thumbnail);
                                sharedElements.put(names.get(0), thumbnail);
                            }
                        }

                        activity.setExitSharedElementCallback((SharedElementCallback) null);
                    }
                });

                if (scrollToId(id)) {
                    TransitionUtils.postponeEnterTransition(activity, 200);
                    OneShotPreDrawListener.add(mBinding.imageContainer, activity::startPostponedEnterTransition);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedItemId != null) {
            outState.putLong(SAVED_SELECTED_ITEM_ID, mSelectedItemId);
        }
    }

    private @NonNull AlbumFilter loadFilters() {
        LocalDate day = null;
        LocalDate upload = null;
        Long owner = null;
        String category = null;

        if (mBinding.albumDateCheckBox.isChecked()) {
            day = (LocalDate) mBinding.albumDateSpinner.getSelectedItem();
        }
        if (mBinding.albumUploadCheckBox.isChecked()) {
            upload = (LocalDate) mBinding.albumUploadSpinner.getSelectedItem();
        }
        if (mBinding.albumPhotographerCheckBox.isChecked()) {
            var person = (Person) mBinding.albumPhotographerSpinner.getSelectedItem();
            owner = person == null ? null : person.getId();
        }
        if (mBinding.albumCategoryCheckBox.isChecked()) {
            category = (String) mBinding.albumCategorySpinner.getSelectedItem();
        }

        return new AlbumFilter(day, upload, owner, category);
    }

    private void search() {
        var filter = loadFilters();
        mAlbumViewModel.filter(filter);
    }

    private void updateView(StatusWrapper<Album> albumStatusWrapper) {
        Album album = albumStatusWrapper.getValue();
        List<Image> images = album != null ? album.getImages() : Collections.emptyList();

        mBinding.setAlbum(album);
        mBinding.setStatus(albumStatusWrapper.getCode());

        if (album != null && album.getName() != null) {
            ViewUtils.setActionBarText(this, album.getName());
        } else {
            ViewUtils.setActionBarText(this, getString(R.string.title_fragment_album));
        }

        if (album != null) {
            updateFilters(album);
            if (mShouldApplyFiltersToView) updateFilterValues(album, mAlbumViewModel.getFilterValue());
        }

        if (albumStatusWrapper.getCode() == StatusWrapper.STATUS_LOADED) {
            mImageAdapter.clear();
            mImageAdapter.addAll(images);
            mImageAdapter.notifyDataSetChanged();
            mDummiesLoaded = false;
        } else if (!mDummiesLoaded) {
            // by using dummy images while the album is not yet loaded the lag spike
            // caused by view inflation for incoming images during fragment transition is reduced
            mImageAdapter.clear();
            mImageAdapter.addAll(Collections.nCopies(20, null));
            mImageAdapter.notifyDataSetChanged();
            mDummiesLoaded = true;
        }

        if (albumStatusWrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
            mShouldApplyFiltersToView = false;
            OneShotPreDrawListener.add(mBinding.imageContainer, this::startPostponedEnterTransition);
        }

        if (albumStatusWrapper.getCode() == StatusWrapper.STATUS_ERROR) {
            mImageAdapter.clear();
            Reason reason = albumStatusWrapper.getReason();
            mBinding.setError(getString(reason == Reason.EMPTY ? R.string.album_empty : reason.getStringRes()));
        }

        int hits = mDummiesLoaded ? 0 : mImageAdapter.getImages().size();
        if (hits > 0) {
            mBinding.setHits(getString(R.string.hits, hits));
        } else {
            mBinding.setHits("");
        }
    }

    private void updateFilters(@NonNull Album album) {
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
    }

    private void updateFilterValues(@NonNull Album album, @NonNull AlbumFilter filter) {
        updateFilterValue(
                mBinding.albumCategoryCheckBox, mBinding.albumCategorySpinner, mAdapterCategory,
                album.getCategories(), filter.getCategory()
        );

        var ownerId = filter.getOwner();
        Person owner;
        if (ownerId != null) {
            owner = new Person(ownerId);
            owner.setUsername(getString(R.string.album_photographer_unknown));
        } else {
            owner = null;
        }
        updateFilterValue(
                mBinding.albumPhotographerCheckBox, mBinding.albumPhotographerSpinner, mAdapterPhotographer,
                album.getPersons(), owner
        );

        updateFilterValue(
                mBinding.albumDateCheckBox, mBinding.albumDateSpinner, mAdapterDate,
                album.getDates(), filter.getDay()
        );

        updateFilterValue(
                mBinding.albumUploadCheckBox, mBinding.albumUploadSpinner, mAdapterUpload,
                album.getUploadDates(), filter.getUpload()
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.album_info) {
            var album = mAlbumViewModel.getAlbumValue();
            if (album.getId() != Album.NO_ID) {
                var action = AlbumFragmentDirections.showAlbumInfo(album);
                Navigation.findNavController(mBinding.getRoot()).navigate(action);
            }
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Image image = mImageAdapter.getItem(position);

        if (image == null) {
            Snackbar.make(mBinding.getRoot(), R.string.image_not_downloaded, Snackbar.LENGTH_SHORT).show();
            return;
        }

        mSelectedItemId = id;

        var activity = requireActivity();
        var args = new ImageFragmentArgs.Builder(image.getId())
                .setImage(image)
                .setImageList(mImageAdapter.getImages().toArray(new Image[0]))
                .build().toBundle();
        var intent = new Intent(activity, ImageActivity.class);
        intent.putExtras(args);
        var options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view.findViewById(R.id.thumbnail), getString(R.string.transition_name_image_fragment_image));
        mImageActivityLauncher.launch(intent, options);
    }

    private Long getSelectedItemId() {
        return mSelectedItemId;
    }

    //<editor-fold desc="Utility Functions" defaultstate="collapsed">
    private static @NonNull AlbumFilter parseFilters(Intent intent) {
        if (intent == null) return AlbumFilter.EMPTY;
        return AlbumFilter.parse(intent.getData());
    }

    private void adjustColumnCount(@NonNull Configuration configuration) {
        double width = configuration.screenWidthDp;
        int columnCount = Double.valueOf(Math.round(width / 150d)).intValue();

        mBinding.imageContainer.setNumColumns(columnCount);
    }

    private boolean scrollToId(long id) {
        // scroll selected image into view
        var selectedView = mBinding.imageContainer.findViewWithTag(id);
        if (selectedView == null) {
            var position = mImageAdapter.getImages().indexOf(new Image(id));
            mBinding.imageContainer.setSelection(position);
            return true;
        } else if (selectedView.getTop() < 0) {
            mBinding.imageContainer.scrollListBy(selectedView.getTop());
            return true;
        } else if (selectedView.getBottom() > mBinding.imageContainer.getHeight()) {
            mBinding.imageContainer.scrollListBy(- mBinding.imageContainer.getHeight() + selectedView.getBottom());
            return true;
        }
        return false;
    }

    private <T> CustomArrayAdapter<T> setupSpinner(@NonNull Spinner spinner, @NonNull Function<T, CharSequence> toString) {
        var adapter = new CustomArrayAdapter<T>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        adapter.setToString(toString);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinner.setAdapter(adapter);
        spinner.setEnabled(false);
        return adapter;
    }

    private <T> void updateFilterValue(CheckBox checkBox, Spinner spinner, ArrayAdapter<T> adapter, List<T> data, T value) {
        checkBox.setChecked(value != null);
        if (value != null) {
            int index = data.indexOf(value);
            if (index != -1) {
                spinner.setSelection(index);
            } else {
                adapter.add(value);
                spinner.setSelection(data.size());
            }
        }
    }
    //</editor-fold>
}
