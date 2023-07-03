package eu.jonahbauer.qed.layoutStuff.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;
import eu.jonahbauer.qed.R;

@SuppressWarnings("unused")
public class AdvancedEditTextPreference extends EditTextPreference {
    private OnPreferenceClickListener mOnSettingsClickListener;

    private final View.OnClickListener mOnClickListener = v -> performSettingsClick();

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context) {
        super(context, null);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_edit_text_settings);
    }

    public void setOnSettingsClickListener(@Nullable OnPreferenceClickListener onSettingsClickListener) {
        this.mOnSettingsClickListener = onSettingsClickListener;
    }

    public @Nullable OnPreferenceClickListener getOnSettingsClickListener() {
        return mOnSettingsClickListener;
    }

    protected void performSettingsClick() {
        if (mOnSettingsClickListener != null) {
            mOnSettingsClickListener.onPreferenceClick(this);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View settingsView = holder.findViewById(R.id.settings);
        if (settingsView != null) {
            settingsView.setOnClickListener(mOnClickListener);
        }
    }
}
