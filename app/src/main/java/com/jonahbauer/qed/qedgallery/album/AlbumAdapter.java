package com.jonahbauer.qed.qedgallery.album;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.util.List;
import java.util.Objects;

public class AlbumAdapter extends ArrayAdapter<Album> {
    private final Context mContext;
    private final List<Album> mAlbumList;

    public AlbumAdapter(Context context, List<Album> albumList) {
        super(context, R.layout.list_item_event, albumList);
        this.mContext = context;
        this.mAlbumList = albumList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Album album = mAlbumList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_gallery, parent, false);
        }

        ImageView galleryIcon = view.findViewById(R.id.gallery_icon);
        galleryIcon.setColorFilter(Application.colorful(album.name.chars().sum()));

        ((TextView)view.findViewById(R.id.gallery_name)).setText(album.name);

        return view;
    }

    public void add(int index, Album album) {
        mAlbumList.add(index, album);
    }
}