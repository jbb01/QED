package com.jonahbauer.qed.activities.mainFragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.SharedElementCallback;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.transition.*;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.transition.Hold;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.imageActivity.ImageViewHolder;
import com.jonahbauer.qed.databinding.FragmentImageBinding;
import com.jonahbauer.qed.layoutStuff.transition.ActionBarAnimation;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ImageFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    private static final String LOG_TAG = ImageFragment.class.getSimpleName();
    private static final @StyleRes int THEME = R.style.Theme_App_Dark;

    private static final String SAVED_EXTENDED = "extended";

    private FragmentImageBinding mBinding;

    private Image mImage; // currently shown image
    private ImageViewHolder mImageViewHolder; // currently show view holder
    private LiveData<StatusWrapper<Image>> mImageStatus; // currently shown status

    private ImageAdapter mImageAdapter;
    private final Observer<StatusWrapper<Image>> mStatusObserver = this::updateOverlay;

    private MenuItem mOpenWithButton;
    private MenuItem mDownloadButton;
    private MenuItem mInfoButton;

    private boolean mExtended = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mImageAdapter = new ImageAdapter();

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(NavController.KEY_DEEP_LINK_INTENT)) {
                Intent intent = (Intent) args.get(NavController.KEY_DEEP_LINK_INTENT);
                Image image = parseIntent(intent);
                if (image != null) {
                    mImage = image;
                    mImageAdapter.submitList(Collections.singletonList(image));
                }
            }

            if (mImage == null) {
                ImageFragmentArgs imageArgs = ImageFragmentArgs.fromBundle(args);
                mImage = imageArgs.getImage();
                mImageAdapter.submitList(imageArgs.getAlbum().getImages());
            }
        }

        if (mImage == null) {
            mImage = new Image(0);
            mImageAdapter.submitList(Collections.singletonList(mImage));
        }

        setupTransitions();

        if (savedInstanceState != null) {
            mExtended = savedInstanceState.getBoolean(SAVED_EXTENDED);
        }
    }

    private void setupTransitions() {
        var duration = TransitionUtils.getTransitionDuration(this);

        var sharedEnterTransition = (TransitionSet) TransitionInflater.from(requireContext()).inflateTransition(R.transition.image_fragment_enter);
        sharedEnterTransition.addListener(new ActionBarAnimation(this, Color.BLACK, false));
        sharedEnterTransition.setDuration(duration);
        setSharedElementEnterTransition(sharedEnterTransition);

        var enterTransition = new Hold();
        enterTransition.addListener(new ActionBarAnimation(this, Color.BLACK, false));
        enterTransition.setDuration(duration);
        setEnterTransition(enterTransition);

        var returnTransition = (TransitionSet) TransitionInflater.from(requireContext()).inflateTransition(R.transition.image_fragment_return);
        returnTransition.setDuration(duration);
        returnTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {
                ViewUtils.resetTransparentSystemBars(ImageFragment.this);
            }
        });
        setSharedElementReturnTransition(returnTransition);
        setReturnTransition(new Fade());

        var exitTransition = new Fade(Fade.MODE_OUT);
        exitTransition.setDuration(duration);
        setExitTransition(exitTransition);

        var reenterTransition = new Hold();
        reenterTransition.setDuration(duration);
        reenterTransition.addListener(new ActionBarAnimation(this, Color.BLACK, false));
        setReenterTransition(reenterTransition);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var context = requireContext();
        inflater = inflater.cloneInContext(context);

        ViewUtils.setTransparentSystemBars(this);

        mBinding = FragmentImageBinding.inflate(inflater, container, false);

        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        MenuInflater menuInflater = new MenuInflater(context);
        Menu menu = mBinding.toolbar.getMenu();
        menuInflater.inflate(R.menu.menu_image, menu);
        mOpenWithButton = menu.findItem(R.id.image_open_with);
        mDownloadButton = menu.findItem(R.id.image_download_original);
        mInfoButton = menu.findItem(R.id.image_info);
        mBinding.toolbar.setVisibility(View.GONE);

        setExtended(mExtended);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.fragment, (v, insets) -> {
            var systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            mBinding.toolbar.setPadding(
                    systemInsets.left,
                    systemInsets.top,
                    systemInsets.right,
                    0
            );
            mBinding.overlayBottom.setPadding(
                    systemInsets.left,
                    (int) ViewUtils.dpToPx(v, 24),
                    systemInsets.right,
                    (int) (ViewUtils.dpToPx(v, 24) + systemInsets.bottom)
            );
            return WindowInsetsCompat.CONSUMED;
        });
        postponeEnterTransition(500, TimeUnit.MILLISECONDS);
        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                super.onMapSharedElements(names, sharedElements);

                var imageTransitionName = getString(R.string.transition_name_image_fragment_image);
                if (!names.contains(imageTransitionName)) return;

                var viewHolder = mImageAdapter.getViewHolderByPosition(mBinding.viewPager.getCurrentItem());
                if (viewHolder == null) return;

                var itemView = viewHolder.itemView;
                var target = itemView.findViewById(R.id.gallery_image);
                target.setTransitionName(imageTransitionName);
                sharedElements.put(imageTransitionName, target);
            }
        });

        // prepare view pager
        mBinding.viewPager.setOnClickListener(v -> toggleExtended());
        mBinding.viewPager.setAdapter(mImageAdapter);

        // page change listener
        ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                ImageFragment.this.onPageSelected(position);
            }
        };
        mBinding.viewPager.registerOnPageChangeCallback(pageChangeCallback);

        // prepare toolbar
        mBinding.toolbar.setNavigationOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });
        mBinding.toolbar.setOnMenuItemClickListener(this);

        // select image
        if (mImage != null) {
            int index = mImageAdapter.getCurrentList().indexOf(mImage);
            if (index != -1) {
                mBinding.viewPager.setCurrentItem(index, false);
            }
            mBinding.viewPager.postDelayed(() -> pageChangeCallback.onPageSelected(index != -1 ? index : 0), 50);
        }
    }

    @Override
    public void onResume() {
        setExtended(mExtended);
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_EXTENDED, mExtended);
    }

    @Override
    public void onDestroyView() {
        var activity = (MainActivity) requireActivity();
        var controller = activity.getWindowInsetsController();
        controller.show(WindowInsetsCompat.Type.systemBars());
        ViewUtils.resetTransparentSystemBars(this);
        super.onDestroyView();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.image_download_original) {
            if (Preferences.gallery().isOfflineMode()) {
                Toast.makeText(requireContext(), R.string.offline_mode_not_available, Toast.LENGTH_SHORT).show();
                return true;
            }

            int position = mBinding.viewPager.getCurrentItem();
            if (!mImage.isOriginal()) {
                downloadOriginal(position);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(R.string.image_already_original);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> downloadOriginal(position));
                builder.setNegativeButton(R.string.no, (dialog, which) -> {});
                builder.show();
            }
            return true;
        } else if (id == R.id.image_info) {
            var action = ImageFragmentDirections.showImageInfo(mImage);
            NavHostFragment.findNavController(this).navigate(action);
            return true;
        } else if (id == R.id.image_open_with) {
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.jonahbauer.qed.fileprovider", new File(mImage.getPath()));

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, mImage.getFormat());

            Activity activity = requireActivity();
            List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(intent);
            return true;
        }
        return false;
    }

    private void updateOverlay(StatusWrapper<Image> statusWrapper) {
        Image image = statusWrapper.getValue();
        int code = statusWrapper.getCode();

        if (image != null) {
            mBinding.setTitle(image.getName());
        }

        if (statusWrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
            startPostponedEnterTransition();
        }

        if (code == StatusWrapper.STATUS_ERROR) {
            mBinding.setError(getResources().getString(statusWrapper.getReason().getStringRes()));
        } else {
            mBinding.setError(null);
        }

        // update buttons
        mOpenWithButton.setEnabled(image != null && image.getPath() != null);
        mDownloadButton.setEnabled(image != null);
        mInfoButton.setEnabled(image != null);
    }

    private void onPageSelected(int position) {
        // remove old listeners
        if (mImageStatus != null) {
            mImageStatus.removeObserver(mStatusObserver);
        }
        if (mImageViewHolder != null) {
            mImageViewHolder.onVisibilityChange(false);
        }

        // add new listeners
        ImageViewHolder viewHolder = mImageAdapter.getViewHolderByPosition(position);
        if (viewHolder != null) {
            mImageStatus = viewHolder.getStatus();
            mImageStatus.observe(getViewLifecycleOwner(), mStatusObserver);

            mImageViewHolder = viewHolder;
            mImageViewHolder.onVisibilityChange(true);
        }

        mImage = mImageAdapter.getCurrentList().get(position);

        // save image id on back stack
        try {
            NavHostFragment.findNavController(this)
                           .getBackStackEntry(R.id.nav_album)
                           .getSavedStateHandle()
                           .set(AlbumFragment.IMAGE_ID_KEY, mImage.getId());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not save image id on backstack.", e);
        }
    }

    private void toggleExtended() { setExtended(!mExtended); }

    private void setExtended(boolean extended) {
        this.mExtended = extended;
        mBinding.setExtended(extended);

        var controller = ((MainActivity) requireActivity()).getWindowInsetsController();
        if (controller != null) {
            if (extended) {
                controller.show(WindowInsetsCompat.Type.systemBars());
            } else {
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
                controller.hide(WindowInsetsCompat.Type.systemBars());
            }
        }
    }

    private void downloadOriginal(int position) {
        ImageViewHolder viewHolder = mImageAdapter.getViewHolderByPosition(position);
        viewHolder.downloadOriginal();
    }

    private Image parseIntent(Intent intent) {
        if (intent == null) return null;

        Uri uri = intent.getData();
        if (uri == null) return null;

        String idStr = uri.getQueryParameter("imageid");
        if (idStr != null) {
            try {
                long id = Long.parseLong(idStr);
                return new Image(id);
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    @Nullable
    @Override
    public Context getContext() {
        var context = super.getContext();
        if (context == null) return null;

        return new ContextThemeWrapper(context, THEME);
    }

    private class ImageAdapter extends ListAdapter<Image, ImageViewHolder> {
        private final Int2ObjectMap<SoftReference<ImageViewHolder>> mViewHolderCache = new Int2ObjectOpenHashMap<>();

        protected ImageAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Image oldItem, @NonNull Image newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Image oldItem, @NonNull Image newItem) {
                    return oldItem.getId() == newItem.getId();
                }
            });
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageViewHolder viewHolder = new ImageViewHolder(LayoutInflater.from(parent.getContext()));
            viewHolder.setOnClickListener(v -> toggleExtended());
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Image image = getItem(position);
            holder.load(image);
            mViewHolderCache.put(position, new SoftReference<>(holder));
        }

        public ImageViewHolder getViewHolderByPosition(int position) {
            SoftReference<ImageViewHolder> ref = mViewHolderCache.get(position);
            if (ref != null) {
                if (ref.get() != null) {
                    return ref.get();
                } else {
                    mViewHolderCache.remove(position, ref);
                }
            }

            return null;
        }
    }
}
