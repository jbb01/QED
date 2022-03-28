package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemPersonBinding;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;
import com.jonahbauer.qed.model.Person;

import java.util.List;
import java.util.function.Function;

import static com.jonahbauer.qed.model.Person.COMPARATOR_FIRST_NAME;
import static com.jonahbauer.qed.model.Person.COMPARATOR_LAST_NAME;

public class PersonAdapter extends FixedHeaderAdapter<Person, Character> {
    private static final Function<Person, Character> headerMapFirstName = person -> {
        if (person.getFirstName() == null) return '?';
        else {
            char chr = person.getFirstName().charAt(0);

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
    };
    private static final Function<Person, Character> headerMapLastName = person -> {
        if (person.getLastName() == null) return '?';
        else {
            char chr = person.getLastName().charAt(0);

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
    };

    private SortMode mSort;

    public PersonAdapter(Context context, @NonNull List<Person> itemList, SortMode sort, View fixedHeader) {
        super(context, itemList, headerMapFirstName, COMPARATOR_FIRST_NAME, fixedHeader);
        setSortMode(sort);
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
        switch (sort) {
            case FIRST_NAME:
                setHeaderMap(headerMapFirstName);
                setComparator(COMPARATOR_FIRST_NAME);
                break;
            case LAST_NAME:
                setHeaderMap(headerMapLastName);
                setComparator(COMPARATOR_LAST_NAME);
                break;
        }

        this.mSort = sort;
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

    public enum SortMode {
        FIRST_NAME, LAST_NAME
    }
}
