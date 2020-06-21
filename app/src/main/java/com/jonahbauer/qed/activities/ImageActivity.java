package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.QEDGalleryPages.Mode;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jonahbauer.qed.DeepLinkingActivity.QEDIntent;
import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.NORMAL;
import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.ORIGINAL;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ImageActivity extends AppCompatActivity implements GalleryDatabaseReceiver, QEDPageReceiver<Image>, QEDPageStreamReceiver {
    public static final String GALLERY_IMAGE_KEY = "galleryImage";

    private Image mImage;

    private ImageView mImageView;
    private TextView mImageName;
    private TextView mImageError;

    private ActionBar mActionBar;
    private View mOverlayTop;
    private View mOverlayBottom;
    private ProgressBar mProgressBarIndeterminate;
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    private View mWindowDecor;
    private Window mWindow;
    private MenuItem mOpenWithButton;
    private MenuItem mDownloadButton;
    private MenuItem mInfoButton;

    private GalleryDatabase mGalleryDatabase;
    private SharedPreferences mSharedPreferences;

    private boolean mExtended = false;

    private boolean mNeedsToGetInfo = false;
    private boolean mNeedsToGetImage = false;
    private Mode mMode = NORMAL;

    private File mDownloadTmp;
    private File mTarget;

    private List<AsyncTask<?,?,?>> mAsyncTasks = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        setContentView(R.layout.activity_image);
        mGalleryDatabase = new GalleryDatabase();
        mGalleryDatabase.init(this, this);

        if (mImage == null) finish();

        mWindow = getWindow();
        mWindowDecor = mWindow.getDecorView();
        mWindowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mOverlayTop = findViewById(R.id.gallery_image_overlay_top);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle("");
        mActionBar.hide();

        mOverlayBottom = findViewById(R.id.gallery_image_overlay_bottom);
        mProgressBarIndeterminate = findViewById(R.id.progress_bar_indeterminate);
        mProgressBar = findViewById(R.id.progress_bar);
        mProgressText = findViewById(R.id.progress_text);
        mImageError = findViewById(R.id.gallery_image_error);
        mImageView = findViewById(R.id.gallery_image);
        mImageView.setOnClickListener(v -> changeExtended());

        mImageName = findViewById(R.id.gallery_image_name);

        setImage(mImage);
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
        switch (item.getItemId()) {
            case R.id.image_download_original:
                if (mSharedPreferences.getBoolean(Pref.Gallery.OFFLINE_MODE, false)) {
                    Toast.makeText(this, R.string.offline_mode_not_available, Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (!mImage.original) {
                    mMode = ORIGINAL;
                    mNeedsToGetImage = true;
                    setImage(mImage);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.image_already_original);
                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                        mMode = ORIGINAL;
                        mNeedsToGetImage = true;
                        setImage(mImage);
                    });
                    builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                    builder.show();
                }
                return true;
            case R.id.image_open_with:
                Intent intent2 = new Intent();
                intent2.setAction(android.content.Intent.ACTION_VIEW);
                intent2.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Uri uri = FileProvider.getUriForFile(this, "com.jonahbauer.qed.fileprovider", new File(mImage.path));

                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent2, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                intent2.setDataAndType(uri, mImage.format);
                startActivity(intent2);
                return true;
            case R.id.image_info:
                Intent intent = new Intent(this, ImageInfoActivity.class);
                intent.putExtra(ImageInfoActivity.EXTRA_IMAGE, mImage);
                startActivity(intent);
                return true;
            case android.R.id.home:
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
        for (AsyncTask<?,?,?> asyncTask : mAsyncTasks) {
            if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_async_still_running);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    super.finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    for (AsyncTask<?,?,?> asyncTask2 : mAsyncTasks) asyncTask2.cancel(true);
                    if (mDownloadTmp != null && mDownloadTmp.exists()) mDownloadTmp.delete();
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
                return;
            }
        }

        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        for (AsyncTask<?,?,?> asyncTask : mAsyncTasks) asyncTask.cancel(true);
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
            mOverlayTop.setVisibility(View.VISIBLE);
            mOverlayBottom.setVisibility(View.VISIBLE);
        } else {
            mWindowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            mActionBar.hide();
            mOverlayTop.setVisibility(View.GONE);
            mOverlayBottom.setVisibility(View.GONE);
        }

        mWindow.setStatusBarColor(Color.TRANSPARENT);
    }

    /**
     * If the image is available it will be shown.
     * Otherwise it will be downloaded
     *
     * For non image resources a icon is shown if the file is available otherwise the user will be prompted to confirm the download
     *
     * If required (e.g. after launching the activity via deep link) additional information about the image is collected
     */
    private void setImage(Image image) {
        this.mImage = image;

        if (mOpenWithButton != null) mOpenWithButton.setEnabled(false);
        if (mDownloadButton != null) mDownloadButton.setEnabled(false);
        if (mInfoButton != null) mInfoButton.setEnabled(false);
        mImageError.setVisibility(View.GONE);

        image = mGalleryDatabase.getImageData(image);
        if (mMode == ORIGINAL) image.original = true;

        if (image.name == null || image.owner == null || image.uploadDate == null || image.format == null) {
            mNeedsToGetInfo = true;
        }

        if (image.name != null) {
            String name = image.name + (image.original ? getString(R.string.image_suffix_original) : "");
            mImageName.setText(name);
        }


        String type = getType(image);
        if (image.path != null && new File(image.path).exists()) {
            switch (type) {
                case "image":
                    Bitmap bmp = BitmapFactory.decodeFile(image.path);
                    if (bmp != null) {
                        mImageView.setImageBitmap(bmp);
                    }
                    break;
                case "video":
                    mImageView.setImageDrawable(getDrawable(R.drawable.ic_gallery_video));
                    break;
                case "audio":
                    mImageView.setImageDrawable(getDrawable(R.drawable.ic_gallery_audio));
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
            mAsyncTasks.add(QEDGalleryPages.getImageInfo(getClass().toString(), image, this));
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
            if (mOpenWithButton != null) mOpenWithButton.setEnabled(true);
            if (mDownloadButton != null) mDownloadButton.setEnabled(true);
            if (mInfoButton != null) mInfoButton.setEnabled(true);
        }
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
    private void downloadImage(Image image, Boolean online) {
        changeExtended(true);

        if ((online != null && !online) || (online == null && mSharedPreferences.getBoolean(Pref.Gallery.OFFLINE_MODE, false))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.image_overwrite_offline_mode));
            builder.setTitle(R.string.offline_mode);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                downloadImage(image, true);
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                onError(ImageActivity.class.toString(), "User denied large download.", null);
                dialog.dismiss();
            });
            builder.setCancelable(false);

            builder.show();
            return;
        }

        String[] tmp = image.format.split("/");
        String suffix = tmp[1];

        File dir = getExternalFilesDir(getString(R.string.gallery_folder_images));
        File dir2 = new File(getExternalCacheDir(), getString(R.string.gallery_folder_images));
        assert dir != null;
        if (!dir.exists()) dir.mkdirs();
        if (!dir2.exists()) dir2.mkdirs();

        mTarget = new File(dir, image.id + "." + suffix);
        mDownloadTmp = new File(dir2, image.id + "." + suffix + ".tmp");

        try {
            FileOutputStream fos = new FileOutputStream(mDownloadTmp);
            image.path = mTarget.getAbsolutePath();
            mAsyncTasks.add(QEDGalleryPages.getImage(getClass().toString(), image, mMode, fos, this));
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }

    /**
     * starts an async download for the specified non image resource after prompting the user for confirmation
     */
    private void downloadNonImage(final Image image) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.image_download_non_picture);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            dialog.dismiss();
            mMode = ORIGINAL;
            image.original = true;
            downloadImage(image);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> finish());
        builder.show();
    }

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    /**
     * called when image download is done
     *
     * shows the image or a icon if a non image resource was downloaded
     */
    @Override
    public void onPageReceived(String tag) {
        if (mDownloadTmp != null && mTarget != null && mDownloadTmp.exists()) {
            if (mTarget.exists()) mTarget.delete();

            if (!mDownloadTmp.renameTo(mTarget)) {
                onError(tag, null, new Exception("Unable to rename file."));
                return;
            }
            if (mDownloadTmp != null) {
                mDownloadTmp.delete();
            }

            mImage.available = true;
        } else {
            onError(tag, null, new FileNotFoundException());
            return;
        }

        mGalleryDatabase.insert(mImage, true);

        switch (getType(mImage)) {
            case "image":
                mImageView.setImageBitmap(BitmapFactory.decodeFile(mImage.path));
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
        mImageError.setVisibility(View.GONE);
        if (mOpenWithButton != null) mOpenWithButton.setEnabled(true);
        if (mDownloadButton != null) mDownloadButton.setEnabled(true);
        if (mInfoButton != null) mInfoButton.setEnabled(true);

        onProgressUpdate(tag, 0,0);
    }

    /**
     * called after getting image info
     *
     * if image is still not available it will continue with downloading the image
     *
     * otherwise the image will be shown and the collected info will be written to the database
     */
    @Override
    public void onPageReceived(String tag, Image image) {
        mGalleryDatabase.insert(image, true);

        if (mNeedsToGetImage) {
            mNeedsToGetImage = false;
            setImage(image);
        } else {
            mGalleryDatabase.insert(image, true);
            mImageView.setVisibility(View.VISIBLE);
            mProgressBarIndeterminate.setVisibility(View.GONE);
            mImageError.setVisibility(View.GONE);
            if (mOpenWithButton != null) mOpenWithButton.setEnabled(true);
            if (mDownloadButton != null) mDownloadButton.setEnabled(true);
            if (mInfoButton != null) mInfoButton.setEnabled(true);
        }
    }


    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(tag, reason, cause);
        QEDPageReceiver.super.onError(tag, reason, cause);

        if (mDownloadTmp != null) mDownloadTmp.delete();

        mImageView.post(() -> {
            String title = (mImage.name != null ? mImage.name : "null");

            boolean available = false;

            switch (getType(mImage)) {
                case "image":
                    Bitmap bm;

                    bm = BitmapFactory.decodeFile(mImage.path);
                    if (bm != null) {
                        mImageView.setImageBitmap(bm);
                        available = true;
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
                    if (mImage.path != null && new File(mImage.path).exists()) {
                        mImageView.setImageResource(R.drawable.ic_gallery_audio);
                        available = true;
                    } else
                        mImageView.setImageResource(R.drawable.ic_gallery_empty_audio);
                    break;
                case "video":
                    if (mImage.path != null && new File(mImage.path).exists()) {
                        mImageView.setImageResource(R.drawable.ic_gallery_video);
                        available = true;
                    } else
                        mImageView.setImageResource(R.drawable.ic_gallery_empty_video);
                    break;
            }

            mImageName.setText(title);

            mImageError.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.VISIBLE);
            mProgressBarIndeterminate.setVisibility(View.GONE);
            if (mOpenWithButton != null) mOpenWithButton.setEnabled(available);
            if (mDownloadButton != null) mDownloadButton.setEnabled(false);
            if (mInfoButton != null) mInfoButton.setEnabled(false);
        });
    }

    @Override
    public void onProgressUpdate(String tag, long done, long total) {
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

    @Override
    protected void onNewIntent(Intent intent) {
        boolean success = handleIntent(intent, this);

        if (success) {
            setImage(mImage);
            super.onNewIntent(intent);
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public static boolean handleIntent(@NonNull Intent intent, @Nullable ImageActivity imageActivity) {
        Object obj = intent.getSerializableExtra(GALLERY_IMAGE_KEY);
        if (obj instanceof Image) {
            if (imageActivity != null) imageActivity.mImage = (Image) obj;
            return true;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW_IMAGE.equals(intent.getAction())) {
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
                } else {
                    return false;
                }
                if (host != null) if (host.equals("qedgallery.qed-verein.de")) {
                    if (path != null) if (path.startsWith("/image_view.php")) {
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putInt(Pref.General.DRAWER_SELECTION, R.id.nav_gallery).apply();

                        String imageIdStr = queries.getOrDefault("imageid", null);
                        if (imageIdStr != null && imageIdStr.matches("\\d+")) {
                            try {
                                int id = Integer.parseInt(imageIdStr);
                                if (imageActivity != null) imageActivity.mImage = new Image();
                                if (imageActivity != null) imageActivity.mImage.id = id;
                                return true;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return false;
    }


    /**
     * @return the type of the resource ("image", "video", "audio"). if no type is specified "image" will be used as a standard
     */
    private String getType(@NonNull Image image) {
        String type = "image";
        if (image.format != null)
            type = image.format.split("/")[0];

        return type;
    }
}
