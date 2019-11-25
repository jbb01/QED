package com.jonahbauer.qed.activities;

import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
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
import java.text.MessageFormat;
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

    private ImageView imageView;
    private TextView imageName;
    private TextView imageError;
    private Image image;
    private View overlayTop;
    private View overlayBottom;
    private ProgressBar progressBarIndeterminate;
    private ProgressBar progressBar;
    private TextView progressText;

    private View windowDecor;
    private Window window;
    private ImageButton launchButton;
    private ImageButton downloadButton;
    private ImageButton infoButton;

    private GalleryDatabase galleryDatabase;
    private SharedPreferences sharedPreferences;

    private boolean extended = false;

    private boolean needsToGetInfo = false;
    private boolean needsToGetImage = false;
    private Mode mode = NORMAL;

    private File downloadTmp;
    private File target;

    private List<AsyncTask> asyncTasks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        setContentView(R.layout.activity_image);
        galleryDatabase = new GalleryDatabase();
        galleryDatabase.init(this, this);
        asyncTasks = new ArrayList<>();

        if (image == null) finish();

        window = getWindow();
        windowDecor = window.getDecorView();
        windowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        overlayTop = findViewById(R.id.gallery_image_overlay_top);
        overlayBottom = findViewById(R.id.gallery_image_overlay_bottom);
        progressBarIndeterminate = findViewById(R.id.progress_bar_indeterminate);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        imageError = findViewById(R.id.gallery_image_error);
        imageView = findViewById(R.id.gallery_image);
        imageView.setOnClickListener(v -> changeExtended());

        imageName = findViewById(R.id.gallery_image_name);
        ImageButton back = findViewById(android.R.id.home);
        back.setOnClickListener(v -> finish());

        launchButton = findViewById(R.id.image_launch);
        launchButton.setOnClickListener(v -> {
            Intent intent2 = new Intent();
            intent2.setAction(android.content.Intent.ACTION_VIEW);
            intent2.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Uri uri = FileProvider.getUriForFile(this, "com.jonahbauer.qed.fileprovider", new File(image.path));

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent2, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            intent2.setDataAndType(uri,image.format);
            startActivity(intent2);
        });
        launchButton.setEnabled(false);

        downloadButton = findViewById(R.id.image_download);
        downloadButton.setOnClickListener(v -> {
            if (sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false)) {
                Toast.makeText(this, R.string.offline_mode_not_available, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!image.original) {
                mode = ORIGINAL;
                needsToGetImage = true;
                setImage(image);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_already_original);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    mode = ORIGINAL;
                    needsToGetImage = true;
                    setImage(image);
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });
        downloadButton.setEnabled(false);

        infoButton = findViewById(R.id.image_info);
        infoButton.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(image.name);

            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(this).inflate(R.layout.alert_dialog_image_info, null);

            if (image.album != null && image.album.name != null) ((TextView)view.findViewById(R.id.image_album)).setText(image.album.name);
            else if (image.albumName != null ) ((TextView)view.findViewById(R.id.image_album)).setText(image.albumName);
            else if (image.album != null && image.album.id != 0)  ((TextView)view.findViewById(R.id.image_album)).setText(String.valueOf(image.album.id));
            if (image.owner != null) ((TextView)view.findViewById(R.id.image_owner)).setText(image.owner);
            if (image.uploadDate != null) ((TextView)view.findViewById(R.id.image_upload_date)).setText(MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", image.uploadDate));
            if (image.creationDate != null) ((TextView)view.findViewById(R.id.image_creation_date)).setText(MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", image.creationDate));
            if (image.format != null) ((TextView)view.findViewById(R.id.image_format)).setText(image.format);
            if (image.path != null) ((TextView)view.findViewById(R.id.image_path)).setText(image.path);

            alertDialogBuilder.setView(view);
            alertDialogBuilder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

            alertDialogBuilder.show();
        });
        infoButton.setEnabled(false);

        setImage(image);
    }

    /**
     * Prompt when downloads are running and change transition to fade
     */
    @Override
    public void finish() {
        for (AsyncTask asyncTask : asyncTasks) {
            if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_async_still_running);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    super.finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    for (AsyncTask asyncTask2 : asyncTasks) asyncTask2.cancel(true);
                    if (downloadTmp != null && downloadTmp.exists()) downloadTmp.delete();
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
                return;
            }
        }

        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        for (AsyncTask asyncTask : asyncTasks) asyncTask.cancel(true);
    }

    /**
     * show/hide overlay with buttons
     */
    private void changeExtended() {changeExtended(!extended);}
    private void changeExtended(boolean extended) {
        this.extended = extended;
        if (extended) {
            windowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            overlayTop.setVisibility(View.VISIBLE);
            overlayBottom.setVisibility(View.VISIBLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            windowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            overlayTop.setVisibility(View.GONE);
            overlayBottom.setVisibility(View.GONE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
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
        this.image = image;

        launchButton.setEnabled(false);
        downloadButton.setEnabled(false);
        infoButton.setEnabled(false);
        imageError.setVisibility(View.GONE);

        image = galleryDatabase.getImageData(image);
        if (mode == ORIGINAL) image.original = true;

        if (image.name == null || image.owner == null || image.uploadDate == null || image.format == null) {
            needsToGetInfo = true;
        }

        if (image.name != null) {
            String name = image.name + (image.original ? getString(R.string.image_suffix_original) : "");
            imageName.setText(name);
        }


        String type = getType(image);
        if (image.path != null && new File(image.path).exists()) {
            switch (type) {
                case "image":
                    Bitmap bmp = BitmapFactory.decodeFile(image.path);
                    if (bmp != null) {
                        imageView.setImageBitmap(bmp);
                    }
                    break;
                case "video":
                    imageView.setImageDrawable(getDrawable(R.drawable.ic_gallery_video));
                    break;
                case "audio":
                    imageView.setImageDrawable(getDrawable(R.drawable.ic_gallery_audio));
                    break;
            }
        } else {
            needsToGetImage = true;
        }


        if (needsToGetImage || needsToGetInfo) {
            imageView.setVisibility(View.GONE);
            progressBarIndeterminate.setVisibility(View.VISIBLE);
        }

        if (needsToGetInfo) {
            needsToGetInfo = false;
            asyncTasks.add(QEDGalleryPages.getImageInfo(getClass().toString(), image, this));
        } else if (needsToGetImage) {
            needsToGetImage = false;
            if ("image".equals(type)) {
                downloadImage(image);
            } else {
                downloadNonImage(image);
            }
        } else {
            imageView.setVisibility(View.VISIBLE);
            progressBarIndeterminate.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            launchButton.setEnabled(true);
            downloadButton.setEnabled(true);
            infoButton.setEnabled(true);
        }
    }

    /**
     * starts an async download for the specified image
     */
    private void downloadImage(Image image) {
        downloadImage(image, 0);
    }

    /**
     * @param overwriteOfflineMode overwrite offline (-1), don't overwrite (0), overwrite online (1)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadImage(Image image, int overwriteOfflineMode) {
        changeExtended(true);

        if (overwriteOfflineMode == -1 || (overwriteOfflineMode == 0 && sharedPreferences.getBoolean(getString(R.string.preferences_gallery_offline_mode_key), false))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.image_overwrite_offline_mode));
            builder.setTitle(R.string.offline_mode);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                downloadImage(image, 1);
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

        target = new File(dir, image.id + "." + suffix);
        downloadTmp = new File(dir2, image.id + "." + suffix + ".tmp");

        try {
            FileOutputStream fos = new FileOutputStream(downloadTmp);
            image.path = target.getAbsolutePath();
            asyncTasks.add(QEDGalleryPages.getImage(getClass().toString(), image, mode, fos, this));
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
            mode = ORIGINAL;
            image.original = true;
            downloadImage(image);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> finish());
        builder.show();
    }

    @Override
    public void onReceiveResult(List items) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    /**
     * called when image download is done
     *
     * shows the image or a icon if a non image resource was downloaded
     */
    @Override
    public void onPageReceived(String tag, File file) {
        if (downloadTmp != null && target != null && downloadTmp.exists()) {
            if (target.exists()) target.delete();

            if (!downloadTmp.renameTo(target)) {
                onError(tag, null, new Exception("Unable to rename file."));
                return;
            }
            if (downloadTmp != null) {
                downloadTmp.delete();
            }

            image.available = true;
        } else {
            onError(tag, null, new FileNotFoundException());
            return;
        }

        galleryDatabase.insert(image, true);

        switch (getType(image)) {
            case "image":
                imageView.setImageBitmap(BitmapFactory.decodeFile(image.path));
                break;
            case "video":
                imageView.setImageResource(R.drawable.ic_gallery_video);
                break;
            case "audio":
                imageView.setImageResource(R.drawable.ic_gallery_audio);
                break;
        }

        imageView.setVisibility(View.VISIBLE);
        progressBarIndeterminate.setVisibility(View.GONE);
        imageError.setVisibility(View.GONE);
        launchButton.setEnabled(true);
        downloadButton.setEnabled(true);
        infoButton.setEnabled(true);

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
        galleryDatabase.insert(image, true);

        if (needsToGetImage) {
            needsToGetImage = false;
            setImage(image);
        } else {
            galleryDatabase.insert(image, true);
            imageView.setVisibility(View.VISIBLE);
            progressBarIndeterminate.setVisibility(View.GONE);
            imageError.setVisibility(View.GONE);
            launchButton.setEnabled(true);
            downloadButton.setEnabled(true);
            infoButton.setEnabled(true);
        }
    }


    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(tag, reason, cause);
        QEDPageReceiver.super.onError(tag, reason, cause);

        if (downloadTmp != null) downloadTmp.delete();

        imageView.post(() -> {
            String title = (image.name != null ? image.name : "null");

            boolean available = false;

            switch (getType(image)) {
                case "image":
                    Bitmap bm;

                    bm = BitmapFactory.decodeFile(image.path);
                    if (bm != null) {
                        imageView.setImageBitmap(bm);
                        available = true;
                        break;
                    }

                    bm = BitmapFactory.decodeFile(image.thumbnailPath);
                    if (bm != null) {
                        imageView.setImageBitmap(bm);
                        title += " (thumbnail)";
                        break;
                    }

                    imageView.setImageResource(R.drawable.ic_gallery_empty_image);
                    break;
                case "audio":
                    if (image.path != null && new File(image.path).exists()) {
                        imageView.setImageResource(R.drawable.ic_gallery_audio);
                        available = true;
                    } else
                        imageView.setImageResource(R.drawable.ic_gallery_empty_audio);
                    break;
                case "video":
                    if (image.path != null && new File(image.path).exists()) {
                        imageView.setImageResource(R.drawable.ic_gallery_video);
                        available = true;
                    } else
                        imageView.setImageResource(R.drawable.ic_gallery_empty_video);
                    break;
            }

            imageName.setText(title);

            imageError.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            progressBarIndeterminate.setVisibility(View.GONE);
            launchButton.setEnabled(available);
            downloadButton.setEnabled(false);
            infoButton.setEnabled(false);
        });
    }

    @Override
    public void onProgressUpdate(String tag, long done, long total) {
        if (done == total) {
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            return;
        }
        if (total < 2e6) return;

        progressBarIndeterminate.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        int percentage = (int)(100 * done / total);
        progressBar.setProgress(percentage);

        String doneStr = String.valueOf(done / 1024d / 1024d);
        doneStr = doneStr.substring(0, Math.min(doneStr.length(), 4));

        String progressString = percentage + "% (" + doneStr + "MiB)";
        progressText.setText(progressString);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        boolean success = handleIntent(intent, this);

        if (success) {
            setImage(image);
            super.onNewIntent(intent);
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public static boolean handleIntent(@NonNull Intent intent, @Nullable ImageActivity imageActivity) {
        Object obj = intent.getSerializableExtra(GALLERY_IMAGE_KEY);
        if (obj instanceof Image) {
            if (imageActivity != null) imageActivity.image = (Image) obj;
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
//                        editor.putInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_gallery).apply();

                        String imageIdStr = queries.getOrDefault("imageid", null);
                        if (imageIdStr != null && imageIdStr.matches("\\d+")) {
                            try {
                                int id = Integer.parseInt(imageIdStr);
                                if (imageActivity != null) imageActivity.image = new Image();
                                if (imageActivity != null) imageActivity.image.id = id;
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
