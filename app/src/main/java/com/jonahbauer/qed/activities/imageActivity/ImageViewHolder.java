package com.jonahbauer.qed.activities.imageActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ViewHolderImageBinding;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.NORMAL;
import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.ORIGINAL;

public class ImageViewHolder extends RecyclerView.ViewHolder implements QEDPageReceiver<Image>, QEDPageStreamReceiver<Image> {
    private static final String LOG_TAG = ImageViewHolder.class.getName();

    private final Context mContext;
    private final AlbumDao mAlbumDao;

    private final ViewHolderImageBinding mBinding;
    private final MutableLiveData<StatusWrapper<Image>> mStatus = new MutableLiveData<>();

    private QEDGalleryPages.Mode mMode = NORMAL;

    private File mTarget;
    private File mDownloadTmp;

    private boolean mVisible;
    private AlertDialog.Builder mPendingDialog;

    private final List<Disposable> mDisposables = new ArrayList<>();

    public ImageViewHolder(@NonNull LayoutInflater inflater) {
        super(createRoot(inflater));

        this.mContext = inflater.getContext();
        this.mAlbumDao = Database.getInstance(mContext.getApplicationContext()).albumDao();

        this.mBinding = ViewHolderImageBinding.inflate(inflater, (ViewGroup) this.itemView, true);
    }

