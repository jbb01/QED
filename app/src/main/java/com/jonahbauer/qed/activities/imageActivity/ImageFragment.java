package com.jonahbauer.qed.activities.imageActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.transition.Fade;
import androidx.viewpager2.widget.ViewPager2;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentImageBinding;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.util.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;

public class ImageFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
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

        ImageFragmentArgs args = ImageFragmentArgs.fromBundle(getArguments());
        mImage = args.getImage();
        if (mImage == null) mImage = new Image(args.getId());

        var imageList = args.getImageList();
        if (imageList != null && imageList.length > 0) {
            mImageAdapter.submitList(ObjectList.of(imageList));
        } else {
            mImageAdapter.submitList(Collections.singletonList(mImage));
        }

        if (savedInstanceState != null) {
            mExtended = savedInstanceState.getBoolean(SAVED_EXTENDED);
        }

        var transition = new Fade();
        transition.setDuration(TransitionUtils.getTransitionDuration(this));
        setExitTransition(transition);
        setReenterTransition(transition);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewUtils.setTransparentSystemBars(this);

        mBinding = FragmentImageBinding.inflate(inflater, container, false);

        var menu = mBinding.toolbar.getMenu();
        mOpenWithButton = menu.findItem(R.id.image_open_with);
        mDownloadButton = menu.findItem(R.id.image_download_original);
        mInfoButton = menu.findItem(R.id.image_info);

        setExtended(mExtended);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.fragment, (v, windowInsets) -> {
            var mask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            var insets = windowInsets.getInsets(mask);

            mBinding.toolbar.setPadding(insets.left, insets.top, insets.right, 0);

            var params = mBinding.toolbar.getLayoutParams();
            params.height = (int) (ViewUtils.getActionBarSize(mBinding.toolbar.getContext()) + insets.top);
            mBinding.toolbar.setLayoutParams(params);

            mBinding.overlayBottom.setPadding(
                    insets.left,
                    (int) ViewUtils.dpToPx(v, 24),
                    insets.right,
                    (int) (ViewUtils.dpToPx(v, 24) + insets.bottom)
            );
            return WindowInsetsCompat.CONSUMED;
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
        mBinding.toolbar.setNavigationOnClickListener(v -> requireActivity().finishAfterTransition());
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
        var activity = (ImageActivity) requireActivity();
        var controller = activity.getWindowInsetsController();
        controller.show(WindowInsetsCompat.Type.systemBars());
        ViewUtils.resetTransparentSystemBars(this);
        super.onDestroyView();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.image_download_original) {
            if (Preferences.getGallery().isOfflineMode()) {
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
            Actions.openContent(requireContext(), uri, mImage.getFormat());
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
            requireActivity().startPostponedEnterTransition();
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

        // save image id
        var activity = getActivity();
        if (activity instanceof ImageActivity) {
            ((ImageActivity) activity).setResultImageId(mImage.getId());
        }
    }

    private void toggleExtended() { setExtended(!mExtended); }

    private void setExtended(boolean extended) {
        this.mExtended = extended;
        mBinding.setExtended(extended);

        var controller = ((ImageActivity) requireActivity()).getWindowInsetsController();
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
