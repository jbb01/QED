package eu.jonahbauer.qed.activities.imageActivity;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.rxjava3.EmptyResultSetException;
import androidx.viewpager2.widget.ViewPager2;
import eu.jonahbauer.qed.Application;
import eu.jonahbauer.qed.ConnectionStateMonitor;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.ViewHolderImageBinding;
import eu.jonahbauer.qed.model.Image;
import eu.jonahbauer.qed.model.room.AlbumDao;
import eu.jonahbauer.qed.model.room.Database;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import eu.jonahbauer.qed.networking.pages.QEDGalleryPages;
import eu.jonahbauer.qed.networking.pages.QEDGalleryPages.Mode;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.StatusWrapper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static eu.jonahbauer.qed.networking.pages.QEDGalleryPages.Mode.NORMAL;
import static eu.jonahbauer.qed.networking.pages.QEDGalleryPages.Mode.ORIGINAL;

public class ImageViewHolder extends RecyclerView.ViewHolder implements QEDPageReceiver<Image>, QEDPageStreamReceiver<Image> {
    private static final String LOG_TAG = ImageViewHolder.class.getName();

    private final Context mContext;
    private final AlbumDao mAlbumDao;

    private final ViewHolderImageBinding mBinding;
    private final MutableLiveData<StatusWrapper<Image>> mStatus = new MutableLiveData<>();

    private Mode mMode = NORMAL;

    private File mTarget;
    private File mDownloadTmp;

    private boolean mVisible;
    private AlertDialog.Builder mPendingDialog;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

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

    @MainThread
    public void load(Image image) {
        this.mMode = NORMAL;
        this.mTarget = null;
        this.mDownloadTmp = null;
        this.mPendingDialog = null;
        setImage(image);
    }

    @MainThread
    private void setImage(Image image) {
        setImage(image, false);
    }

    /**
     * Loads and displays the given {@link Image}.
     * <br><br>
     * If the image is downloaded already and {@code forceDownload} is false the downloaded image
     * (or an icon for non-image resources) will be displayed. Otherwise the image will be downloaded.
     * @see #download(Image)
     * @see #downloadWithPrompt(Image)
     */
    @MainThread
    private void setImage(Image image, boolean forceDownload) {
        mDisposable.clear();

        this.mBinding.setProgress(null);
        this.mBinding.setProgressText(null);
        this.mBinding.setDrawable(null);

        if (!image.isDatabaseLoaded()) {
            mDisposable.add(
                    mAlbumDao.findImageById(image.getId())
                             .subscribeOn(Schedulers.io())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(
                                     img -> {
                                         image.set(img);
                                         image.setDatabaseLoaded(true);
                                         setImage(image, forceDownload);
                                     },
                                     err -> {
                                         if (err instanceof EmptyResultSetException) {
                                             mDisposable.add(
                                                     QEDGalleryPages.getImageInfo(image, this)
                                             );
                                         } else {
                                             onError(image, Reason.UNKNOWN, err);
                                         }
                                     }
                             )
            );
            return;
        }

        if (!forceDownload && image.getPath() != null && new File(image.getPath()).exists()) {
            setResult(image);
        } else {
            this.mStatus.setValue(StatusWrapper.preloaded(image));
            if (image.getType() == Image.Type.IMAGE) {
                download(image);
            } else if (!isMeteredConnection()) {
                mMode = ORIGINAL;
                download(image);
            } else {
                downloadWithPrompt(image);
            }
        }
    }

    private void setResult(@NonNull Image image) {
        setResult(image, null);
    }

    private void setResult(@NonNull Image image, @Nullable Reason reason) {
        var type = image.getType();
        if (reason == null && type == Image.Type.IMAGE) {
            setImageFromFile(image);
        } else if (image.getThumbnail() != null) {
            mBinding.setDrawable(new BitmapDrawable(mContext.getResources(), image.getThumbnail()));
        } else if (reason == null) {
            mBinding.setDrawable(AppCompatResources.getDrawable(mContext, Image.getThumbnail(type, true)));
            mStatus.setValue(StatusWrapper.loaded(image));
        } else {
            mBinding.setDrawable(AppCompatResources.getDrawable(mContext, Image.getThumbnail(type, false)));
            mStatus.setValue(StatusWrapper.error(image, reason));
        }
    }

