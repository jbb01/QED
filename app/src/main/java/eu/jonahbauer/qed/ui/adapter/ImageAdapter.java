package eu.jonahbauer.qed.ui.adapter;

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
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.ListItemImageBinding;
import eu.jonahbauer.qed.model.Image;
import eu.jonahbauer.qed.model.room.AlbumDao;
import eu.jonahbauer.qed.model.room.Database;
import eu.jonahbauer.qed.networking.pages.QEDGalleryPages;
import eu.jonahbauer.qed.util.Preferences;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;

public class ImageAdapter extends ArrayAdapter<Image> {
    private static final String LOG_TAG = ImageAdapter.class.getName();

    private final Context mContext;
    private final AlbumDao mAlbumDao;

    private final List<Image> mImageList;
    private boolean mOfflineMode;

    public ImageAdapter(Context context, List<Image> imageList) {
        super(context, R.layout.list_item_image, imageList);

        this.mContext = context;
        this.mAlbumDao = Database.getInstance(context.getApplicationContext()).albumDao();

        this.mImageList = imageList;
        this.mOfflineMode = Preferences.getGallery().isOfflineMode();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Image image = mImageList.get(position);

        ListItemImageBinding binding;
        if (convertView != null) {
            binding = (ListItemImageBinding) convertView.getTag(R.id.dataBinding);
        } else {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            binding = ListItemImageBinding.inflate(inflater, parent, false);
            binding.setDisposable(new CompositeDisposable());
            binding.getRoot().setTag(R.id.dataBinding, binding);
        }

        Long imageId = image != null ? image.getId() : null;
        binding.getRoot().setTag(imageId);
        // must set transition name programmatically for view bindings introduce additional delay
        binding.thumbnail.setTransitionName(imageId != null ? getContext().getString(R.string.transition_name_image_thumbnail, imageId) : null);

        binding.setImage(image);
        setThumbnail(image, binding);

        return binding.getRoot();
    }

    private void setThumbnail(Image image, ListItemImageBinding binding) {
        binding.getDisposable().clear();

        if (image == null) {
            applyThumbnail(binding, AppCompatResources.getDrawable(getContext(), R.drawable.ic_gallery_other));
            return;
        }

        if (image.getThumbnail() != null) {
            applyThumbnail(binding, getThumbnail(image, false));
            return;
        }

        applyThumbnail(binding, null);
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
                                             applyThumbnail(binding, getThumbnail(image, false));
                                         }
                                 },
                                 err -> {
                                     if (!mOfflineMode) {
                                         downloadThumbnail(image, binding);
                                     } else {
                                         applyThumbnail(binding, getThumbnail(image, true));
                                     }
                                 }
                         )
        );
    }

    private void applyThumbnail(ListItemImageBinding binding, Drawable drawable) {
        binding.setThumbnail(drawable);
    }

    private void downloadThumbnail(Image image, ListItemImageBinding binding) {
        binding.getDisposable().addAll(
                QEDGalleryPages.getThumbnail(image)
                               .doOnSuccess(optional -> optional.ifPresent(bitmap -> saveThumbnail(image, bitmap)))
                               .subscribe(
                                       optional -> {
                                           image.setThumbnail(optional.orElse(null));
                                           applyThumbnail(binding, getThumbnail(image, false));
                                       },
                                       err -> {
                                           if (!(err instanceof IOException)) {
                                               Log.e(LOG_TAG, "Error loading thumbnail for image " + image.getId() + ".", err);
                                           }
                                           applyThumbnail(binding, getThumbnail(image, true));
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

        boolean available = (!error && !mOfflineMode) || image.getPath() != null;
        int resource = Image.getThumbnail(image.getType(), available);

        return AppCompatResources.getDrawable(mContext, resource);
    }

    public List<Image> getImages() {
        return mImageList;
    }

    @Nullable
    @Override
    public Image getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : Image.NO_ID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.mOfflineMode = offlineMode;
    }
}