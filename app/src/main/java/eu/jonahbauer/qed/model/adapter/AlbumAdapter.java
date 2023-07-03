package eu.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.layoutStuff.views.MaterialListItem;
import eu.jonahbauer.qed.model.Album;
import eu.jonahbauer.qed.util.Themes;

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
        var context = getContext();
        final Album album = mAlbumList.get(position);

        MaterialListItem item;
        if (convertView instanceof MaterialListItem) {
            item = (MaterialListItem) convertView;
        } else {
            item = new MaterialListItem(context);
            item.setIcon(R.drawable.ic_gallery_icon);
        }

        item.setTitle(album.getName());
        item.setIconTint(Themes.colorful(context, album.getId()));
        item.setTransitionName(context.getString(R.string.transition_name_album, album.getId()));

        return item;
    }

    @Nullable
    @Override
    public Album getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : Album.NO_ID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}