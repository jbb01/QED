package com.jonahbauer.qed.activities.messageInfoSheet;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.layoutStuff.MathView;
import com.jonahbauer.qed.layoutStuff.MessageView;

public class MessageInfoFragment extends Fragment implements View.OnScrollChangeListener {
    private static final String ARGUMENT_MESSAGE = "message";

    private static final int[] backgroundResId = {R.drawable.background_0, R.drawable.background_2, R.drawable.background_3, R.drawable.background_4,
                                                  R.drawable.background_5, R.drawable.background_6, R.drawable.background_7, R.drawable.background_7};

    private Message mMessage;
    private boolean mKatex;

    private NestedScrollView mContent;
    private MessageView mMessageView;
    private View mBackground;
    private View mContentShadow;
    private View mToolbarShadow;
    private Toolbar mToolbar;

    private float mActionBarSize;

    public static MessageInfoFragment newInstance(Message message) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_MESSAGE, message);
        MessageInfoFragment fragment = new MessageInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public MessageInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            mMessage = args.getParcelable(ARGUMENT_MESSAGE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mKatex = PreferenceManager.getDefaultSharedPreferences(inflater.getContext()).getBoolean(Pref.Chat.KATEX, false);
        return inflater.inflate(R.layout.fragment_message_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContent = view.findViewById(R.id.content);
        mMessageView = view.findViewById(R.id.message);
        mBackground = view.findViewById(R.id.background);
        mContentShadow = view.findViewById(R.id.content_shadow);

        mToolbarShadow = view.findViewById(R.id.toolbar_shadow);
        mToolbar = view.findViewById(R.id.toolbar);

        // this is equal to #CCC multiplied by transformedColor (the same as the dark regions of background)
        int darkColor = Color.rgb(
                Color.red(mMessage.transformedColor) * 204 / 255,
                Color.green(mMessage.transformedColor) * 204 / 255,
                Color.blue(mMessage.transformedColor) * 204 / 255);

        if (mMessage != null) {
            View backgroundLayout = view.findViewById(R.id.background_layout);
            // choose random background image and set color
            mBackground.setBackgroundResource(backgroundResId[(int) mMessage.id % backgroundResId.length]);
            mBackground.setBackgroundTintList(ColorStateList.valueOf(mMessage.transformedColor));
            backgroundLayout.setBackgroundColor(darkColor);
            if (mToolbar != null) {
                mToolbar.setBackgroundColor(darkColor);
                mToolbar.setTitleTextColor(Color.WHITE);
                mToolbar.setTitle(mMessage.name);
            }

            // wait for layout
            mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mBackground.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // make background 16:9
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        int height = mBackground.getHeight();
                        int width = height * 9 / 16;

                        ViewGroup.LayoutParams layoutParams = mBackground.getLayoutParams();
                        if (layoutParams != null) {
                            layoutParams.width = width;
                        } else {
                            layoutParams = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
                        }
                        mBackground.setLayoutParams(layoutParams);
                    } else {
                        int width = mBackground.getWidth();
                        int height = width * 9 / 16;

                        ViewGroup.LayoutParams layoutParams = mBackground.getLayoutParams();
                        if (layoutParams != null) {
                            layoutParams.height = height;
                        } else {
                            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                        }

                        mBackground.setLayoutParams(layoutParams);
                    }

                    // give layout time to happen
                    mBackground.post(() -> view.<MessageView>findViewById(R.id.message).ellipsize());


                    // forward touch event on header to content scroll view
                    mBackground.setOnTouchListener(new View.OnTouchListener() {
                        private boolean mIntercepted;

                        @SuppressLint("ClickableViewAccessibility")
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);

                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                mIntercepted = mContent.onInterceptTouchEvent(event);
                            } else if (!mIntercepted) {
                                mIntercepted = mContent.onInterceptTouchEvent(event);
                            }

                            if (mIntercepted) {
                                return mContent.onTouchEvent(event);
                            }

                            return true;
                        }
                    });
                }
            });

            TextView valueId = view.findViewById(R.id.value_id);
            TextView valueName = view.findViewById(R.id.value_name);
            TextView valueUsername = view.findViewById(R.id.value_username);
            TextView valueTimestamp = view.findViewById(R.id.value_timestamp);
            TextView valueBottag = view.findViewById(R.id.value_bottag);
            TextView valueMessage = view.findViewById(R.id.value_message);
            TextView valueChannel = view.findViewById(R.id.value_channel);
            ViewStub valueMessageMathStub = view.findViewById(R.id.value_message_math_stub);

            // set values
            valueId.setText(String.valueOf(mMessage.id));
            valueName.setText(mMessage.name);
            valueTimestamp.setText(mMessage.date);
            valueBottag.setText(String.valueOf(mMessage.bottag));
            valueMessage.setText(mMessage.message);
            valueChannel.setText(mMessage.channel.equals("") ? "(main)" : mMessage.channel);

            if (mKatex) {
                MathView mathView = (MathView) valueMessageMathStub.inflate();
                mathView.setText(mMessage.message);
                valueMessage.setVisibility(View.GONE);
            }

            if (mMessage.userName != null) {
                valueUsername.setText(String.format("%s (%s)", mMessage.userName, mMessage.userId));
                view.findViewById(R.id.layout_username).setVisibility(View.VISIBLE);
                view.findViewById(R.id.icon_username).setVisibility(View.VISIBLE);
            }

            mMessageView.setMessage(mMessage);
        }

        // setup fading
        TypedValue typedValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
        mActionBarSize = typedValue.getDimension(getResources().getDisplayMetrics());


        if (mToolbar != null) {
            mContent.setOnScrollChangeListener(this);
        }
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        // fade out header
        float max = mContent.getTop() - mActionBarSize;
        float alpha = 1 - scrollY / max;

        if (alpha < 0) alpha = 0;
        else if (alpha > 1) alpha = 1;

        mMessageView.setAlpha(alpha);
        mBackground.setAlpha(alpha);
        mContentShadow.setTranslationY(- scrollY);


        // fade in toolbar
        // when toolbar != null then toolbarShadow must be != null
        assert mToolbarShadow != null;

        // scrollY > top
        float toolbarTranslationFactor = ((mContent.getTop() - scrollY) / mActionBarSize) - 1;
        if (toolbarTranslationFactor < 0) toolbarTranslationFactor = 0;
        else if (toolbarTranslationFactor > 1) toolbarTranslationFactor = 1;

        if (toolbarTranslationFactor == 1) {
            mToolbar.setVisibility(View.GONE);
            mToolbarShadow.setVisibility(View.GONE);
            mContentShadow.setAlpha(1);
        } else {
            mToolbar.setTranslationY(- toolbarTranslationFactor * mActionBarSize);
            mToolbarShadow.setTranslationY(- toolbarTranslationFactor * mActionBarSize);
            mToolbar.setVisibility(View.VISIBLE);
            mToolbarShadow.setVisibility(View.VISIBLE);

            mContentShadow.setAlpha(toolbarTranslationFactor);
        }
    }
}
