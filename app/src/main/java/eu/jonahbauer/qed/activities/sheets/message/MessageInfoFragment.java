package eu.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.FragmentInfoMessageBinding;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.model.viewmodel.MessageViewModel;

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
    protected long getDesignSeed() {
        return getMessage().getId();
    }
}
