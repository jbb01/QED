package com.jonahbauer.qed.qedgallery.album;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;

import java.util.List;
import java.util.Objects;

public class AlbumAdapter extends ArrayAdapter<Album> {
    private final Context context;
    private final List<Album> albumList;

    public AlbumAdapter(Context context, List<Album> albumList) {
        super(context, R.layout.list_item_event, albumList);
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Album album = albumList.get(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_gallery, parent, false);
        }

        ImageView galleryIcon = view.findViewById(R.id.gallery_icon);
        switch (album.name.chars().sum()%10) {
            case 0:
                galleryIcon.setColorFilter(Color.argb(0xff, 0x33, 0xb5, 0xe5));
                break;
            case 1:
                galleryIcon.setColorFilter(Color.argb(0xff, 0x99, 0xcc, 0x00));
                break;
            case 2:
                galleryIcon.setColorFilter(Color.argb(0xff, 0xff, 0x44, 0x44));
                break;
            case 3:
                galleryIcon.setColorFilter(Color.argb(0xff, 0x00, 0x99, 0xcc));
                break;
            case 4:
                galleryIcon.setColorFilter(Color.argb(0xff, 0x66, 0x99, 0x00));
                break;
            case 5:
                galleryIcon.setColorFilter(Color.argb(0xff, 0xcc, 0x00, 0x00));
                break;
            case 6:
                galleryIcon.setColorFilter(Color.argb(0xff, 0xaa, 0x66, 0xcc));
                break;
            case 7:
                galleryIcon.setColorFilter(Color.argb(0xff, 0xff, 0xbb, 0x33));
                break;
            case 8:
                galleryIcon.setColorFilter(Color.argb(0xff, 0xff, 0x88, 0x00));
                break;
            case 9:
                galleryIcon.setColorFilter(Color.argb(0xff, 0x00, 0xdd, 0xff));
                break;
        }

        ((TextView)view.findViewById(R.id.gallery_name)).setText(album.name);

        return view;
    }

    public void add(int index, Album album) {
        albumList.add(index, album);
    }
}