    @NonNull
    private static View createRoot(@NonNull LayoutInflater inflater) {
        // Manipulate root layout to relay #requestDisallowInterceptTouchEvent (as sent by AdvancedImageView)
        // to ViewPager2#setUserInputEnabled.
        RelativeLayout root = new RelativeLayout(inflater.getContext()) {
            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                super.requestDisallowInterceptTouchEvent(disallowIntercept);

                ViewParent parent = getParent();
                while (parent != null) {
                    if (parent instanceof ViewPager2) {
                        ((ViewPager2) parent).setUserInputEnabled(!disallowIntercept);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        };

        // ViewPager2 requires children to use MATCH_PARENT
        ViewGroup.LayoutParams layoutParams = root.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        root.setLayoutParams(layoutParams);
        return root;
    }

    private void cancel() {
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }
        mDisposables.clear();
    }

    void reset(Image image) {
        this.mMode = NORMAL;
        this.mTarget = null;
        this.mDownloadTmp = null;
        setImage(image);
    }

    void setImage(Image image) {
        setImage(image, false);
    }

    /**
     * If the image is available it will be shown.
     * Otherwise it will be downloaded
     *
     * For non image resources a icon is shown if the file is available otherwise the user will be prompted to confirm the download
     *
     * If required (e.g. after launching the activity via deep link) additional information about the image is collected
     */
    void setImage(Image image, boolean force) {
        cancel();

        this.mBinding.setProgress(null);
        this.mBinding.setProgressText(null);
        this.mBinding.setDrawable(null);

        if (!image.isDatabaseLoaded()) {
            Disposable disposable = mAlbumDao.findImageById(image.getId())
                                             .subscribeOn(Schedulers.io())
                                             .observeOn(AndroidSchedulers.mainThread())
                                             .subscribe(
                                                     img -> {
                                                         image.set(img);
                                                         image.setDatabaseLoaded(true);
                                                         setImage(image);
                                                     },
                                                     err -> {
                                                         onError(image, Reason.UNKNOWN, err);
                                                     }
                                             );
            mDisposables.add(disposable);
            return;
        }

        String type = ImageActivity.getType(image);
        if (!force && image.getPath() != null && new File(image.getPath()).exists()) {
            switch (type) {
                default:
                case "image":
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                    this.mStatus.setValue(StatusWrapper.wrap(image, StatusWrapper.STATUS_LOADED));
                    if (bitmap != null) {
                        this.mBinding.setDrawable(new BitmapDrawable(mContext.getResources(), bitmap));
                    } else {
                        this.mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_image));
                    }
                    break;
                case "video":
                    this.mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_video));
                    this.mStatus.setValue(StatusWrapper.wrap(image, StatusWrapper.STATUS_LOADED));
                    break;
                case "audio":
                    this.mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_audio));
                    this.mStatus.setValue(StatusWrapper.wrap(image, StatusWrapper.STATUS_LOADED));
                    break;
            }
        } else {
            this.mStatus.setValue(StatusWrapper.wrap(image, StatusWrapper.STATUS_PRELOADED));
            if ("image".equals(type)) {
                downloadImage(image);
            } else {
                downloadNonImage(image);
            }
        }
    }

    void downloadOriginal() {
        if (mMode != ORIGINAL) {
            mMode = ORIGINAL;

            StatusWrapper<Image> status = mStatus.getValue();
            if (status == null || status.getValue() == null) return;

            Image image = status.getValue();
            setImage(image, true);
        }
    }

    public void setOnClickListener(View.OnClickListener l) {
        mBinding.galleryImage.setOnClickListener(l);
    }

    /**
     * starts an async download for the specified image
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadImage(Image image) {
        // when in offline mode show confirmation dialog
        if (Preferences.gallery().isOfflineMode()) {
            onError(image, Reason.USER, null);
            return;
        }

        String suffix = image.getName();
        suffix = suffix.substring(suffix.lastIndexOf('.') + 1);

        File dir = mContext.getExternalFilesDir(mContext.getString(R.string.gallery_folder_images));
        File dir2 = new File(mContext.getExternalCacheDir(), mContext.getString(R.string.gallery_folder_images));
        assert dir != null;
        if (!dir.exists()) dir.mkdirs();
        if (!dir2.exists()) dir2.mkdirs();

        mTarget = new File(dir, image.getId() + "." + suffix);
        mDownloadTmp = new File(dir2, image.getId() + "." + suffix + ".tmp");

        try {
            FileOutputStream fos = new FileOutputStream(mDownloadTmp);
            AsyncTask<?, ?, ?> task = QEDGalleryPages.getImage(image, image, mMode, fos, this);
            mDisposables.add(new Disposable() {
                @Override
                public void dispose() {
                    task.cancel(false);
                }

                @Override
                public boolean isDisposed() {
                    return task.isCancelled();
                }
            });
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    /**
     * starts an async download for the specified non image resource after prompting the user for confirmation
     */
    private void downloadNonImage(final Image image) {
        // when in offline mode show confirmation dialog
        if (Preferences.gallery().isOfflineMode()) {
            onError(image, Reason.USER, null);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.image_download_non_picture);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            mMode = ORIGINAL;
            image.setOriginal(true);
            downloadImage(image);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
            onError(image, Reason.USER, null);
        });
        builder.setCancelable(false);
        showDialog(builder);
    }

    /**
     * called when image download is done
     *
     * shows the image or a icon if a non image resource was downloaded
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onPageReceived(@NonNull Image image) {
        if (mDownloadTmp != null && mTarget != null && mDownloadTmp.exists()) {
            if (mTarget.exists()) mTarget.delete();

            if (!mDownloadTmp.renameTo(mTarget)) {
                onError(image, Reason.UNKNOWN, new Exception("Unable to rename file."));
                return;
            }
            if (mDownloadTmp != null) {
                mDownloadTmp.delete();
            }
        } else {
            onError(image, Reason.UNKNOWN, new FileNotFoundException());
            return;
        }

        image.setPath(mTarget.getAbsolutePath());
        image.setOriginal(mMode == ORIGINAL);
        mAlbumDao.insertImagePath(image.getId(), image.getPath(), image.isOriginal())
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(() -> {}, err -> Log.e(LOG_TAG, "Could not save image path to database.", err));

        switch (ImageActivity.getType(image)) {
            case "image":
                mBinding.setDrawable(new BitmapDrawable(mContext.getResources(), BitmapFactory.decodeFile(image.getPath())));
                break;
            case "video":
                mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_video));
                break;
            case "audio":
                mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_audio));
                break;
        }

        mStatus.setValue(StatusWrapper.wrap(image, StatusWrapper.STATUS_LOADED));

        onProgressUpdate(null, 0,0);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onError(Image image, @NonNull Reason reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(image, reason, cause);
        if (mDownloadTmp != null) mDownloadTmp.delete();

        mStatus.setValue(StatusWrapper.wrap(image, reason));

        switch (ImageActivity.getType(image)) {
            default:
            case "image":
                if (image.getThumbnail() != null) {
                    mBinding.setDrawable(new BitmapDrawable(mContext.getResources(), image.getThumbnail()));
                } else {
                    mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_empty_image));
                }
                break;
            case "audio":
                mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_empty_audio));
                break;
            case "video":
                mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_empty_video));
                break;
        }
    }

    @Override
    public void onProgressUpdate(Image image, long done, long total) {
        int percentage = total != 0 ? (int)(100 * done / total) : 0;
        String progressText = String.format(Locale.getDefault(), "%d%% (%.2f MiB)", percentage, done / 1_048_546d);

        mBinding.setProgress(percentage);
        mBinding.setProgressText(progressText);
    }

    public LiveData<StatusWrapper<Image>> getStatus() {
        return mStatus;
    }

    public void onVisibilityChange(boolean visible) {
        if (visible && mPendingDialog != null) {
            mPendingDialog.show();
            mPendingDialog = null;
        }
        mVisible = visible;
    }

    private void showDialog(AlertDialog.Builder dialog) {
        if (mVisible) {
            dialog.show();
        } else {
            mPendingDialog = dialog;
        }
    }
}
