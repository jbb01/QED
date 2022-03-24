package com.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoMessageBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.viewmodel.MessageViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class MessageInfoFragment extends InfoFragment {
    private MessageViewModel mMessageViewModel;
    private FragmentInfoMessageBinding mBinding;

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
        mBinding = FragmentInfoMessageBinding.inflate(inflater, container, false);
        mMessageViewModel.getMessage().observe(getViewLifecycleOwner(), messageStatusWrapper -> {
            mBinding.setMessage(messageStatusWrapper.getValue());
            mBinding.setColor(messageStatusWrapper.getValue().getTransformedColor());
        });
        return mBinding.getRoot();
    }

    @NonNull
    private Message getMessage() {
        StatusWrapper<Message> wrapper = mMessageViewModel.getMessage().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Message message = wrapper.getValue();
        assert message != null : "Message should not be null";
        return message;
    }

    @Override
    public int getColor() {
        return getMessage().getTransformedColor();
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getMessage().getId());
    }

    @Override
    protected CharSequence getTitle() {
        var message = getMessage();
        return message.isAnonymous() ? getText(R.string.message_name_anonymous) : message.getName();
    }

    @Override
    protected float getTitleBottom() {
        return mBinding.getRoot().getTop();
    }

    @Override
    public void hideTitle() {}
}
