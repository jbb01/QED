package eu.jonahbauer.qed.activities.sheets.album;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.FragmentInfoAlbumBinding;
import eu.jonahbauer.qed.model.Album;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.viewmodel.AlbumViewModel;
import eu.jonahbauer.qed.util.Themes;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

public class AlbumInfoFragment extends InfoFragment {
    private AlbumViewModel mAlbumViewModel;

    public static AlbumInfoFragment newInstance() {
        return new AlbumInfoFragment();
    }

    public AlbumInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbumViewModel = getViewModelProvider(0).get(AlbumViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var binding = FragmentInfoAlbumBinding.inflate(inflater, container, false);
        mAlbumViewModel.getValue().observe(getViewLifecycleOwner(), binding::setAlbum);
        binding.setColor(getColor());
        return binding.getRoot();
    }

    private @NonNull Album getAlbum() {
        return mAlbumViewModel.getAlbumValue();
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getAlbum().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getAlbum().getId());
    }

    @BindingAdapter("album_categories")
    public static void bindCategories(ViewGroup parent, Collection<String> categories) {
        bindList(parent, categories, (category, item) -> {
            item.setIcon(R.drawable.ic_album_category);
            item.setTitle(Album.decodeCategory(category));
        });
    }

    @BindingAdapter("album_persons")
    public static void bindPersons(ViewGroup parent, Collection<Person> persons) {
        bindList(parent, persons, (person, item) -> {
            item.setIcon(R.drawable.ic_album_person);
            item.setTitle(person.getUsername());
        });
    }
}
