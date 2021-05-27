package com.jonahbauer.qed.activities.imageActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.layoutStuff.AdvancedImageView;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import com.jonahbauer.qed.util.Preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.NORMAL;
import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.ORIGINAL;

public class ImageViewHolder extends RecyclerView.ViewHolder implements QEDPageReceiver<Image>, QEDPageStreamReceiver<Image> {
    private final AdvancedImageView mImageView;
    private final ProgressBar mProgressBarIndeterminate;
    private final ProgressBar mProgressBar;
    private final TextView mProgressText;

    private QEDGalleryPages.Mode mMode = NORMAL;
    private boolean mNeedsToGetInfo;
    private boolean mNeedsToGetImage;

    private File mTarget;
    private File mDownloadTmp;

    private final Context mContext;
    private final GalleryDatabase mGalleryDatabase;

    private Image mImage;
    private final ImageStatus mImageStatus;

    private final List<AsyncTask<?,?,?>> mAsyncTasks = new ArrayList<>();

    public ImageViewHolder(@NonNull LayoutInflater inflater, GalleryDatabase galleryDatabase) {
        super(create(inflater));

        this.mImageView = itemView.findViewById(R.id.gallery_image);
        this.mProgressBarIndeterminate = itemView.findViewById(R.id.progress_bar_indeterminate);
        this.mProgressBar = itemView.findViewById(R.id.progress_bar);
        this.mProgressText = itemView.findViewById(R.id.progress_text);

        this.mContext = inflater.getContext();
        this.mGalleryDatabase = galleryDatabase;

        this.mImageStatus = new ImageStatus();
    }

    @NonNull
    private static View create(@NonNull LayoutInflater inflater) {
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

        inflater.inflate(R.layout.view_holder_image, root, true);
        return root;
    }

    private void cancel() {
        for (AsyncTask<?,?,?> asyncTask : mAsyncTasks) asyncTask.cancel(true);
    }

    void reset(Image image) {
        this.mMode = NORMAL;
        this.mNeedsToGetInfo = false;
        this.mNeedsToGetImage = false;
        this.mTarget = null;
        this.mDownloadTmp = null;
        setImage(image);
    }

