package com.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoMessageBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.viewmodel.MessageViewModel;
import com.jonahbauer.qed.util.Themes;

import java.util.Objects;

public class MessageInfoFragment extends InfoFragment {
    private MessageViewModel mMessageViewModel;

    public static MessageInfoFragment newInstance() {
        return new MessageInfoFragment();
    }

    public MessageInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessageViewModel = getViewModelProvider(0).get(MessageViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var binding = FragmentInfoMessageBinding.inflate(inflater, container, false);
        mMessageViewModel.getValue().observe(getViewLifecycleOwner(), message -> {
            binding.setMessage(message);
            binding.setColor(message.getColor(requireContext()));
        });
        return binding.getRoot();
    }


    private @NonNull Message getMessage() {
        return Objects.requireNonNull(mMessageViewModel.getValue().getValue());
    }

    @Override
    public int getColor() {
        return getMessage().getColor(requireContext());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getMessage().getId());
    }
}
