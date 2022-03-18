package com.jonahbauer.qed.activities.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import com.jonahbauer.qed.util.Colors;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

public abstract class AbstractPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionUtils.setupDefaultTransitions(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindows(this);
        view.setBackgroundColor(Colors.getBackgroundColor(requireContext()));
        super.onViewCreated(view, savedInstanceState);
    }
}
