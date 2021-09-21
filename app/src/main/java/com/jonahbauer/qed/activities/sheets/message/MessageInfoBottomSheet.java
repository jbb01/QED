package com.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.viewmodel.MessageViewModel;
import com.jonahbauer.qed.util.StatusWrapper;

public class MessageInfoBottomSheet extends AbstractInfoBottomSheet {
    private static final String ARGUMENT_MESSAGE = "message";

    private MessageViewModel mMessageViewModel;

    @NonNull
    public static MessageInfoBottomSheet newInstance(Message message) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_MESSAGE, message);
        MessageInfoBottomSheet sheet = new MessageInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public MessageInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;

        Message message = args.getParcelable(ARGUMENT_MESSAGE);
        if (message == null) {
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mMessageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        mMessageViewModel.load(message);
    }

    @Override
    public int getColor() {
        return getMessage().getTransformedColor();
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return MessageInfoFragment.newInstance();
    }

    @NonNull
    private Message getMessage() {
        StatusWrapper<Message> wrapper = mMessageViewModel.getMessage().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Message message = wrapper.getValue();
        assert message != null : "Message should not be null";
        return message;
    }
}
