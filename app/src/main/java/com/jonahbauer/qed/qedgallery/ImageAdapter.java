package com.jonahbauer.qed.qedgallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.jonahbauer.qed.R;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ImageAdapter extends ArrayAdapter<Image> implements QEDGalleryThumbnailReceiver {
    private final Context context;
    private final List<Image> imageList;

    private HashMap<Image, ImageView> imageViewsToUpdate;
    private HashMap<Image, ProgressBar> progressBarsToUpdate;
    private HashMap<ImageView, QEDGalleryThumbnail> asyncTasks;

    private char[] userid, pwhash, sessionid;


    public ImageAdapter(Context context, List<Image> imageList, char[] userid, char[] pwhash, char[] sessionid) {
        super(context, R.layout.list_item_image, imageList);
        this.context = context;
        this.imageList = imageList;
        this.userid = userid;
        this.pwhash = pwhash;
        this.sessionid = sessionid;

        imageViewsToUpdate = new HashMap<>();
        progressBarsToUpdate = new HashMap<>();
        asyncTasks = new HashMap<>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Image image = imageList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_image, parent, false);
        }

        ImageView thumbnail = view.findViewById(R.id.thumbnail);
        ProgressBar progressBar = view.findViewById(R.id.loading);

        setThumbnail(image, thumbnail, progressBar);

        return view;
    }

    public void add(int index, Image image) {
        imageList.add(index, image);
    }

    private void setThumbnail(Image image, ImageView thumbnail, ProgressBar progressBar) {
        if (image.thumbnail != null) {
            thumbnail.setImageBitmap(image.thumbnail);
            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            imageViewsToUpdate.put(image, thumbnail);
            progressBarsToUpdate.put(image, progressBar);

            thumbnail.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            QEDGalleryThumbnail oldAsync = asyncTasks.get(thumbnail);
            if (oldAsync != null) oldAsync.cancel(true);

            QEDGalleryThumbnail qedGalleryThumbnail = new QEDGalleryThumbnail();
            asyncTasks.put(thumbnail, qedGalleryThumbnail);
            qedGalleryThumbnail.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionid, pwhash, userid, image);
        }
    }

    @Override
    public void onGalleryThumbnailReceived(Image image, Bitmap bitmap) {
        image.thumbnail = bitmap;

        ImageView thumbnail = imageViewsToUpdate.get(image);
        ProgressBar progressBar = progressBarsToUpdate.get(image);
        if (thumbnail != null && progressBar != null) {
            thumbnail.setImageBitmap(bitmap);

            thumbnail.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            imageViewsToUpdate.remove(image);
            progressBarsToUpdate.remove(image);
        }
    }
}