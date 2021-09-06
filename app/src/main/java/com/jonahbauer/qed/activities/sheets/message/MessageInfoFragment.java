package com.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoMessageBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.viewmodel.MessageViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class MessageInfoFragment extends AbstractInfoFragment {
    private MessageViewModel mMessageViewModel;
    private FragmentInfoMessageBinding mBinding;

    public static MessageInfoFragment newInstance() {
        return new MessageInfoFragment();
    }

    public MessageInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewModelStoreOwner owner = getParentFragment();
        if (owner == null) owner = requireActivity();
        mMessageViewModel = new ViewModelProvider(owner).get(MessageViewModel.class);
    }

    @Override
    protected ViewDataBinding onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoMessageBinding.inflate(inflater, container, true);
        mMessageViewModel.getMessage().observe(getViewLifecycleOwner(), messageStatusWrapper -> {
            mBinding.setMessage(messageStatusWrapper.getValue());
            mBinding.setColor(messageStatusWrapper.getValue().getTransformedColor());
        });
        return mBinding;
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
    protected int getColor() {
        return getMessage().getTransformedColor();
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getMessage().getId());
    }

    @Override
    protected String getTitle() {
        return getMessage().getName();
    }

    @Override
    protected float getTitleBottom() {
        return mBinding.getRoot().getTop();
    }
}
