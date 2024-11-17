package eu.jonahbauer.qed.ui.adapter;

import android.content.Context;
import android.icu.text.Normalizer2;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.ListItemPersonBinding;
import eu.jonahbauer.qed.ui.FixedHeaderAdapter;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.parcel.ParcelableEnum;
import eu.jonahbauer.qed.util.Preferences;
import lombok.Value;

import java.util.*;
import java.util.function.Function;

public class PersonAdapter extends FixedHeaderAdapter<PersonAdapter.PersonWrapper, String> {
    @SuppressWarnings("NotNullFieldNotInitialized")
    private @NonNull SortMode mSort;

    public PersonAdapter(
            @NonNull Context context,
            @NonNull List<Person> itemList,
            @NonNull SortMode sort,
            @NonNull View fixedHeader
    ) {
        super(context, preprocess(itemList), fixedHeader);
        setSortMode(sort);
    }

    private static @NonNull List<PersonWrapper> preprocess(@NonNull List<Person> list) {
        var user = Preferences.getGeneral().getUsername();

        var out = new ArrayList<PersonWrapper>(list.size() + 1);
        out.add(null);
        for (Person person : list) {
            var isSelf = Objects.equals(person.getUsername(), user);
            if (isSelf && out.get(0) == null) {
                out.set(0, new PersonWrapper(person, true));
            }
            out.add(new PersonWrapper(person, false));
        }
        return out.get(0) == null ? out.subList(1, out.size()) : out;
    }

    @NonNull
    @Override
    protected View getItemView(PersonWrapper person, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
        ListItemPersonBinding binding;

        if (convertView != null) {
            binding = (ListItemPersonBinding) convertView.getTag();
        } else {
            binding = ListItemPersonBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
        }

        binding.setPerson(person.getPerson());
        binding.setInvertedInitials(mSort == SortMode.LAST_NAME);
        binding.header.setText("");
        return binding.getRoot();
    }

    @Override
    protected void setHeader(@NonNull View view, String header) {
        // TODO use databinding when database fragment uses it
        ((TextView)view.findViewById(R.id.header)).setText(String.valueOf(header));
    }

    public void setSortMode(@NonNull SortMode sort) {
        this.mSort = Objects.requireNonNull(sort);
        setHeaderMap(sort.getHeaderMap(getContext().getString(R.string.persons_database_header_self)));
        setComparator(sort.getComparator());
    }

    public @Nullable Person getPerson(int position) {
        var wrapper = getItem(position);
        return wrapper == null ? null : wrapper.getPerson();
    }

    public void setPersons(@NonNull List<Person> list) {
        this.clear();
        this.addAll(preprocess(list));
    }

    @Override
    public @Nullable PersonWrapper getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        if (item == null) return Person.NO_ID;
        else if (item.isSelf()) return - item.getPerson().getId();
        else return item.getPerson().getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public enum SortMode implements ParcelableEnum {
        FIRST_NAME(Person::getFirstName, Person.Comparators.FIRST_NAME),
        LAST_NAME(Person::getLastName, Person.Comparators.LAST_NAME),
        ;

        public static final Parcelable.Creator<SortMode> CREATOR = new Creator<>(SortMode.values(), SortMode[]::new);
        private static final Normalizer2 NORMALIZER = Normalizer2.getNFDInstance();

        private final @NonNull Function<Person, String> name;
        private final @NonNull Comparator<PersonWrapper> comparator;

        SortMode(@NonNull Function<Person, String> name, @NonNull Comparator<Person> comparator) {
            this.name = Objects.requireNonNull(name);
            this.comparator = Comparator.<PersonWrapper>comparingInt(wrapper -> wrapper.isSelf() ? 0 : 1)
                    .thenComparing(PersonWrapper::getPerson, comparator);
        }

        public @NonNull Function<PersonWrapper, String> getHeaderMap(@NonNull String self) {
            return wrapper -> {
                var isSelf = wrapper.isSelf();
                return isSelf ? self : getHeader(name.apply(wrapper.getPerson()));
            };
        }

        public @NonNull Comparator<PersonWrapper> getComparator() {
            return comparator;
        }

        private static @NonNull String getHeader(@Nullable String name) {
            if (name == null || name.isEmpty()) return "?";
            return NORMALIZER.normalize(name).toUpperCase(Locale.ROOT).substring(0, 1);
        }
    }

    @Value
    public static class PersonWrapper {
        @NonNull Person person;
        boolean isSelf;

        public PersonWrapper(@NonNull Person person, boolean isSelf) {
            this.person = Objects.requireNonNull(person);
            this.isSelf = isSelf;
        }
    }
}
