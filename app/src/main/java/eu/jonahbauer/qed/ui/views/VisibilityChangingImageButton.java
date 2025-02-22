package eu.jonahbauer.qed.ui.views;


import android.content.Context;
import android.util.AttributeSet;

/**
 * An {@code ImageButton} with its visibility bound to its enabled state.
 *
 * When the button is disabled it is gone and when it is enabled it becomes visible.
 *
 * @see androidx.appcompat.widget.AppCompatImageButton
 */
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
        throw new UnsupportedOperationException("Visibility is bound to enabled state.");
    }
}
