package com.jonahbauer.qed.layoutStuff;


import android.content.Context;
import android.util.AttributeSet;

import org.apache.commons.lang3.NotImplementedException;

public class VisibilityChangingImageButton extends androidx.appcompat.widget.AppCompatImageButton {
    public VisibilityChangingImageButton(Context context) {
        super(context);
    }

    public VisibilityChangingImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VisibilityChangingImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        super.setVisibility(enabled ? VISIBLE : GONE);
    }

    @Override
    public void setVisibility(int visibility) {
        throw new NotImplementedException("Visibility is bound to enabled state.");
    }
}
