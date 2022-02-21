package com.jonahbauer.qed.model.adapter;

import static com.jonahbauer.qed.model.Image.AUDIO_FILE_EXTENSIONS;
import static com.jonahbauer.qed.model.Image.VIDEO_FILE_EXTENSIONS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemImageBinding;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages;
import com.jonahbauer.qed.util.Preferences;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageAdapter extends ArrayAdapter<Image> {
    private static final String LOG_TAG = ImageAdapter.class.getName();

    private final Context mContext;
    private final AlbumDao mAlbumDao;

    private final List<Image> mImageList;
    private final boolean mOfflineMode;

    public ImageAdapter(Context context, List<Image> imageList) {
        super(context, R.layout.list_item_image, imageList);

        this.mContext = context;
        this.mAlbumDao = Database.getInstance(context.getApplicationContext()).albumDao();

        this.mImageList = imageList;
        this.mOfflineMode = Preferences.gallery().isOfflineMode();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Image image = mImageList.get(position);

        ListItemImageBinding binding;
        if (convertView != null) {
            binding = (ListItemImageBinding) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            binding = ListItemImageBinding.inflate(inflater, parent, false);
            binding.setDisposable(new CompositeDisposable());
            binding.getRoot().setTag(binding);
        }

        binding.setImage(image);
        setThumbnail(image, binding);

        return binding.getRoot();
    }

    private void setThumbnail(Image image, ListItemImageBinding binding) {
        binding.getDisposable().clear();

        if (image.getThumbnail() != null) {
            binding.setThumbnail(getThumbnail(image, false));
            return;
        }

        binding.setThumbnail(null);
        binding.getDisposable().add(
                mAlbumDao.findImageById(image.getId())
                         .subscribeOn(Schedulers.io())
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(
                                 img -> {
                                     image.set(img);
                                     image.setDatabaseLoaded(true);

                                     if (binding.getImage() == image)
                                         if (image.getThumbnail() == null && !mOfflineMode) {
                                             downloadThumbnail(image, binding);
                                         } else {
                                             binding.setThumbnail(getThumbnail(image, false));
                                         }
                                 },
                                 err -> {
                                     if (!mOfflineMode) {
                                         downloadThumbnail(image, binding);
                                     } else {
                                         binding.setThumbnail(getThumbnail(image, true));
                                     }
                                 }
                         )
        );
    }

    private void downloadThumbnail(Image image, ListItemImageBinding binding) {
        binding.getDisposable().addAll(
                QEDGalleryPages.getThumbnail(image)
                               .doOnSuccess(optional -> optional.ifPresent(bitmap -> saveThumbnail(image, bitmap)))
                               .subscribe(
                                       optional -> {
                                           image.setThumbnail(optional.orElse(null));
                                           binding.setThumbnail(getThumbnail(image, false));
                                       },
                                       err -> {
                                           Log.e(LOG_TAG, "Error loading thumbnail for image " + image.getId() + ".", err);
                                           binding.setThumbnail(getThumbnail(image, true));
                                       }
                               )
        );
    }

    private void saveThumbnail(Image image, Bitmap bitmap) {
        //noinspection ResultOfMethodCallIgnored
        mAlbumDao.insertThumbnail(image.getId(), bitmap)
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(() -> {}, err -> Log.e(LOG_TAG, "Could not save thumbnail to database.", err));
    }

    private Drawable getThumbnail(Image image, boolean error) {
        Bitmap thumbnail = image.getThumbnail();

        if (thumbnail != null) {
            return new BitmapDrawable(mContext.getResources(), thumbnail);
        }

        String fileExtension = null;
        if (image.getName() != null) {
            fileExtension = image.getName();
            fileExtension = fileExtension.substring(fileExtension.lastIndexOf('.') + 1);
        }

        boolean available = (!error && !mOfflineMode) || image.getPath() != null;
        int resource;

        if (VIDEO_FILE_EXTENSIONS.contains(fileExtension)) {
            resource = available ? R.drawable.ic_gallery_video : R.drawable.ic_gallery_empty_video;
        } else if (AUDIO_FILE_EXTENSIONS.contains(fileExtension)) {
            resource = available ? R.drawable.ic_gallery_audio : R.drawable.ic_gallery_empty_audio;
        } else {
            resource = available ? R.drawable.ic_gallery_image : R.drawable.ic_gallery_empty_image;
        }

        return AppCompatResources.getDrawable(mContext, resource);
    }

    public List<Image> getImages() {
        return mImageList;
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