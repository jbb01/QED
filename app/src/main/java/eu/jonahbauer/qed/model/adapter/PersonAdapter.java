package eu.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.ListItemPersonBinding;
import eu.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.parcel.ParcelableEnum;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static eu.jonahbauer.qed.model.Person.COMPARATOR_FIRST_NAME;
import static eu.jonahbauer.qed.model.Person.COMPARATOR_LAST_NAME;

public class PersonAdapter extends FixedHeaderAdapter<Person, Character> {
    private @NonNull SortMode mSort;

    public PersonAdapter(
            @NonNull Context context,
            @NonNull List<Person> itemList,
            @NonNull SortMode sort,
            @NonNull View fixedHeader
    ) {
        super(context, itemList, sort, sort, fixedHeader);
        this.mSort = sort;
    }

    @NonNull
    @Override
    protected View getItemView(Person person, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
        ListItemPersonBinding binding;

        if (convertView != null) {
            binding = (ListItemPersonBinding) convertView.getTag();
        } else {
            binding = ListItemPersonBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
        }

        binding.setPerson(person);
        binding.setInvertedInitials(mSort == SortMode.LAST_NAME);
        binding.header.setText("");
        return binding.getRoot();
    }

    @Override
    protected void setHeader(@NonNull View view, Character header) {
        // TODO use databinding when database fragment uses it
        ((TextView)view.findViewById(R.id.header)).setText(String.valueOf(header));
    }

    public void setSortMode(@NonNull SortMode sort) {
        this.mSort = Objects.requireNonNull(sort);
        setHeaderMap(sort);
        setComparator(sort);
    }

    @Nullable
    @Override
    public Person getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : Person.NO_ID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public enum SortMode implements ParcelableEnum, Function<Person, Character>, Comparator<Person> {
        FIRST_NAME(Person::getFirstName, COMPARATOR_FIRST_NAME),
        LAST_NAME(Person::getLastName, COMPARATOR_LAST_NAME),
        ;

        public static final Parcelable.Creator<SortMode> CREATOR = new Creator<>(SortMode.values(), SortMode[]::new);

        private final @NonNull Function<Person, String> name;
        private final @NonNull Comparator<Person> comparator;

        SortMode(@NonNull Function<Person, String> name, @NonNull Comparator<Person> comparator) {
            this.name = Objects.requireNonNull(name);
            this.comparator = Objects.requireNonNull(comparator);
        }

        @Override
        public @NonNull Character apply(@NonNull Person person) {
            return getHeader(name.apply(person));
        }

        @Override
        public int compare(@NonNull Person first, @NonNull Person second) {
            return comparator.compare(first, second);
        }

        private static Character getHeader(String name) {
            if (name == null || name.isEmpty()) return '?';
            char chr = name.charAt(0);
            switch (chr) {
                case 'Ä':
                case 'ä':
                    return 'A';
                case 'Ö':
                case 'ö':
                    return 'O';
                case 'Ü':
                case 'ü':
                    return 'U';
                default:
                    if ('a' <= chr && chr <= 'z') chr -= 0x32;
                    return chr;
            }
        }
    }
}
