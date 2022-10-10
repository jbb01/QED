package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemMessageBinding;
import com.jonahbauer.qed.layoutStuff.views.ExtendedMessageView;
import com.jonahbauer.qed.layoutStuff.views.MathView;
import com.jonahbauer.qed.layoutStuff.views.SimpleMessageView;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.ViewUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends ArrayAdapter<Message> {
    public static final int INVALID_POSITION = -1;

    private final Context mContext;
    private final ListView mListView;
    private final List<Message> mData;

    // Date Banners
    private final DateTimeFormatter mDateBannerFormat = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault());

    // Settings
    private boolean mLinkify;
    private boolean mColorful;
    private boolean mKatex;
    private final boolean mExtended;

    private boolean mKatexSet;
    private final boolean mLinkifySet;

    @Px
    private int mDefaultTextSize;
    @ColorInt
    private int mDefaultTextColor;
    private final LinearLayout mathPreload;

    private int mCheckedItemPosition = INVALID_POSITION;

    public MessageAdapter(@NonNull ListView list, @Nullable LinearLayout mathPreload) {
        this(list, mathPreload, null, null, false);
    }

    public MessageAdapter(@NonNull ListView list, @Nullable LinearLayout mathPreload, @Nullable Boolean katex, @Nullable Boolean linkify, boolean extended) {
        this(list, new ArrayList<>(100), mathPreload, katex, linkify, extended);
    }

    private MessageAdapter(@NonNull ListView list, @NonNull List<Message> data, @Nullable LinearLayout mathPreload, @Nullable Boolean katex, @Nullable Boolean linkify, boolean extended) {
        super(list.getContext(), 0, data);
        this.mContext = list.getContext();
        this.mListView = list;
        this.mData = data;
        this.mathPreload = mathPreload;

        this.mExtended = extended;

        this.mLinkifySet = linkify != null;
        if (this.mLinkifySet) this.mLinkify = linkify;

        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        if (mExtended && mKatexSet && mKatex) throw new IllegalArgumentException("Extended message views do not support Katex!");

        obtainDefaultTextAppearance();
        reload();
    }

    private void obtainDefaultTextAppearance() {
        mDefaultTextSize = (int) ViewUtils.spToPx(mContext, 14);
        mDefaultTextColor = Color.BLACK;

        var styleable = mExtended
                ? R.styleable.ExtendedMessageView_messageTextAppearance
                : R.styleable.SimpleMessageView_messageTextAppearance;
        var defStyleAttr = mExtended
                ? R.attr.extendedMessageStyle
                : R.attr.messageStyle;
        var defStyleRes = mExtended
                ? R.style.Widget_App_Message_Extended
                : R.style.Widget_App_Message;

        var array = mContext.obtainStyledAttributes(null, new int[] {styleable}, defStyleAttr, defStyleRes);
        var textAppearance = array.getResourceId(styleable, 0);
        array.recycle();

        if (textAppearance != 0) {
            var taArray = mContext.obtainStyledAttributes(textAppearance, new int[] {R.styleable.TextAppearance_android_textColor, R.styleable.TextAppearance_android_textSize});
            mDefaultTextSize = taArray.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, mDefaultTextSize);
            mDefaultTextColor = taArray.getColor(R.styleable.TextAppearance_android_textColor, mDefaultTextColor);
            taArray.recycle();
        }
    }

    private ExtendedMessageView getExtendedView(View convertView) {
        if (convertView instanceof ExtendedMessageView) {
            var messageView = (ExtendedMessageView) convertView;
            if (!messageView.isColorful() || mColorful) return messageView;
        }
        return new ExtendedMessageView(mContext);
    }

    private SimpleMessageView getSimpleView(View convertView, int position, Message message) {
        ListItemMessageBinding binding = null;

        if (convertView != null && convertView.getTag() instanceof ListItemMessageBinding) {
            binding = (ListItemMessageBinding) convertView.getTag();
            var messageView = binding.message;
            if (messageView.isColorful() && !mColorful) binding = null;
        }

        if (binding == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            binding = ListItemMessageBinding.inflate(inflater, null, false);
            binding.getRoot().setTag(binding);
        }

        var lp = binding.message.getLayoutParams();
        lp.width = mKatex ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT;
        binding.message.setLayoutParams(lp);

        var previousMessage = position == 0 ? null : getItem(position - 1);
        if (previousMessage == null || !Objects.equals(message.getLocalDate(), previousMessage.getLocalDate())) {
            binding.messageDateBanner.setVisibility(View.VISIBLE);
            binding.messageDateBanner.setText(formatBanner(message.getLocalDate()));
        } else {
            binding.messageDateBanner.setVisibility(View.GONE);
        }

        return binding.message;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Message message = getItem(position);

        var view = mExtended
                ? getExtendedView(convertView)
                : getSimpleView(convertView, position, message);

        view.setColorful(mColorful);

        if (view instanceof SimpleMessageView) {
            var simple = (SimpleMessageView) view;
            simple.setKatex(mKatex);
            simple.setMathPreload(mathPreload);
        }

        view.setMessage(message);
        view.setLinkify(mLinkify);
        view.setFocusable(false);
        view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        var root = mExtended ? view : (View) view.getParent();
        savePosition(position, root);
        root.setActivated(position == mCheckedItemPosition);
        return root;
    }

    public List<Message> getData() {
        return new ArrayList<>(mData);
    }

    @Override
    public void add(Message message) {
        super.add(message);

        if (mKatex) {
            preloadMath(message);
        }
    }

    @Override
    public void addAll(@NonNull Collection<? extends Message> collection) {
        super.addAll(collection);

        if (mKatex) {
            for (Message message : collection) {
                preloadMath(message);
            }
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Nullable
    @Override
    public Message getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        var item = getItem(position);
        return item != null ? item.getId() : Message.NO_ID;
    }

    @Override
    public void clear() {
        super.clear();
        mCheckedItemPosition = INVALID_POSITION;
        reload();
    }

    public void reload() {
        mColorful = Preferences.getChat().isColorful();
        if (!mLinkifySet)
            mLinkify = Preferences.getChat().isLinkify();
        if (!mKatexSet)
            mKatex = Preferences.getChat().isKatex();

        MathView.clearCache();
        if (mathPreload != null) mathPreload.removeAllViews();
        if (mKatex && !mExtended) {
            for (Message message : mData) {
                preloadMath(message);
            }
        }
    }

    public void setKatex(Boolean katex) {
        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        reload();
    }

    private void preloadMath(Message message) {
        MathView.extractAndPreload(
                mContext,
                message.getMessage(),
                mDefaultTextSize,
                mColorful ? message.getColor(getContext()) : mDefaultTextColor,
                mathPreload
        );
    }

    private String formatBanner(LocalDate date) {
        return mDateBannerFormat.format(date);
    }

    private void savePosition(int position, View view) {
        view.setTag(R.id.tag_position, position);
    }

    public void setCheckedItemPosition(int position) {
        if (mCheckedItemPosition != position) {
            var oldCheckedView = getViewForPosition(mCheckedItemPosition);
            if (oldCheckedView != null) {
                oldCheckedView.setActivated(false);
            }

            mCheckedItemPosition = position;

            var newCheckedView = getViewForPosition(mCheckedItemPosition);
            if (newCheckedView != null) {
                newCheckedView.setActivated(true);
            }
        }
    }

    public int getCheckedItemPosition() {
        return mCheckedItemPosition;
    }

    private @Nullable View getViewForPosition(int position) {
        var firstPosition = mListView.getFirstVisiblePosition();
        var lastPosition = firstPosition + mListView.getChildCount() - 1;

        if (position < firstPosition || position > lastPosition) {
            return null;
        } else {
            var out = mListView.getChildAt(position - firstPosition);
            var viewPosition = (int) out.getTag(R.id.tag_position);
            if (viewPosition != position) throw new RuntimeException();
            return out;
        }
    }

}
