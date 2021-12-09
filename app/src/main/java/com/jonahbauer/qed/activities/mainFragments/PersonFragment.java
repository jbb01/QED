package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.activities.sheets.person.PersonInfoFragment;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.ViewUtils;

public class PersonFragment extends Fragment {

    private PersonViewModel mPersonViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PersonFragmentArgs args = PersonFragmentArgs.fromBundle(getArguments());
        Person person = args.getPerson();

        mPersonViewModel = ViewUtils.getViewModelProvider(this).get(PersonViewModel.class);
        mPersonViewModel.load(person);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SingleFragmentBinding binding = SingleFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPersonViewModel.getPerson().observe(getViewLifecycleOwner(), wrapper -> {
            Person value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, value.getFullName());
                ViewUtils.setActionStatusBarColor(this, Themes.colorful(value.getId()));
            }
        });

        getChildFragmentManager().addFragmentOnAttachListener(new FragmentOnAttachListener() {
            @Override
            public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment f) {
                if (f instanceof InfoFragment) {
                    ((InfoFragment) f).hideTitle();
                    getChildFragmentManager().removeFragmentOnAttachListener(this);
                }
            }
        });

        PersonInfoFragment fragment = PersonInfoFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
    }

    @Override
    public void onDetach() {
        ViewUtils.resetActionStatusBarColor(this);
        super.onDetach();
    }
}
