package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.activities.MainActivity;

public abstract class QEDFragment extends Fragment {
    protected final Handler mHandler = new Handler(Looper.getMainLooper());

    protected static final String ARGUMENT_THEME_ID = "themeId";
    protected static final String ARGUMENT_LAYOUT_ID = "layoutId";

    protected int mThemeId;
    protected int mLayoutId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Bundle args = getArguments();

        if (args != null) {
            mThemeId = getArguments().getInt(ARGUMENT_THEME_ID, 0);
            mLayoutId = getArguments().getInt(ARGUMENT_LAYOUT_ID, 0);
        }
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mThemeId != 0) {
            ContextThemeWrapper wrapper = new ContextThemeWrapper(inflater.getContext(), mThemeId);
            inflater = inflater.cloneInContext(wrapper);
        }

        return inflater.inflate(mLayoutId, container, false);
    }

    /**
     * Will be executed before the fragment is dropped.
     *
     * @param force if true the fragment will be dropped independent of the return value
     * @return  false iff the fragment mustn't be dropped now,
     *          true iff it may be dropped now,
     *          null iff no decision can be made now (e.g. wait for user input)
     *          if null is returned {@link MainActivity#onDropResult(boolean)} must be called
     *
     * @see MainActivity#onDropResult(boolean)
     */
    public Boolean onDrop(boolean force) {
        return true;
    }

    /**
     * Called by the {@link MainActivity} when the alternative toolbar borrowed to this fragment
     * is force closed.
     *
     * @see MainActivity#borrowAltToolbar()
     * @see MainActivity#returnAltToolbar()
     */
    public void revokeAltToolbar() {}
}
