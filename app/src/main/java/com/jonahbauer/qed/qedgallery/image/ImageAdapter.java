package com.jonahbauer.qed.qedgallery.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.database.GalleryDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.QEDGalleryPages.Mode;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;
import com.jonahbauer.qed.util.Triple;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static com.jonahbauer.qed.qedgallery.image.Image.audioFileExtensions;
import static com.jonahbauer.qed.qedgallery.image.Image.videoFileExtensions;

public class ImageAdapter extends ArrayAdapter<Image> implements GalleryDatabaseReceiver, QEDPageStreamReceiver {
    private final GalleryAlbumActivity mContext;
    private final List<Image> mImageList;

    private final HashMap<String, AsyncTask<?,?,?>> mAsyncTasks;
    private final HashMap<String, Triple<Image, ImageView, ProgressBar>> mByTag;
    private final HashMap<View, String> mTagByView;
    private final HashMap<Integer, ByteArrayOutputStream> mBaosById;

    private final SparseArray<SoftReference<Bitmap>> mCache;

    private final Set<String> mInvalidatedTags;

    private final GalleryDatabase mGalleryDatabase;

    private boolean mOfflineMode;
    public static boolean sReceivedError = false;

    private final Random mRandom;

    public ImageAdapter(GalleryAlbumActivity context, List<Image> imageList, boolean offlineMode) {
        super(context, R.layout.list_item_image, imageList);
        this.mContext = context;
        this.mImageList = imageList;
        this.mOfflineMode = offlineMode;

        mRandom = new Random();

        mGalleryDatabase = new GalleryDatabase();
        mGalleryDatabase.init(context, this);

        mAsyncTasks = new HashMap<>();
        mByTag = new HashMap<>();
        mTagByView = new HashMap<>();
        mInvalidatedTags = new HashSet<>();
        mCache = new SparseArray<>();
        mBaosById = new HashMap<>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Image image = mImageList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
            String tag = mTagByView.getOrDefault(convertView, "");
            mInvalidatedTags.add(tag);

            AsyncTask<?,?,?> async = mAsyncTasks.get(tag);
            if (async != null) async.cancel(false);
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_image, parent, false);
        }

        ImageView thumbnail = view.findViewById(R.id.thumbnail);
        ProgressBar progressBar = view.findViewById(R.id.loading);

        ((TextView)view.findViewById(R.id.image_title)).setText(image.name);
        thumbnail.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        String tag = getClass().toString() + mRandom.nextLong();
        mByTag.put(tag, new Triple<>(image, thumbnail, progressBar));

        setThumbnail(tag, image, thumbnail, progressBar);

        mTagByView.put(view, tag);

        return view;
    }

    public void add(int index, Image image) {
        mImageList.add(index, image);
    }

    private void setThumbnail(String tag, @NonNull Image image, ImageView thumbnail, ProgressBar progressBar) {
        // Cache
        SoftReference<Bitmap> cachedBitmap = mCache.get(image.id);
        if (cachedBitmap != null) {
            Bitmap bitmap = cachedBitmap.get();
            if (bitmap != null) {
                thumbnail.setImageBitmap(bitmap);
                thumbnail.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                return;
            }
        }

        // Database
        Bitmap bitmap = mGalleryDatabase.getThumbnail(image);
        if (bitmap != null) {
            mCache.put(image.id, new SoftReference<>(bitmap));
            thumbnail.setImageBitmap(bitmap);
            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            image.available = true;
            return;
        }

        String fileExtension = null;
        if (image.name != null) {
            String[] tmp = image.name.split("\\.");
            fileExtension = tmp[tmp.length - 1];
        }

        if (mOfflineMode) {
            if (image.path == null) image.path = mGalleryDatabase.getImagePath(image);
            image.available = image.path != null && new File(image.path).exists();

            int drawableId;
            if (image.available) {
                drawableId = R.drawable.ic_gallery_image;
                if (videoFileExtensions.contains(fileExtension)) {
                    drawableId = R.drawable.ic_gallery_video;
                } else if (audioFileExtensions.contains(fileExtension)) {
                    drawableId = R.drawable.ic_gallery_audio;
                }
            } else {
                drawableId = R.drawable.ic_gallery_empty_image;
                if (videoFileExtensions.contains(fileExtension)) {
                    drawableId = R.drawable.ic_gallery_empty_video;
                } else if (audioFileExtensions.contains(fileExtension)) {
                    drawableId = R.drawable.ic_gallery_empty_audio;
                }
            }

            thumbnail.setImageDrawable(getContext().getDrawable(drawableId));
            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            return;
        } else {
            if (videoFileExtensions.contains(fileExtension)) {
                thumbnail.setImageDrawable(getContext().getDrawable(R.drawable.ic_gallery_video));
                thumbnail.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                image.available = true;
                return;
            }

            if (audioFileExtensions.contains(fileExtension)) {
                thumbnail.setImageDrawable(getContext().getDrawable(R.drawable.ic_gallery_audio));
                thumbnail.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                image.available = true;
                return;
            }
        }

        thumbnail.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Download
        downloadImage(tag, image);
    }

    private void downloadImage(String tag, @NonNull Image image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mBaosById.put(image.id, baos);

        AsyncTask<?,?,?> async = QEDGalleryPages.getImage(tag, image, Mode.THUMBNAIL, baos, this);

        mAsyncTasks.put(tag, async);
    }

    @Override
    public void onPageReceived(String tag) {
        Triple<Image, ImageView, ProgressBar> triple = mByTag.get(tag);
        assert triple != null;

        Image image = triple.first;
        ImageView thumbnail = triple.second;
        ProgressBar progressBar = triple.third;

        ByteArrayOutputStream baos = mBaosById.get(image.id);
        if (baos == null) return;

        byte[] encodedBitmap = baos.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodedBitmap, 0, encodedBitmap.length);
        if (bitmap != null) {
            mGalleryDatabase.insertThumbnail(image, bitmap);
            mCache.put(image.id, new SoftReference<>(bitmap));
        }

        mBaosById.remove(image.id);
        mByTag.remove(tag);

        if (mInvalidatedTags.contains(tag)) return;

        if (thumbnail != null && progressBar != null) {
            if (bitmap == null) {
                thumbnail.setImageResource(R.drawable.ic_gallery_empty_image);
            } else {
                thumbnail.setImageBitmap(bitmap);
            }

            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProgressUpdate(String tag, long done, long total) {}

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    public void setOfflineMode(boolean offlineMode) {
        this.mOfflineMode = offlineMode;
    }

    public void clearCache() {
        this.mCache.clear();
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(tag, reason, cause);

        if (!sReceivedError) {
            sReceivedError = true;
            mContext.switchToOfflineMode();
        }
    }
}