    private void setImageFromFile(@NonNull Image image) {
        mDisposable.add(
                Single.fromCallable(() -> BitmapFactory.decodeFile(image.getPath()))
                      .subscribeOn(Schedulers.io())
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(
                              bitmap -> {
                                  mStatus.setValue(StatusWrapper.loaded(image));
                                  mBinding.setDrawable(new BitmapDrawable(mContext.getResources(), bitmap));
                              },
                              t -> {
                                  mStatus.setValue(StatusWrapper.loaded(image));
                                  mBinding.setDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_image));
                              }
                      )
        );
    }

    /**
     * Forces a re-download of the original version of the currently displayed image.
     */
    public void downloadOriginal() {
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
    private void download(Image image) {
        // when in offline mode show confirmation dialog
        if (Preferences.getGallery().isOfflineMode()) {
            onError(image, Reason.NETWORK, null);
            return;
        }

        String suffix = image.getName();
        suffix = suffix.substring(suffix.lastIndexOf('.') + 1);

        File targetDir = mContext.getExternalFilesDir(mContext.getString(R.string.gallery_folder_images));
        File cacheDir = new File(mContext.getExternalCacheDir(), mContext.getString(R.string.gallery_folder_images));
        assert targetDir != null;

        if (!((targetDir.exists() || targetDir.mkdirs()) && (cacheDir.exists() || cacheDir.mkdirs()))) {
            onError(image, Reason.UNKNOWN, new IOException("Could not create target directories."));
            return;
        }

        mTarget = new File(targetDir, image.getId() + "." + suffix);
        mDownloadTmp = new File(cacheDir, image.getId() + "." + suffix + ".tmp");

        try {
            FileOutputStream fos = new FileOutputStream(mDownloadTmp);
            mDisposable.add(
                    QEDGalleryPages.getImage(image, image, mMode, fos, this)
            );
        } catch (IOException e) {
            onError(image, Reason.UNKNOWN, e);
        }
    }

    /**
     * starts an async download for the specified non image resource after prompting the user for confirmation
     */
    private void downloadWithPrompt(final Image image) {
        // when in offline mode show confirmation dialog
        if (Preferences.getGallery().isOfflineMode()) {
            onError(image, Reason.NETWORK, null);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.image_download_non_picture);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            mMode = ORIGINAL;
            image.setOriginal(true);
            download(image);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> onError(image, Reason.USER, null));
        builder.setCancelable(false);
        showDialog(builder);
    }

    /**
     * called when image download is done
     *
     * shows the image or a icon if a non image resource was downloaded
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onResult(@NonNull Image image) {
        // callback for image info
        if (!image.isDatabaseLoaded()) {
            image.setLoaded(true);
            image.setDatabaseLoaded(true);
            mDisposable.add(
                    this.mAlbumDao.insertOrUpdateImage(image)
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(
                                          () -> setImage(image),
                                          err -> Log.e(LOG_TAG, "Could not save image info to database.", err)
                                  )
            );
            return;
        }

        // callback for image
        if (mDownloadTmp != null && mTarget != null && mDownloadTmp.exists()) {
            if (mTarget.exists()) mTarget.delete();

            if (!mDownloadTmp.renameTo(mTarget)) {
                onError(image, Reason.UNKNOWN, new IOException("Unable to rename file."));
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
        mAlbumDao.insertImagePath(image.getId(), image.getPath(), image.getFormat(), image.isOriginal())
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(() -> {}, err -> Log.e(LOG_TAG, "Could not save image path to database.", err));

        setResult(image);
        this.mBinding.setProgress(null);
        this.mBinding.setProgressText(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onError(Image image, @NonNull Reason reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(image, reason, cause);
        if (mDownloadTmp != null) mDownloadTmp.delete();
        setResult(image, reason);
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

    private boolean isMeteredConnection() {
        Context applicationContext = mContext.getApplicationContext();
        if (applicationContext instanceof Application) {
            var state = ((Application) applicationContext).getConnectionStateMonitor().getConnectionState().getValue();
            return state == ConnectionStateMonitor.State.CONNECTED_METERED;
        }
        return true;
    }
}
