package com.jonahbauer.qed.activities.imageActivity;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Objects;

public class ImageStatus {
    private String imageName;
    private String imageError;
    private boolean ready;

    private final LinkedList<WeakReference<Listener>> listeners = new LinkedList<>();

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        if (!Objects.equals(this.imageName, imageName)) {
            this.imageName = imageName;
            invalidate();
        }
    }

    public String getImageError() {
        return imageError;
    }

    public void setImageError(String imageError) {
        if (!Objects.equals(this.imageError, imageError)) {
            this.imageError = imageError;
            invalidate();
        }
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        if (this.ready != ready) {
            this.ready = ready;
            invalidate();
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(new WeakReference<>(listener));
    }

    public void removeListener(Listener listener) {
        this.listeners.removeIf(ref -> {
            Listener l = ref.get();
            return l == null || l == listener;
        });
    }

    private void invalidate() {
        listeners.removeIf(ref -> ref.get() == null);
        listeners.forEach(ref -> ref.get().invalidated());
    }

    public interface Listener {
        void invalidated();
    }
}
