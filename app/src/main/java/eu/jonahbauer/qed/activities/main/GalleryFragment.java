package eu.jonahbauer.qed.activities.main;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import com.google.android.material.snackbar.Snackbar;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.FragmentGalleryBinding;
import eu.jonahbauer.qed.model.Album;
import eu.jonahbauer.qed.ui.adapter.AlbumAdapter;
import eu.jonahbauer.qed.model.viewmodel.AlbumListViewModel;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.StatusWrapper;
import eu.jonahbauer.qed.util.TransitionUtils;
import eu.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;

public class GalleryFragment extends Fragment implements AdapterView.OnItemClickListener, MenuProvider {
    private AlbumAdapter mAlbumAdapter;
    private FragmentGalleryBinding mBinding;
    private MenuItem mRefresh;

    private AlbumListViewModel mAlbumListViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentGalleryBinding.inflate(inflater, container, false);
        mAlbumListViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_gallery).get(AlbumListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TransitionUtils.postponeEnterAnimationToPreDraw(this, view);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        mAlbumAdapter = new AlbumAdapter(getContext(), new ArrayList<>());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setAdapter(mAlbumAdapter);

        mBinding.setOnOfflineClick(v -> {
            if (Preferences.getGallery().isOfflineMode()) {
                Preferences.getGallery().setOfflineMode(false);
            }
            mAlbumListViewModel.load();
        });

        mAlbumListViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            mBinding.setStatus(albums.getCode());

            if (mRefresh != null) {
                mRefresh.setEnabled(albums.getCode() != StatusWrapper.STATUS_PRELOADED);
            }

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

            mBinding.setForcedOfflineMode(Preferences.getGallery().isOfflineMode());
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album album = mAlbumAdapter.getItem(position);

        if (album == null) return;
        if (isOnline() || album.getLoaded() != null) {
            var extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(view, getString(R.string.transition_name_album_fragment))
                    .build();
            var action = GalleryFragmentDirections.showAlbum(album.getId());
            action.setAlbum(album);
            Navigation.findNavController(view).navigate(action, extras);

            TransitionUtils.setupReenterElevationScale(this);
        } else {
            Snackbar.make(mBinding.getRoot(), getString(R.string.album_not_downloaded), Snackbar.LENGTH_SHORT).show();
        }
    }

    private boolean isOnline() {
        Boolean offline = mAlbumListViewModel.getOffline().getValue();
        return offline != null && !offline;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_gallery, menu);
        mRefresh = menu.findItem(R.id.menu_refresh);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mAlbumListViewModel.load();

            Drawable icon = mRefresh.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            return true;
        }
        return false;
    }
}
