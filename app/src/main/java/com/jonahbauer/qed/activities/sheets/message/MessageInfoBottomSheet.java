package com.jonahbauer.qed.activities.sheets.message;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.viewmodel.InfoViewModel;
import com.jonahbauer.qed.model.viewmodel.MessageViewModel;
import com.jonahbauer.qed.util.Themes;

import java.util.Objects;

public class MessageInfoBottomSheet extends InfoBottomSheet {
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

        mMessageViewModel = getViewModelProvider().get(MessageViewModel.class);
        mMessageViewModel.load(message);
    }

    @Override
    public int getColor() {
        return getMessage().getColor(requireContext());
    }

    @Override
    public int getBackground() {
        return Themes.pattern(getMessage().getId());
    }

    @Override
    public @NonNull InfoFragment createFragment() {
        return MessageInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mMessageViewModel;
    }

    @NonNull
    private Message getMessage() {
        return Objects.requireNonNull(mMessageViewModel.getValue().getValue());
    }
}
