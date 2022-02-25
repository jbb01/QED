package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.text.util.LinkifyCompat;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.RoundedCornerTreatment;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.ViewUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ExtendedMessageView extends LinearLayout implements MessageView {
    private CharSequence mName;
    private CharSequence mMessage;
    private CharSequence mTimestamp;
    private CharSequence mMessageId;
    private CharSequence mChannel;

    private final TextView mNameTextView;
    private final TextView mMessageTextView;
    private final TextView mTimestampTextView;
    private final TextView mIdTextView;
    private final TextView mChannelTextView;

    private boolean mColorful;
    private boolean mLinkify;

    public ExtendedMessageView(@NonNull Context context) {
        this(context, null);
    }

    public ExtendedMessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.extendedMessageStyle);
    }

    public ExtendedMessageView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ExtendedMessageView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        var array = context.obtainStyledAttributes(attrs, R.styleable.ExtendedMessageView, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.ExtendedMessageView, attrs, array, defStyleAttr, defStyleRes);
        }

        // config
        mLinkify = array.getBoolean(R.styleable.SimpleMessageView_linkify, false);
        mColorful = array.getBoolean(R.styleable.SimpleMessageView_colorful, false);

        // inflate
        var inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.message_view_extended, this, true);

        mNameTextView = findViewById(R.id.message_name);
        mMessageTextView = findViewById(R.id.message_message);
        mTimestampTextView = findViewById(R.id.message_timestamp);
        mIdTextView = findViewById(R.id.message_id);
        mChannelTextView = findViewById(R.id.message_channel);


        if (array.hasValue(R.styleable.ExtendedMessageView_name)) {
            setName(array.getText(R.styleable.ExtendedMessageView_name));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_message)) {
            setMessage(array.getText(R.styleable.ExtendedMessageView_message));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_timestamp)) {
            setTimestamp(array.getText(R.styleable.ExtendedMessageView_timestamp));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_channel)) {
            setTimestamp(array.getText(R.styleable.ExtendedMessageView_channel));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_messageId)) {
            setMessageId(array.getText(R.styleable.ExtendedMessageView_messageId));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_nameTextAppearance)) {
            setNameTextAppearance(array.getResourceId(R.styleable.ExtendedMessageView_nameTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_messageTextAppearance)) {
            setMessageTextAppearance(array.getResourceId(R.styleable.ExtendedMessageView_messageTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.ExtendedMessageView_dataTextAppearance)) {
            setDataTextAppearance(array.getResourceId(R.styleable.ExtendedMessageView_dataTextAppearance, 0));
        }

        setBackground(createBackground(this));
        setOrientation(VERTICAL);
    }

    @Override
    public void setName(CharSequence name) {
        this.mName = name;
        this.mNameTextView.setText(name);
    }

    @Override
    public CharSequence getName() {
        return mName;
    }

    @Override
    public void setMessage(CharSequence message) {
        this.mMessage = message;
        this.mMessageTextView.setText(message);
    }

    @Override
    public CharSequence getMessage() {
        return mMessage;
    }

    @Override
    public void setTimestamp(Instant instant) {
        setTimestamp(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                 .withZone(ZoneId.systemDefault())
                                 .format(instant)
        );
    }

    @Override
    public void setTimestamp(CharSequence timestamp) {
        this.mTimestamp = timestamp;
        this.mTimestampTextView.setText(timestamp);
    }

    @Override
    public CharSequence getTimestamp() {
        return mTimestamp;
    }

    public void setMessageId(CharSequence messageId) {
        this.mMessageId = messageId;
        this.mIdTextView.setText(messageId);
    }

    public CharSequence getMessageId() {
        return mMessageId;
    }

    public void setChannel(CharSequence channel) {
        this.mChannel = channel;
        this.mChannelTextView.setText(channel);
    }

    public CharSequence getChannel() {
        return mChannel;
    }

    @Override
    public void setMessage(Message message) {
        var context = getContext();
        setName(MessageView.formatName(context, message.getName(), message.getUserName()));
        setMessage(message.getMessage());
        setMessageId("#" + message.getId());
        setChannel(MessageView.formatChannel(context, message.getChannel()));
        setTimestamp(message.getDate());

        var color = ColorStateList.valueOf(message.getTransformedColor());
        setNameColor(color);
        if (mColorful) setMessageColor(color);
    }

    @Override
    public void setNameTextAppearance(@StyleRes int textAppearance) {
        this.mNameTextView.setTextAppearance(textAppearance);
    }

    @Override
    public void setMessageTextAppearance(@StyleRes int textAppearance) {
        this.mMessageTextView.setTextAppearance(textAppearance);
    }

    @Override
    public void setDataTextAppearance(@StyleRes int textAppearance) {
        this.mTimestampTextView.setTextAppearance(textAppearance);
        this.mChannelTextView.setTextAppearance(textAppearance);
        this.mIdTextView.setTextAppearance(textAppearance);
    }

    @Override
    public void setNameColor(ColorStateList color) {
        this.mNameTextView.setTextColor(color);
    }

    @Override
    public void setMessageColor(ColorStateList color) {
        this.mMessageTextView.setTextColor(color);
    }

    @Override
    public void setDataColor(ColorStateList color) {
        this.mIdTextView.setTextColor(color);
        this.mTimestampTextView.setTextColor(color);
        this.mChannelTextView.setTextColor(color);
    }

    @Override
    public void setColorful(boolean colorful) {
        this.mColorful = colorful;
    }

    @Override
    public boolean isColorful() {
        return mColorful;
    }

    @Override
    public void setLinkify(boolean linkify) {
        this.mLinkify = linkify;
        if (linkify) {
            if (LinkifyCompat.addLinks(mMessageTextView, Linkify.WEB_URLS)) {
                // prevent the textview from capturing all touch events
                // even those not on a link
                mMessageTextView.setMovementMethod(SimpleMessageView.BetterLinkMovementMethod.getInstance());
                mMessageTextView.setClickable(false);
                mMessageTextView.setLongClickable(false);
            }
        } else {
            mMessageTextView.setText(mMessage);
        }
    }

    @Override
    public boolean isLinkify() {
        return mLinkify;
    }

    private static Drawable createBackground(View view) {
        var metrics = view.getResources().getDisplayMetrics();

        var shape = ShapeAppearanceModel
                .builder()
                .setAllCorners(new RoundedCornerTreatment())
                .setAllCornerSizes(ViewUtils.dpToPx(metrics, 10))
                .build();

        var drawable = new MaterialShapeDrawable(shape);
        drawable.setFillColor(ColorStateList.valueOf(Color.WHITE));
        drawable.setPadding(
                (int) ViewUtils.dpToPx(metrics, 8),
                (int) ViewUtils.dpToPx(metrics, 5),
                (int) ViewUtils.dpToPx(metrics, 8),
                (int) ViewUtils.dpToPx(metrics, 5)
        );
        return drawable;
    }
}
