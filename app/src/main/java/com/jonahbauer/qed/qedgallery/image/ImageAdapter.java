package com.jonahbauer.qed.qedgallery.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.QEDGalleryPages.Mode;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class ImageAdapter extends ArrayAdapter<Image> implements GalleryDatabaseReceiver, QEDPageStreamReceiver {
    private static final Set<String> videoFileExtensions = new HashSet<>();
    private static final Set<String> audioFileExtensions = new HashSet<>();
    private final GalleryAlbumActivity context;
    private final List<Image> imageList;

    private HashMap<String, AsyncTask> asyncTasks;
    private HashMap<String, Triple<Image, ImageView, ProgressBar>> byTag;
    private HashMap<View, String> tagByView;

    private Set<String> invalidatedTags;

    private GalleryDatabase galleryDatabase;

    private boolean offlineMode;
    public static boolean receivedError = false;

    private Random random;

    static {
        videoFileExtensions.add("mp4");
        videoFileExtensions.add("mkv");
        videoFileExtensions.add("flv");
        videoFileExtensions.add("mov");
        videoFileExtensions.add("avi");
        videoFileExtensions.add("wmv");
        videoFileExtensions.add("mpeg");

        audioFileExtensions.add("wav");
        audioFileExtensions.add("mp3");
        audioFileExtensions.add("ogg");
        audioFileExtensions.add("m4a");
        audioFileExtensions.add("flac");
        audioFileExtensions.add("opus");
    }

    public ImageAdapter(GalleryAlbumActivity context, List<Image> imageList, boolean offlineMode) {
        super(context, R.layout.list_item_image, imageList);
        this.context = context;
        this.imageList = imageList;
        this.offlineMode = offlineMode;

        random = new Random();

        galleryDatabase = new GalleryDatabase();
        galleryDatabase.init(context, this);

        asyncTasks = new HashMap<>();
        byTag = new HashMap<>();
        tagByView = new HashMap<>();
        invalidatedTags = new HashSet<>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Image image = imageList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
            String tag = tagByView.getOrDefault(convertView, "");
            invalidatedTags.add(tag);

            AsyncTask async = asyncTasks.get(tag);
            if (async != null) async.cancel(false);
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_image, parent, false);
        }

        ImageView thumbnail = view.findViewById(R.id.thumbnail);
        ProgressBar progressBar = view.findViewById(R.id.loading);

        ((TextView)view.findViewById(R.id.image_title)).setText(image.name);
        thumbnail.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        String tag = getClass().toString() + random.nextLong();
        byTag.put(tag, new Triple<>(image, thumbnail, progressBar));

        setThumbnail(tag, image, thumbnail, progressBar);

        tagByView.put(view, tag);

        return view;
    }

    public void add(int index, Image image) {
        imageList.add(index, image);
    }

    private void setThumbnail(String tag, Image image, ImageView thumbnail, ProgressBar progressBar) {
        String path = image.thumbnailPath;
        if (path == null) path = galleryDatabase.getImageThumbnailPath(image);
        if (path != null) {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp != null) {
                thumbnail.setImageBitmap(bmp);
                thumbnail.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                return;
            }
        }

        String fileExtension = null;
        if (image.name != null) {
            String[] tmp = image.name.split("\\.");
            fileExtension = tmp[tmp.length - 1];
        }

        if (offlineMode) {
            int drawableId = R.drawable.ic_gallery_empty_image;
            if (videoFileExtensions.contains(fileExtension)) {
                drawableId = R.drawable.ic_gallery_empty_video;
            } else if (audioFileExtensions.contains(fileExtension)) {
                drawableId = R.drawable.ic_gallery_empty_audio;
            }

            thumbnail.setImageDrawable(getContext().getDrawable(drawableId));
            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            image.available = false;
            return;
        }

        thumbnail.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        downloadImage(tag, image);
    }

    private void downloadImage(String tag, Image image) {
        File dir = getContext().getExternalFilesDir(getContext().getString(R.string.gallery_folder_thumbnails));

        assert dir != null;

        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, image.id + ".jpeg");

        try {
            FileOutputStream fos = new FileOutputStream(file);

            image.thumbnailPath = file.getAbsolutePath();

            AsyncTask async = QEDGalleryPages.getImage(tag, image, Mode.THUMBNAIL, fos, this);

            Log.d(Application.LOG_TAG_DEBUG, tag + ": " + async.toString());
            asyncTasks.put(tag, async);
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public void onPageReceived(String tag) {
        if (invalidatedTags.contains(tag)) return;
        Triple<Image,ImageView, ProgressBar> triple = byTag.get(tag);
        assert triple != null;

        Image image = triple.first;
        ImageView thumbnail = triple.second;
        ProgressBar progressBar = triple.third;

        galleryDatabase.insert(image, true);

        if (thumbnail != null && progressBar != null) {
            thumbnail.setImageBitmap(BitmapFactory.decodeFile(image.thumbnailPath));

            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProgressUpdate(String tag, long done, long total) {}

    @Override
    public void onReceiveResult(List items) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    @Override
    public void onNetworkError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "networkError at " + tag);

        if (!receivedError) {
            receivedError = true;
            context.switchToOfflineMode();
        }
    }

    @Override
    public void onStreamError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "streamError at " + tag);
        if (!receivedError) {
            receivedError = true;
            context.switchToOfflineMode();
        }
    }

    private class Triple<A,B,C> {
        A first;
        B second;
        C third;

        public Triple(A a, B b, C c) {
            first = a;
            second = b;
            third = c;
        }
    }
}