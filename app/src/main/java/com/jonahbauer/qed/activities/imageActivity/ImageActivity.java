package com.jonahbauer.qed.activities.imageActivity;

import static com.jonahbauer.qed.activities.DeepLinkingActivity.QEDIntent;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.ImageInfoActivity;
import com.jonahbauer.qed.databinding.ActivityImageBinding;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ImageActivity extends AppCompatActivity {
    public static final String GALLERY_IMAGE_KEY = "galleryImage";
    public static final String GALLERY_IMAGES_KEY = "galleryImages";

    private Image mImage;
    private ActivityImageBinding mBinding;

    private ActionBar mActionBar;

    private View mWindowDecor;
    private Window mWindow;
    private MenuItem mOpenWithButton;
    private MenuItem mDownloadButton;
    private MenuItem mInfoButton;

    private boolean mExtended = false;

    private ImageAdapter mAdapter;
    private ViewPager2 mViewPager;

    private ImageViewHolder mImageViewHolder;
    private LiveData<StatusWrapper<Image>> mImageStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        mAdapter = new ImageAdapter();

        onNewIntent(intent);
        if (isFinishing()) return;

        mBinding = ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mWindow = getWindow();
        mWindowDecor = mWindow.getDecorView();
        mWindowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Toolbar toolbar = mBinding.toolbar;
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle("");
        mActionBar.hide();

        mViewPager = mBinding.galleryImageViewPager;
        mViewPager.setAdapter(mAdapter);

        int index = -1;
        if (mImage != null) {
            index = mAdapter.getCurrentList().indexOf(mImage);

            if (index != -1) {
                mViewPager.setCurrentItem(index, false);
            }
        }

        ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                ImageViewHolder viewHolder = mAdapter.getViewHolderByPosition(position);
                if (mImageStatus != null) {
                    mImageStatus.removeObservers(ImageActivity.this);
                }
                if (mImageViewHolder != null) {
                    mImageViewHolder.onVisibilityChange(false);
                }

                if (viewHolder != null) {
                    mImageStatus = viewHolder.getStatus();
                    mImageStatus.observe(ImageActivity.this, statusWrapper -> {
                        mBinding.setStatus(statusWrapper);

                        boolean ready = statusWrapper != null && statusWrapper.getCode() == StatusWrapper.STATUS_LOADED;
                        if (mDownloadButton != null) mDownloadButton.setEnabled(ready);
                        if (mInfoButton != null) mInfoButton.setEnabled(ready);
                        if (mOpenWithButton != null) mOpenWithButton.setEnabled(ready);
                    });

                    mImageViewHolder = viewHolder;
                    mImageViewHolder.onVisibilityChange(true);
                }

                mImage = mAdapter.getCurrentList().get(position);
            }
        };

        mViewPager.registerOnPageChangeCallback(pageChangeCallback);

        final int ind = index;
        mViewPager.postDelayed(() -> pageChangeCallback.onPageSelected(ind != -1 ? ind : 0), 50);
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeExtended(mExtended);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        mDownloadButton = menu.getItem(0);
        mOpenWithButton = menu.getItem(1);
        mInfoButton = menu.getItem(2);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.image_download_original) {
            if (Preferences.gallery().isOfflineMode()) {
                Toast.makeText(this, R.string.offline_mode_not_available, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (!mImage.isOriginal()) {
                downloadOriginal();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_already_original);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> downloadOriginal());
                builder.setNegativeButton(R.string.no, (dialog, which) -> {});
                builder.show();
            }
            return true;
        } else if (itemId == R.id.image_open_with) {
            Uri uri = FileProvider.getUriForFile(this, "com.jonahbauer.qed.fileprovider", new File(mImage.getPath()));

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, mImage.getFormat());

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(intent);
            return true;
        } else if (itemId == R.id.image_info) {
            Intent intent = new Intent(this, ImageInfoActivity.class);
            intent.putExtra(ImageInfoActivity.EXTRA_IMAGE, mImage);
            startActivity(intent);
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    /**
     * Prompt when downloads are running and change transition to fade
     */
    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(GALLERY_IMAGE_KEY, mImage);
        setResult(RESULT_OK, intent);

        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * show/hide overlay with buttons
     */
    private void changeExtended() {changeExtended(!mExtended);}
    private void changeExtended(boolean extended) {
        this.mExtended = extended;
        if (extended) {
            mWindowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            mActionBar.show();
            mBinding.setExtended(true);
        } else {
            mWindowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            mActionBar.hide();
            mBinding.setExtended(false);
        }

        mWindow.setStatusBarColor(Color.TRANSPARENT);
    }

    private void downloadOriginal() {
        int currentPosition = mViewPager.getCurrentItem();
        ImageViewHolder viewHolder = mAdapter.getViewHolderByPosition(currentPosition);
        viewHolder.downloadOriginal();
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

    public static boolean handleIntent(@NonNull Intent intent, @Nullable ImageActivity imageActivity) {
        Image image = intent.getParcelableExtra(GALLERY_IMAGE_KEY);
        ArrayList<Image> images = intent.getParcelableArrayListExtra(GALLERY_IMAGES_KEY);

        // internal intent
        if (image != null || images != null) {
            if (imageActivity != null) {
                if (image != null) {
                    imageActivity.mImage = image;
                } else {
                    imageActivity.mImage = images.get(0);
                }

                if (images != null && !images.isEmpty()) {
                    imageActivity.mAdapter.submitList(Collections.unmodifiableList(images));
                } else {
                    imageActivity.mAdapter.submitList(Collections.singletonList(image));
                }
            }

            return true;
        }

        // external intent via deep link
        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW_IMAGE.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data != null) {
                String host = data.getHost();
                if (host == null || !host.equals("qedgallery.qed-verein.de")) return false;

                String path = data.getPath();
                if (path == null || !path.startsWith("/image_view.php")) return false;

                String idStr = data.getQueryParameter("imageid");
                if (idStr == null) return false;

                try {
                    long id = Long.parseLong(idStr);

                    if (imageActivity != null) {
                        imageActivity.mImage = new Image(id);
                        imageActivity.mAdapter.submitList(Collections.singletonList(imageActivity.mImage));
                    }

                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * @return the type of the resource ("image", "video", "audio"). if no type is specified "image" will be used as a standard
     */
    static String getType(@NonNull Image image) {
        String type = "image";
        if (image.getFormat() != null) {
            type = image.getFormat().split("/")[0];
        } else if (image.getName() != null) {
            String suffix = image.getName();
            suffix = suffix.substring(suffix.lastIndexOf('.') + 1);

            if (Image.VIDEO_FILE_EXTENSIONS.contains(suffix)) {
                type = "video";
            } else if (Image.AUDIO_FILE_EXTENSIONS.contains(suffix)) {
                type = "audio";
            }
        }

        return type;
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
            viewHolder.setOnClickListener(v -> changeExtended());
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Image image = getItem(position);
            holder.reset(image);
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
