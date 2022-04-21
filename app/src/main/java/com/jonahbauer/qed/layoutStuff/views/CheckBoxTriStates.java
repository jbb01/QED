package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;

import java.util.Objects;

import lombok.RequiredArgsConstructor;

/**
 * Based on https://stackoverflow.com/a/40939367/3950497.
 */
public class CheckBoxTriStates extends MaterialCheckBox {
    private State mState = State.UNKNOWN;

    private final OnCheckedChangeListener mSuperListener = this::onCheckedChanged;
    private OnCheckedChangeListener mListener;

    private boolean mChangingState;

    public CheckBoxTriStates(@NonNull Context context) {
        this(context, null);
    }

    public CheckBoxTriStates(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.triCheckboxStyle);
    }

    public CheckBoxTriStates(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mState = State.UNKNOWN;
        updateDrawable(true);
        super.setOnCheckedChangeListener(mSuperListener);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mChangingState) return;
        switch (mState) {
            case UNKNOWN:
                setState(State.UNCHECKED);
                break;
            case UNCHECKED:
                setState(State.CHECKED);
                break;
            case CHECKED:
                setState(State.UNKNOWN);
                break;
        }
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mListener = listener;
    }

    private void updateDrawable(boolean isInit) {
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

    public @NonNull State getState() {
        return mState;
    }

    private void setState(@NonNull State state) {
        Objects.requireNonNull(state);
        if (this.mState != state) {
            this.mState = state;

            mChangingState = true;
            setChecked(state != State.UNKNOWN);
            updateDrawable(false);
            mChangingState = false;

            if (this.mListener != null) {
                this.mListener.onCheckedChanged(this, isChecked());
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        var superState = super.onSaveInstanceState();
        var ss = new SavedState(superState);
        ss.mState = mState;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        var ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mState = ss.mState;
        updateDrawable(true);
    }

    static class SavedState extends BaseSavedState {
        State mState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mState = ParcelExtensions.readEnum(in, State.values());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            ParcelExtensions.writeEnum(out, mState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<>() {
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

    @RequiredArgsConstructor
    public enum State {
        UNKNOWN(null),
        UNCHECKED(false),
        CHECKED(true);

        private final Boolean value;

        public @Nullable Boolean asBoolean() {
            return value;
        }
    }
}