    /**
     * If the image is available it will be shown.
     * Otherwise it will be downloaded
     *
     * For non image resources a icon is shown if the file is available otherwise the user will be prompted to confirm the download
     *
     * If required (e.g. after launching the activity via deep link) additional information about the image is collected
     */
    void setImage(Image image) {
        cancel();

        this.mImage = image;

        mImageStatus.setReady(false);
        mImageStatus.setImageError(null);

        mGalleryDatabase.getImageData(image);
        if (mMode == ORIGINAL) image.setOriginal(true);

        if (image.getName() == null || image.getOwner() == null || image.getUploadDate() == null || image.getFormat() == null) {
            mNeedsToGetInfo = true;
        }

        if (image.getName() != null) {
            mImageStatus.setImageName(image.getName() + (image.isOriginal() ? mContext.getString(R.string.image_suffix_original) : ""));
        }


        String type = ImageActivity.getType(image);
        if (image.getPath() != null && new File(image.getPath()).exists()) {
            switch (type) {
                case "image":
                    Bitmap bmp = BitmapFactory.decodeFile(image.getPath());
                    if (bmp != null) {
                        mImageView.setImageBitmap(bmp);
                    }
                    break;
                case "video":
                    mImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_video));
                    break;
                case "audio":
                    mImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_gallery_audio));
                    break;
            }
        } else {
            mNeedsToGetImage = true;
        }

        if (mNeedsToGetImage || mNeedsToGetInfo) {
            mImageView.setVisibility(View.GONE);
            mProgressBarIndeterminate.setVisibility(View.VISIBLE);
        }

        if (mNeedsToGetInfo) {
            mNeedsToGetInfo = false;
            mAsyncTasks.add(QEDGalleryPages.getImageInfo(image, this));
        } else if (mNeedsToGetImage) {
            mNeedsToGetImage = false;
            if ("image".equals(type)) {
                downloadImage(image);
            } else {
                downloadNonImage(image);
            }
        } else {
            mImageView.setVisibility(View.VISIBLE);
            mProgressBarIndeterminate.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mImageStatus.setReady(true);
        }
    }

    public void setMode(QEDGalleryPages.Mode mode) {
        if (this.mMode != mode) {
            this.mMode = mode;
            this.mNeedsToGetImage = true;
            this.setImage(mImage);
        }
    }

    public void setOnClickListener(View.OnClickListener l) {
        this.mImageView.setOnClickListener(l);
    }

    /**
     * starts an async download for the specified image
     */
    private void downloadImage(Image image) {
        downloadImage(image, null);
    }

    /**
     * @param online default (null), force online (true), force offline (false) overwrite offline (-1), don't overwrite (0), overwrite online (1)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadImage(Image image, Boolean online) {
        if ((online != null && !online) || (online == null && Preferences.gallery().isOfflineMode())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(mContext.getString(R.string.image_overwrite_offline_mode));
            builder.setTitle(R.string.offline_mode);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                downloadImage(image, true);
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                onError(image, Reason.USER, null);
                dialog.dismiss();
            });
            builder.setCancelable(false);

            builder.show();
            return;
        }

        String[] tmp = image.getFormat().split("/");
        String suffix = tmp[1];

        File dir = mContext.getExternalFilesDir(mContext.getString(R.string.gallery_folder_images));
        File dir2 = new File(mContext.getExternalCacheDir(), mContext.getString(R.string.gallery_folder_images));
        assert dir != null;
        if (!dir.exists()) dir.mkdirs();
        if (!dir2.exists()) dir2.mkdirs();

        mTarget = new File(dir, image.getId() + "." + suffix);
        mDownloadTmp = new File(dir2, image.getId() + "." + suffix + ".tmp");

        try {
            FileOutputStream fos = new FileOutputStream(mDownloadTmp);
            image.setPath(mTarget.getAbsolutePath());
            mAsyncTasks.add(QEDGalleryPages.getImage(null, image, mMode, fos, this));
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }

    /**
     * starts an async download for the specified non image resource after prompting the user for confirmation
     */
    private void downloadNonImage(final Image image) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.image_download_non_picture);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            dialog.dismiss();
            mMode = ORIGINAL;
            image.setOriginal(true);
            downloadImage(image);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {});
        builder.show();
    }

    /**
     * called when image download is done
     *
     * shows the image or a icon if a non image resource was downloaded
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onDownloadComplete() {
        if (mDownloadTmp != null && mTarget != null && mDownloadTmp.exists()) {
            if (mTarget.exists()) mTarget.delete();

            if (!mDownloadTmp.renameTo(mTarget)) {
                onError(null, Reason.UNKNOWN, new Exception("Unable to rename file."));
                return;
            }
            if (mDownloadTmp != null) {
                mDownloadTmp.delete();
            }

            mImage.setAvailable(true);
        } else {
            onError(null, Reason.UNKNOWN, new FileNotFoundException());
            return;
        }

        mGalleryDatabase.insert(mImage, true);

        switch (ImageActivity.getType(mImage)) {
            case "image":
                mImageView.setImageBitmap(BitmapFactory.decodeFile(mImage.getPath()));
                break;
            case "video":
                mImageView.setImageResource(R.drawable.ic_gallery_video);
                break;
            case "audio":
                mImageView.setImageResource(R.drawable.ic_gallery_audio);
                break;
        }

        mImageView.setVisibility(View.VISIBLE);
        mProgressBarIndeterminate.setVisibility(View.GONE);
        mImageStatus.setImageError(null);
        mImageStatus.setReady(true);

        onProgressUpdate(null, 0,0);
    }

    /**
     * called after getting image info
     *
     * if image is still not available it will continue with downloading the image
     *
     * otherwise the image will be shown and the collected info will be written to the database
     */
    public void onInfoReceived(Image image) {
        mGalleryDatabase.insert(image, true);

        if (image.getName() == null || image.getOwner() == null || image.getUploadDate() == null || image.getFormat() == null) {
            onError(image, Reason.NOT_FOUND, null);
            return;
        }

        if (mNeedsToGetImage) {
            mNeedsToGetImage = false;
            setImage(image);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            mProgressBarIndeterminate.setVisibility(View.GONE);
            mImageStatus.setImageError(null);
            mImageStatus.setReady(true);
        }
    }

    @Override
    public void onPageReceived(@Nullable Image out) {
        if (out != null) {
            onInfoReceived(out);
        } else {
            onDownloadComplete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onError(Image image, @Reason String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(image, reason, cause);
        if (mDownloadTmp != null) mDownloadTmp.delete();

        mImageView.post(() -> {
            String title = (mImage.getName() != null ? mImage.getName() : "null");

            switch (ImageActivity.getType(mImage)) {
                case "image":
                    Bitmap bm;

                    bm = BitmapFactory.decodeFile(mImage.getPath());
                    if (bm != null) {
                        mImageView.setImageBitmap(bm);
                        break;
                    }

                    bm = mGalleryDatabase.getThumbnail(mImage);
                    if (bm != null) {
                        mImageView.setImageBitmap(bm);
                        title += " (thumbnail)";
                        break;
                    }

                    mImageView.setImageResource(R.drawable.ic_gallery_empty_image);
                    break;
                case "audio":
                    if (mImage.getPath() != null && new File(mImage.getPath()).exists()) {
                        mImageView.setImageResource(R.drawable.ic_gallery_audio);
                    } else
                        mImageView.setImageResource(R.drawable.ic_gallery_empty_audio);
                    break;
                case "video":
                    if (mImage.getPath() != null && new File(mImage.getPath()).exists()) {
                        mImageView.setImageResource(R.drawable.ic_gallery_video);
                    } else
                        mImageView.setImageResource(R.drawable.ic_gallery_empty_video);
                    break;
            }

            mImageStatus.setImageName(title);
            mImageStatus.setReady(false);

            switch (reason) {
                case Reason.NOT_FOUND:
                    mImageStatus.setImageError(mContext.getString(R.string.error_404));
                    break;
                case Reason.NETWORK:
                    mImageStatus.setImageError(mContext.getString(R.string.error_network));
                    break;
                case Reason.UNABLE_TO_LOG_IN:
                    mImageStatus.setImageError(mContext.getString(R.string.error_login));
                    break;
                case Reason.USER:
                    mImageStatus.setImageError(mContext.getString(R.string.error_user));
                    break;
                default:
                case Reason.UNKNOWN:
                    mImageStatus.setImageError(mContext.getString(R.string.error_unknown));
                    break;
            }

            mImageView.setVisibility(View.VISIBLE);
            mProgressBarIndeterminate.setVisibility(View.GONE);
        });
    }

    @Override
    public void onProgressUpdate(Image image, long done, long total) {
        if (done == total) {
            mProgressBar.setVisibility(View.GONE);
            mProgressText.setVisibility(View.GONE);
            return;
        }
        if (total < 2e6) return;

        mProgressBarIndeterminate.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);

        int percentage = (int)(100 * done / total);
        mProgressBar.setProgress(percentage);

        String doneStr = String.valueOf(done / 1024d / 1024d);
        doneStr = doneStr.substring(0, Math.min(doneStr.length(), 4));

        String progressString = percentage + "% (" + doneStr + "MiB)";
        mProgressText.setText(progressString);
    }

    public ImageStatus getImageStatus() {
        return this.mImageStatus;
    }
}
