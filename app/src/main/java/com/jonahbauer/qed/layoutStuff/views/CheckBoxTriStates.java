package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.jonahbauer.qed.R;

/**
 * Base on https://stackoverflow.com/a/40939367/3950497 answer.
 */
public class CheckBoxTriStates extends MaterialCheckBox {

    static public final int UNKNOWN = -1;

    static public final int UNCHECKED = 0;

    static public final int CHECKED = 1;

    private int mState;

    /**
     * This is the listener set to the super class which is going to be evoke each
     * time the check state has changed.
     */
    private final OnCheckedChangeListener mPrivateListener = (CompoundButton buttonView, boolean isChecked) -> {
        // checkbox status is changed from uncheck to checked.
        switch (mState) {
            case UNKNOWN:
                setState(UNCHECKED);
                break;
            case UNCHECKED:
                setState(CHECKED);
                break;
            case CHECKED:
                setState(UNKNOWN);
                break;
        }
    };

    /**
     * Holds a reference to the listener set by a client, if any.
     */
    private OnCheckedChangeListener mClientListener;

    /**
     * This flag is needed to avoid accidentally changing the current {@link #mState} when
     * {@link #onRestoreInstanceState(Parcelable)} calls {@link #setChecked(boolean)}
     * evoking our {@link #mPrivateListener} and therefore changing the real state.
     */
    private boolean mRestoring;

    public CheckBoxTriStates(Context context) {
        this(context, null);
    }

    public CheckBoxTriStates(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.triCheckboxStyle);
    }

    public CheckBoxTriStates(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getState() {
        return mState;
    }

    private void setState(int state) {
        if(!this.mRestoring && this.mState != state) {
            setChecked(state != UNKNOWN);
            this.mState = state;

            if(this.mClientListener != null) {
                this.mClientListener.onCheckedChanged(this, this.isChecked());
            }

            updateBtn(false);
        }
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {

        // we never truly set the listener to the client implementation, instead we only hold
        // a reference to it and evoke it when needed.
        if(this.mPrivateListener != listener) {
            this.mClientListener = listener;
        }

        // always use our implementation
        super.setOnCheckedChangeListener(mPrivateListener);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.mState = mState;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        this.mRestoring = true; // indicates that the ui is restoring its state
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setState(ss.mState);
        requestLayout();
        this.mRestoring = false;
    }

    private void init() {
        mState = UNKNOWN;
        updateBtn(true);
        setOnCheckedChangeListener(this.mPrivateListener);
    }

    private void updateBtn(boolean isInit) {
        int btnDrawable = R.drawable.ic_checkbox_checked_to_unknown;
        switch (mState) {
            case UNKNOWN:
                if (isInit) btnDrawable = R.drawable.ic_checkbox_unknown;
                else btnDrawable = R.drawable.ic_checkbox_checked_to_unknown_animated;
                break;
            case UNCHECKED:
                if (isInit) btnDrawable = R.drawable.ic_checkbox_unchecked;
                else btnDrawable = R.drawable.ic_checkbox_unknown_to_unchecked_animated;
                break;
            case CHECKED:
                if (isInit) btnDrawable = R.drawable.ic_checkbox_checked;
                else btnDrawable = R.drawable.ic_checkbox_unchecked_to_checked_animated;
                break;
        }
        setButtonDrawable(AppCompatResources.getDrawable(getContext(), btnDrawable));
        if (getButtonDrawable() instanceof Animatable) ((Animatable)getButtonDrawable()).start();
    }

    static class SavedState extends BaseSavedState {
        int mState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mState = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(mState);
        }

        @NonNull
        @Override
        public String toString() {
            return "CheckboxTriState.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " state=" + mState + "}";
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}