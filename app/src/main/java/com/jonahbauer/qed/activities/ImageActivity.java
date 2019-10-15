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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.NORMAL;
import static com.jonahbauer.qed.networking.QEDGalleryPages.Mode.ORIGINAL;

public class ImageActivity extends AppCompatActivity implements GalleryDatabaseReceiver, QEDPageReceiver<Image>, QEDPageStreamReceiver {
    public static final String GALLERY_IMAGE_KEY = "galleryImage";

    private ImageView imageView;
    private TextView imageName;
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
    private boolean extended = false;

    private List<Image> pending;
    private boolean needsToGetInfo = false;
    private boolean needsToGetImage = false;
    private Mode mode = NORMAL;

    private SharedPreferences sharedPreferences;

    private List<AsyncTask> asyncTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        galleryDatabase = new GalleryDatabase();
        galleryDatabase.init(this, this);
        sharedPreferences = getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE);
        asyncTasks = new ArrayList<>();

        pending = new ArrayList<>();

        Intent intent = getIntent();
        image = (Image) intent.getSerializableExtra(GALLERY_IMAGE_KEY);

        if (image == null) finish();

        window = getWindow();
        windowDecor = window.getDecorView();
        windowDecor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);


        overlayTop = findViewById(R.id.gallery_image_overlay_top);
        overlayBottom = findViewById(R.id.gallery_image_overlay_bottom);
        progressBarIndeterminate = findViewById(R.id.progress_bar_indeterminate);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
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

            //Log.d("test", image.format + "//" + image.path);
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
            if (!image.original) {
                mode = ORIGINAL;
                needsToGetImage = true;
                showImage(image);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_already_original);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    mode = ORIGINAL;
                    needsToGetImage = true;
                    showImage(image);
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

            View view = LayoutInflater.from(this).inflate(R.layout.alert_dialog_image_info, null);

            ((TextView)view.findViewById(R.id.image_album)).setText(image.album.name != null ? image.album.name : String.valueOf(image.album.id));
            ((TextView)view.findViewById(R.id.image_owner)).setText(image.owner);
            ((TextView)view.findViewById(R.id.image_upload_date)).setText(MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", image.uploadDate));
            ((TextView)view.findViewById(R.id.image_creation_date)).setText(MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", image.creationDate));
            ((TextView)view.findViewById(R.id.image_format)).setText(image.format);
            ((TextView)view.findViewById(R.id.image_path)).setText(image.path);

            alertDialogBuilder.setView(view);
            alertDialogBuilder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

            alertDialogBuilder.show();
        });
        infoButton.setEnabled(false);

        showImage(image);
    }

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

    public void changeExtended() {
        if (!extended) {
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
        extended = !extended;
    }

    public void showImage(Image image) {
        this.image = image;

        String name = image.name + (image.original ? getString(R.string.image_suffix_original) : "");
        imageName.setText(name);
        setImage(image, imageView, progressBarIndeterminate);
    }

    private void setImage(Image image, ImageView imageView, @Nullable ProgressBar progressBar) {
        launchButton.setEnabled(false);
        downloadButton.setEnabled(false);
        infoButton.setEnabled(false);

        image = galleryDatabase.getImageData(image);
        String path = image.path;
        if (mode == ORIGINAL) image.original = true;

        if (image.name == null || image.owner == null || image.uploadDate == null || image.format == null) {
            needsToGetInfo = true;
        }

        boolean noPicture = false;
        if (image.format != null) {
            String type = image.format.split("/")[0];
            if (!type.equals("image")) {
                path = image.thumbnailPath;
                if (image.path == null) needsToGetImage = true;
//                mode = ORIGINAL;
//                image.original = true;
                noPicture = true;
            }
        }

        if (image.name != null) {
            String name = image.name + (image.original ? getString(R.string.image_suffix_original) : "");
            imageName.setText(name);
        }


        if (path != null) {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp != null) {
                imageView.setImageBitmap(bmp);
            }
        } else {
            needsToGetImage = true;
        }

        if (needsToGetImage || needsToGetInfo) {
            imageView.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }

        if (needsToGetInfo) {
            needsToGetInfo = false;
            QEDGalleryPages.getImageInfo(getClass().toString(), image, this);
        } else if (needsToGetImage) {
            needsToGetImage = false;
            if (noPicture) {
                final Image image2 = image;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.image_download_non_picture);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    downloadImage(image2);
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> finish());
                builder.show();
            } else {
                downloadImage(image);
            }
        } else {
            imageView.setVisibility(View.VISIBLE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            launchButton.setEnabled(true);
            downloadButton.setEnabled(true);
            infoButton.setEnabled(true);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void downloadImage(Image image) {
//        boolean savePublic = sharedPreferences.getBoolean(getString(R.string.preferences_gallery_save_public_key), false);
        File file;
        String[] tmp = image.format.split("/");
        String suffix = tmp[1];

//        if (!savePublic) {
            // save to private storage
            File dir = getExternalFilesDir(getString(R.string.gallery_folder_images));
            assert dir != null;
            if (!dir.exists()) dir.mkdirs();
            file = new File(dir, image.id + "." + suffix);
//        } else {
//            // save to public storage
//            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//            dir = new File(dir, "QEDGallery/");
//            file = new File(dir, image.id + "." + suffix);
//
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                pending.add(image);
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3141);
//                return;
//            }
//
//            if (!file.exists()) dir.mkdirs();
//        }

        try {
            FileOutputStream fos = new FileOutputStream(file);
            image.path = file.getAbsolutePath();
            asyncTasks.add(QEDGalleryPages.getImage(getClass().toString(), image, mode, fos, this));
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public void onReceiveResult(List items) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    @Override
    public void onPageReceived(String tag) {
        galleryDatabase.insert(image, true);

        String[] tmp = image.format.split("/");
        String type = tmp[0];

        if (type.equals("image")) {
            //Log.d("test", "showed file");
            imageView.setImageBitmap(BitmapFactory.decodeFile(image.path));
        } else {
            //Log.d("test", "showed thumbnail instead of file");
            imageView.setImageBitmap(BitmapFactory.decodeFile(image.thumbnailPath));
        }
        imageView.setVisibility(View.VISIBLE);
        progressBarIndeterminate.setVisibility(View.GONE);
        launchButton.setEnabled(true);
        downloadButton.setEnabled(true);
        infoButton.setEnabled(true);

        onProgressUpdate(tag, 0,0);
    }

    @Override
    public void onPageReceived(String tag, Image image) {
        String name = image.name + (image.original ? getString(R.string.image_suffix_original) : "");
        imageName.setText(name);
        changeExtended();

        if (needsToGetImage) {
            needsToGetImage = false;
            boolean noPicture = !image.format.split("/")[0].equals("image");
            if (noPicture && image.path != null) {
                File file = new File(image.path);
                if (file.exists()) {
                    //Log.d("test", "showed thumbnail instead of file");
                    imageView.setImageBitmap(BitmapFactory.decodeFile(image.thumbnailPath));
                    imageView.setVisibility(View.VISIBLE);
                    progressBarIndeterminate.setVisibility(View.GONE);
                    launchButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    infoButton.setEnabled(true);
                }
            } else if (noPicture) {
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
            } else {
                downloadImage(image);
            }
        } else {
            galleryDatabase.insert(image, true);
            imageView.setVisibility(View.VISIBLE);
            progressBarIndeterminate.setVisibility(View.GONE);
            launchButton.setEnabled(true);
            downloadButton.setEnabled(true);
            infoButton.setEnabled(true);
        }
    }

    public void onNetworkError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "networkError at: " + tag);

        imageView.post(() -> {
            String title = image.name + " (thumbnail)";
            imageName.setText(title);
            imageView.setImageBitmap(BitmapFactory.decodeFile(image.thumbnailPath));
            imageView.setVisibility(View.VISIBLE);
            progressBarIndeterminate.setVisibility(View.GONE);
            launchButton.setEnabled(false);
            downloadButton.setEnabled(false);
            infoButton.setEnabled(false);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 3141) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pending.forEach(this::downloadImage);
                pending.clear();
            } else {
                sharedPreferences.edit().putBoolean(getString(R.string.preferences_gallery_save_public_key), false).apply();
            }
        }
    }

    @Override
    public void onStreamError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "streamError at: " + tag);
        // TODO
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
}
