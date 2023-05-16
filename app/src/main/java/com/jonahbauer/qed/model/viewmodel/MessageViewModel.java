package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.StatusWrapper;

public class MessageViewModel extends InfoViewModel<Message> {
    public MessageViewModel(@NonNull Application application) {
        super(application);
    }

    public void load(@NonNull Message message) {
        submit(StatusWrapper.loaded(message));
    }

    @Override
    protected CharSequence getTitle(@NonNull Message message) {
        return null;
    }

    @Override
    protected CharSequence getToolbarTitle(@NonNull Message message) {
        return MessageUtils.formatName(getApplication(), message.getName());
    }

    @Override
    protected @StringRes Integer getDefaultTitle() {
        return null;
    }

    @Override
    protected @StringRes Integer getDefaultToolbarTitle() {
        return R.string.title_fragment_message;
    }
}