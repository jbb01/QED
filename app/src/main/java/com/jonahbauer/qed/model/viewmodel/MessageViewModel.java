package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.StatusWrapper;

public class MessageViewModel extends ViewModel {
    private final MutableLiveData<StatusWrapper<Message>> mMessage = new MutableLiveData<>();

    public void load(@NonNull Message message) {
        this.mMessage.setValue(StatusWrapper.loaded(message));
    }

    public LiveData<StatusWrapper<Message>> getMessage() {
        return mMessage;
    }
}