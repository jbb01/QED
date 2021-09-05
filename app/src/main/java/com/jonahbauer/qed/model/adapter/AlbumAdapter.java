package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemAlbumBinding;
import com.jonahbauer.qed.model.Album;

import java.util.List;

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

        ListItemAlbumBinding binding;
        if (convertView != null) {
            binding = (ListItemAlbumBinding) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            binding = ListItemAlbumBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
        }

        binding.setAlbum(album);
        return binding.getRoot();
    }
}