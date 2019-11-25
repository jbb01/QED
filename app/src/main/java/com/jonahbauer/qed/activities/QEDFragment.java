package com.jonahbauer.qed.activities;

import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.Application;

import java.lang.ref.SoftReference;

abstract class QEDFragment extends Fragment {
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected final SoftReference<Application> applicationReference = Application.getApplicationReference();


    /**
     * Will be executed before the fragment is dropped.
     *
     * @param force if true the fragment will be dropped independent of the return value
     * @return  false if and only if the fragment mustn't be dropped now,
     *          true if it may be dropped now,
     *
     *          null no decision can be made now (e.g. wait for user input)
     *          if null is returned {@code MainActivity#onDropResult} must be called
     */
    Boolean onDrop(boolean force) {
        return true;
    }
}
