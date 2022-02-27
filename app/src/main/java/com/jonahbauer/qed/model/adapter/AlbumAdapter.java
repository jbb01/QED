package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.views.MaterialListItem;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.util.Themes;

import java.util.List;

public class AlbumAdapter extends ArrayAdapter<Album> {
    private final List<Album> mAlbumList;

    public AlbumAdapter(Context context, List<Album> albumList) {
        super(context, 0, albumList);
        this.mAlbumList = albumList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Album album = mAlbumList.get(position);

        MaterialListItem item;
        if (convertView instanceof MaterialListItem) {
            item = (MaterialListItem) convertView;
        } else {
            item = new MaterialListItem(getContext());
            item.setIcon(R.drawable.ic_gallery_icon);
        }

        item.setTitle(album.getName());
        item.setIconTint(Themes.colorful(getContext(), album.getId()));

        return item;
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}