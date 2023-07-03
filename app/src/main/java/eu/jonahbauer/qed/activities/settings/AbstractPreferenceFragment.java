package eu.jonahbauer.qed.activities.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import eu.jonahbauer.qed.util.Colors;
import eu.jonahbauer.qed.util.TransitionUtils;

public abstract class AbstractPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionUtils.setupDefaultTransitions(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(Colors.getBackgroundColor(requireContext()));
        super.onViewCreated(view, savedInstanceState);
    }
}
