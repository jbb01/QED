package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.StatusWrapper;

public class MessageViewModel extends InfoViewModel<Message> {
    public MessageViewModel(@NonNull Application application) {
        super(application);
    }

    public void load(@NonNull Message message) {
        submit(StatusWrapper.loaded(message));
    }

    @NonNull
    @Override
    protected CharSequence getTitle(@NonNull Message message) {
        return message.getName();
    }

    @Override
    protected @StringRes int getDefaultTitle() {
        return R.string.title_fragment_message;
    }
}