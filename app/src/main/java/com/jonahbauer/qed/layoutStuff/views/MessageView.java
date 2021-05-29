package com.jonahbauer.qed.layoutStuff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;

@SuppressWarnings({"unused"})
public class MessageView extends RelativeLayout {
    private static final String TRIM = "(^[\\s\\u200B]*|[\\s\\u200B]*$)";
    private static final String TRIM_END = "[\\s\\u200B]*$";
    private static float dp = 0;

    private TextView mNameTextView;
    private TextView mMessageTextView;
    private TextView mDateTextView;
    private TextView mDateBannerTextView;
    private TextView mChannelTextView;
    private TextView mIdTextView;

    private MathView mMessageMathView;
    private ViewStub mMessageMathViewStub;

    private View mTriangleView;
    private ViewGroup mMessageLayout;
    private LinearLayout mMessageContent;

    private boolean mExtended;
    private boolean mLinkify;
    private boolean mColorful;
    private boolean mKatex;

    private String mName;
    private String mMessage;
    private String mDate;
    private String mDateBanner;
    private String mChannel;
    private String mId;

    @Dimension @Px private float mNameTextSize;
    @ColorInt private int mNameTextColor;
    @StyleRes private int mNameTextAppearance;
    private boolean mNameTextSizeSet;
    private boolean mNameTextColorSet;
    private boolean mNameTextAppearanceSet;

    @Dimension @Px private float mDateTextSize;
    @ColorInt private int mDateTextColor;
    @StyleRes private int mDateTextAppearance;
    private boolean mDateTextSizeSet;
    private boolean mDateTextColorSet;
    private boolean mDateTextAppearanceSet;

    @Dimension @Px private float mDataTextSize;
    @ColorInt private int mDataTextColor;
    @StyleRes private int mDataTextAppearance;
    private boolean mDataTextSizeSet;
    private boolean mDataTextColorSet;
    private boolean mDataTextAppearanceSet;

    @Dimension @Px private float mMessageTextSize;
    @ColorInt private int mMessageTextColor;
    @StyleRes private int mMessageTextAppearance;
    private boolean mMessageTextSizeSet;
    private boolean mMessageTextColorSet;
    private boolean mMessageTextAppearanceSet;

    private ColorStateList mMessageTextColorsNotColorful;
    @ColorInt private int mMessageTextColorNotColorful;
    private boolean mMessageTextColorSetNotColorful;

    @Dimension @Px private float mDateBannerTextSize;
    @ColorInt private int mDateBannerTextColor;
    @StyleRes private int mDateBannerTextAppearance;
    private boolean mDateBannerTextSizeSet;
    private boolean mDateBannerTextColorSet;
    private boolean mDateBannerTextAppearanceSet;

    private ColorStateList mBackgroundColor;
    private boolean mBackgroundColorSet;

    private ColorStateList mColorPressedHighlight;
    private boolean mColorPressedHighlightSet;

    private int mWidth;
    private long mMessageId = -1;

    public MessageView(Context context) {
        this(context, (Boolean) null);
    }

    public MessageView(Context context, Boolean extended) {
        this(context, null, extended);
    }

    public MessageView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public MessageView(Context context, AttributeSet attrs, Boolean extended) {
        this(context, attrs, 0, extended);
    }

    public MessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public MessageView(Context context, AttributeSet attrs, int defStyleAttr, Boolean extended) {
        super(applyTheme(context), attrs, defStyleAttr);

        if (dp == 0) {
            dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        }

        obtainAttributes(this.getContext(), attrs, defStyleAttr);

        if (extended != null)
            this.mExtended = extended;

        reload();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
    }

    /**
     * Wraps the given context in the theme specified in {@code MessageStyle} attribute.
     */
    @NonNull
    private static Context applyTheme(@NonNull Context context) {
        TypedValue messageStyle = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.messageStyle, messageStyle, true);

        if (messageStyle.resourceId != 0) {
            context = new ContextThemeWrapper(context, messageStyle.resourceId);
        }

        return context;
    }

    /**
     * Extracts all {@code MessageView} attributes from the given {@code AttributeSet} and stores
     * them to the member variables of this object.
     *
     * @param context a context
     * @param attrs a attribute set
     * @param defStyleAttr a
     */
    private void obtainAttributes(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MessageView, defStyleAttr, 0);

        TypedValue elevationValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.elevation, elevationValue, true);
        setElevation(elevationValue.getDimension(getResources().getDisplayMetrics()));

        mNameTextSize = typedArray.getDimension(R.styleable.MessageView_nameTextSize, 0);
        mNameTextColor = typedArray.getColor(R.styleable.MessageView_nameTextColor, 0);
        mNameTextAppearance = typedArray.getResourceId(R.styleable.MessageView_nameTextAppearance, 0);
        mNameTextSizeSet = typedArray.hasValue(R.styleable.MessageView_nameTextSize);
        mNameTextColorSet = typedArray.hasValue(R.styleable.MessageView_nameTextColor);
        mNameTextAppearanceSet = typedArray.hasValue(R.styleable.MessageView_nameTextAppearance);

        mDateTextSize = typedArray.getDimension(R.styleable.MessageView_dateTextSize, 0);
        mDateTextColor = typedArray.getColor(R.styleable.MessageView_dateTextColor, 0);
        mDateTextAppearance = typedArray.getResourceId(R.styleable.MessageView_dateTextAppearance, 0);
        mDateTextSizeSet = typedArray.hasValue(R.styleable.MessageView_dateTextSize);
        mDateTextColorSet = typedArray.hasValue(R.styleable.MessageView_dateTextColor);
        mDateTextAppearanceSet = typedArray.hasValue(R.styleable.MessageView_dateTextAppearance);
        
        mDataTextSize = typedArray.getDimension(R.styleable.MessageView_dataTextSize, 0);
        mDataTextColor = typedArray.getColor(R.styleable.MessageView_dataTextColor, 0);
        mDataTextAppearance = typedArray.getResourceId(R.styleable.MessageView_dataTextAppearance, 0);
        mDataTextSizeSet = typedArray.hasValue(R.styleable.MessageView_dataTextSize);
        mDataTextColorSet = typedArray.hasValue(R.styleable.MessageView_dataTextColor);
        mDataTextAppearanceSet = typedArray.hasValue(R.styleable.MessageView_dataTextAppearance);

        mMessageTextSize = typedArray.getDimension(R.styleable.MessageView_messageTextSize, 0);
        mMessageTextColor = typedArray.getColor(R.styleable.MessageView_messageTextColor, 0);
        mMessageTextAppearance = typedArray.getResourceId(R.styleable.MessageView_messageTextAppearance, 0);
        mMessageTextSizeSet = typedArray.hasValue(R.styleable.MessageView_messageTextSize);
        mMessageTextColorSet = typedArray.hasValue(R.styleable.MessageView_messageTextColor);
        mMessageTextAppearanceSet = typedArray.hasValue(R.styleable.MessageView_messageTextAppearance);

        mDateBannerTextSize = typedArray.getDimension(R.styleable.MessageView_dateBannerTextSize, 0);
        mDateBannerTextColor = typedArray.getColor(R.styleable.MessageView_dateBannerTextColor, 0);
        mDateBannerTextAppearance = typedArray.getResourceId(R.styleable.MessageView_dateBannerTextAppearance, 0);
        mDateBannerTextSizeSet = typedArray.hasValue(R.styleable.MessageView_dateBannerTextSize);
        mDateBannerTextColorSet = typedArray.hasValue(R.styleable.MessageView_dateBannerTextColor);
        mDateBannerTextAppearanceSet = typedArray.hasValue(R.styleable.MessageView_dateBannerTextAppearance);

        mBackgroundColor = typedArray.getColorStateList(R.styleable.MessageView_android_colorBackground);
        mBackgroundColorSet = typedArray.hasValue(R.styleable.MessageView_android_colorBackground);

        mColorPressedHighlight = typedArray.getColorStateList(R.styleable.MessageView_android_colorPressedHighlight);
        mColorPressedHighlightSet = typedArray.hasValue(R.styleable.MessageView_android_colorPressedHighlight);

        mName = typedArray.getString(R.styleable.MessageView_name);
        mMessage = typedArray.getString(R.styleable.MessageView_message);
        mDate = typedArray.getString(R.styleable.MessageView_date);
        mChannel = typedArray.getString(R.styleable.MessageView_channel);
        mId = typedArray.getString(R.styleable.MessageView_id);

        mExtended = typedArray.getBoolean(R.styleable.MessageView_extended, false);
        mLinkify = typedArray.getBoolean(R.styleable.MessageView_linkify, false);
        mKatex = typedArray.getBoolean(R.styleable.MessageView_katex, false);
        mColorful = typedArray.getBoolean(R.styleable.MessageView_colorful, false);

        typedArray.recycle();
    }

    /**
     * Completely reconstructs this message view by removing all children and re-inflating the layout.
     */
    private void reload() {
        this.removeAllViews();

        if (mExtended) {
            LayoutInflater.from(this.getContext()).inflate(R.layout.message_view_extended, this, true);
        } else {
            LayoutInflater.from(this.getContext()).inflate(R.layout.message_view, this, true);
        }

        mNameTextView = findViewById(R.id.message_name);
        mMessageTextView = findViewById(R.id.message_message);
        mMessageMathViewStub = findViewById(R.id.message_message_math_stub);
        mDateTextView = findViewById(R.id.message_date);
        mDateBannerTextView = findViewById(R.id.message_date_banner);
        mTriangleView = findViewById(R.id.message_layout_tip);
        mMessageLayout = findViewById(R.id.message_layout);
        mMessageContent = findViewById(R.id.message_content);
        mChannelTextView = findViewById(R.id.message_channel);
        mIdTextView = findViewById(R.id.message_id);

        setupElevation();

        setupTime();

        applyValues();
    }

    private void applyValues() {
        mNameTextView.setText(mName);
        mDateTextView.setText(mDate);
        mMessageTextView.setText(mMessage);
        if (mExtended) {
            mChannelTextView.setText(mChannel);
            mIdTextView.setText(mId);
        }
        if (mKatex) mMessageMathView.setText(mMessage, mMessageId);

        if (!mExtended) {
            mDateBannerTextView.setText(mDateBanner);
            mDateBannerTextView.setVisibility(mDateBanner == null ? GONE : VISIBLE);
        }

        if (mNameTextAppearanceSet) mNameTextView.setTextAppearance(mNameTextAppearance);
        if (mNameTextColorSet) mNameTextView.setTextColor(mNameTextColor);
        if (mNameTextSizeSet) mNameTextView.setTextSize(mNameTextSize);

        if (mMessageTextAppearanceSet) {
            mMessageTextView.setTextAppearance(mMessageTextAppearance);
            if (mKatex) mMessageMathView.setTextAppearance(mMessageTextAppearance);
        }
        if (mMessageTextColorSet) {
            mMessageTextView.setTextColor(mMessageTextColor);
            if (mKatex) mMessageMathView.setTextColor(mMessageTextColor);
        }
        if (mMessageTextSizeSet) {
            mMessageTextView.setTextSize(mMessageTextSize);
            if (mKatex) mMessageMathView.setTextSize(mMessageTextSize);
        }

        if (mDateTextAppearanceSet) mDateTextView.setTextAppearance(mDateTextAppearance);
        if (mDateTextColorSet) mDateTextView.setTextColor(mDateTextColor);
        if (mDateTextSizeSet) mDateTextView.setTextSize(mDateTextSize);

        if (!mExtended) {
            if (mDateBannerTextAppearanceSet) mDateBannerTextView.setTextAppearance(mDateBannerTextAppearance);
            if (mDateBannerTextColorSet) mDateBannerTextView.setTextColor(mDateBannerTextColor);
            if (mDateBannerTextSizeSet) mDateBannerTextView.setTextSize(mDateBannerTextSize);
        }

        if (mDataTextAppearanceSet && mExtended) {
            mChannelTextView.setTextAppearance(mDataTextAppearance);
            mIdTextView.setTextAppearance(mDataTextAppearance);
            mDateTextView.setTextAppearance(mDataTextAppearance);
        }
        if (mDataTextColorSet && mExtended) {
            mChannelTextView.setTextColor(mDataTextColor);
            mIdTextView.setTextColor(mDataTextColor);
            mDateTextView.setTextColor(mDataTextColor);
        }
        if (mDataTextSizeSet && mExtended) {
            mChannelTextView.setTextSize(mDataTextSize);
            mIdTextView.setTextSize(mDataTextSize);
            mDateTextView.setTextSize(mDataTextSize);
        }

        if (mBackgroundColorSet) {
            setBackgroundColor(mBackgroundColor);
        }
    }

    private void setupElevation() {
        float elevation = getElevation();

        if (!mExtended) {
            mTriangleView.setElevation(elevation);
            mTriangleView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    Path path = new Path();
                    path.moveTo(0, 0);
                    path.lineTo(view.getWidth(), 0);
                    path.lineTo(view.getWidth(), view.getHeight());
                    path.close();

                    outline.setConvexPath(path);
                }
            });
        }

        mMessageLayout.setElevation(elevation);
        mMessageLayout.setClipToPadding(false);
        mMessageLayout.setClipChildren(false);
        mMessageLayout.setClipToOutline(false);

        setClipToPadding(false);
        setClipToOutline(false);
        setClipChildren(false);
    }

    private void setupTime() {
        if (!mExtended && mMessage != null) {
            StringBuilder messageText = new StringBuilder(mMessage);

            float dateWidth = mDateTextView.getPaint().measureText(mDate) + 5 * dp;
            float spaceWidth = mMessageTextView.getPaint().measureText("\u0020");

            int spaceCount = (int) Math.ceil(dateWidth / spaceWidth);

            for (int i = 0; i < spaceCount; i++)
                messageText.append('\u0020');

            messageText.append('\u200B');

            mMessage = messageText.toString();
        }
    }

    /**
     * Calculates the maximum number of lines that fits in the given text view without an overflow.
     */
    private static int getMaxLineCount(TextView textView) {
        int height = textView.getHeight();
        int lineHeight = textView.getLineHeight();
        int lineCount = textView.getLineCount();
        float lineSpacingMultiplier = textView.getLineSpacingMultiplier();
        float lineSpacingExtra = textView.getLineSpacingExtra();

        return (int) ((height + lineSpacingMultiplier + lineSpacingExtra) / (lineHeight * lineSpacingMultiplier + lineSpacingExtra));
    }

    /**
     * Trims the text view to {@code maxLineCount} lines.
     * The last line's content will not be longer than {@code maxWidth}.
     * The last line will be filled with trailing whitespaces to reach {@code targetWidth}.
     *
     * @return the tailing part of the text views content that got replaced with an ellipsis character
     */
    private static String ellipsize(TextView textView, int maxLineCount, float maxWidth, float targetWidth) {
        // all parts that get removed
        StringBuilder out = new StringBuilder();

        String text = textView.getText().toString();
        Layout layout = textView.getLayout();

        // trim to maxLineCount
        int currentLineCount = textView.getLineCount();
        if (currentLineCount > maxLineCount) {
            int start = layout.getLineStart(maxLineCount);

            out = new StringBuilder(text.substring(start + 1));
            text = text.substring(0, start);
        }

        // remove tailing whitespaces
        text = text.trim();
        text = text.replaceAll(TRIM, "");

        int lineStart = layout.getLineStart(maxLineCount - 1);
        // if the last line is no longer existing the complete string will fit into the text view
        if (lineStart <= text.length()) {
            StringBuilder line = new StringBuilder(text.substring(lineStart).replaceAll(TRIM_END, ""));

            Paint paint = textView.getPaint();

            float lineWidth = paint.measureText(line.toString());
            float ellipsisWidth = paint.measureText(" \u2026");

            if (lineWidth > maxWidth - (out.length() > 0 ? ellipsisWidth : 0)) { // text does not fit in the line
                // since there will be an ellipsis character the maxWidth needs to be reduced
                maxWidth -= ellipsisWidth;
                if (maxWidth < 0f) maxWidth = 0f;

                // remove words until the last line's width is below maxWidth
                while (lineWidth > maxWidth) {
                    // (non whitespace) (zero width match) (group of whitespace) (non whitespace) (end of string)
                    String[] split = line.toString().split("(?<!\\s)(?=\\s+\\S*$)");
                    if (split.length > 1) {
                        out.insert(0, split[1]);
                    } else {
                        break; // no more spaces in string --> cant delete words
                    }
                    line.setLength(split[0].length());
                    lineWidth = paint.measureText(line.toString());
                }

                // remove chars until the last line's width is below maxWidth
                while (lineWidth > maxWidth) {
                    int length = line.length();
                    out.insert(0, line.charAt(length - 1));
                    line.setLength(length - 1);
                    lineWidth = paint.measureText(line.toString());
                }

                // add ellipsis character
                line.append(" \u2026");
            } else if (out.length() > 0) {
                line.append(" \u2026");
            }

            // fill with spaces to target width
            lineWidth = paint.measureText(line.toString());
            float spaceWidth = paint.measureText(" ");
            int spaceCount = (int) ((targetWidth - lineWidth) / spaceWidth - 1);

            for (int i = 0; i < spaceCount; i++) {
                line.append("\u0020");
            }

            text = text.substring(0, lineStart) + line;
        } else if (out.length() > 0) {
            // if last line should not be changed, but other lines were removed, just add an ellipsis character
            text += " \u2026";
        }

        if (out.length() > 0) {
            float max = maxWidth;

            // text layout and line breaks might change after editing thus this checks if constraints are still fulfilled after 50 ms
            Runnable check = new Runnable() {
                @Override
                public void run() {
                    Layout layout1 = textView.getLayout();
                    if (layout1 != null) {
                        int count = layout1.getLineCount();
                        if (count > maxLineCount) {
                            ellipsize(textView, maxLineCount, max, targetWidth);
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(this, 50);
                    }
                }
            };
            new Handler(Looper.getMainLooper()).postDelayed(check, 50);

            // adding a zero-width whitespace makes android not remove trailing spaces
            text += "\u200B";
            textView.setText(text);
        }

        return out.toString();
    }

    /**
     * Ellipsizes this views messageTextView in a way such that the date is still visible
     */
    private void ellipsizeInternal() {
        int maxLineCount = getMaxLineCount(mMessageTextView);

        // 10dp for triangle, 8 dp padding start and end, 5 dp distance to date
        float targetWidth = mWidth - 31 * dp;
        float maxWidth = targetWidth - mDateTextView.getPaint().measureText(mDate);

        String str = ellipsize(mMessageTextView, maxLineCount, maxWidth, targetWidth);
        if (str.length() > 0) {
            mMessage = mMessageTextView.getText().toString();
            setLinkify(mLinkify);
        }

    }

    /**
     * Ellipsizes this views messageTextView to guarantee that the text fits into the bounds given by the layout.
     * When the view has not yet been drawn the ellipsization will occur on global layout.
     */
    public void ellipsize() {
        int height = mMessageTextView.getHeight();

        if (height != 0) {
            ellipsizeInternal();
        } else {
            mMessageTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = mMessageTextView.getHeight();
                    if (height != 0) {
                        mMessageTextView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        ellipsizeInternal();
                    }
                }
            });
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) ((ViewGroup) parent).setClipChildren(false);
    }

    private void setMessageInternal(@NonNull Message message) {
        mName = message.getName() + (message.getUserName() != null ? " | " + message.getUserName() : "");
        mMessage = message.getMessage();
        mId = String.valueOf(message.getId());

        if (message.getChannel().isEmpty()) {
            mChannel = "(main)";
        } else {
            mChannel = message.getChannel();
        }

        if (mExtended) {
            mDate = message.getDate();
        } else {
            String[] dates = message.getDate().split("(:|\\s)");
            mDate = dates[1] + ":" + dates[2];
        }

        if (mName != null && !mName.matches("[\\s\\n\\r]*")) {
            mNameTextView.setVisibility(VISIBLE);
            mNameTextColor = message.getTransformedColor();
            mNameTextColorSet = true;

            mMessageTextColor = 0;
            mMessageTextColorSet = false;
        } else {
            mNameTextView.setVisibility(GONE);
            mNameTextColor    = mMessageTextColor    = message.getTransformedColor();
            mNameTextColorSet = mMessageTextColorSet = true;
        }

        this.mMessageId = message.getId();
    }

    /**
     * Extracts all required values from the given message.
     *
     * @param message a message
     */
    public void setMessage(Message message) {
        setMessageInternal(message);
        setupTime();
        applyValues();
    }

    /*
     *  Standard Getters and Setters
     */

    public void setMessage(String message) {
        mMessage = message;
        mMessageTextView.setText(mMessage);
        if (mKatex) mMessageMathView.setText(mMessage, mMessageId = -1);
    }

    public String getMessage() {
        return mMessage;
    }

    public void setName(String name) {
        mName = name;
        mNameTextView.setText(mName);
    }

    public String getName() {
        return mName;
    }

    public void setChannel(String channel) {
        mChannel = channel;
        if (mExtended) {
            mChannelTextView.setText(mChannel);
        }
    }

    public String getChannel() {
        return mChannel;
    }

    public void setIdText(String id) {
        mId = id;
        if (mExtended) {
            mIdTextView.setText(mId);
        }
    }

    public String getIdText() {
        return mId;
    }

    public void setDate(String date) {
        mDate = date;
        mDateTextView.setText(mDate);
    }

    public String getDate() {
        return mDate;
    }

    public void setDateBanner(String dateBanner) {        
        mDateBanner = dateBanner;
        if (!mExtended) {
            mDateBannerTextView.setText(mDateBanner);
            mDateBannerTextView.setVisibility(mDateBanner == null ? GONE : VISIBLE);
        }
    }

    public String getDateBanner() {
        return mDateBanner;
    }

    public void setNameTextColor(@ColorInt int color) {
        mNameTextColorSet = true;
        mNameTextColor = color;
        mNameTextView.setTextColor(color);

        if (mColorful) setMessageTextColor(color);
    }

    public void setDateTextColor(@ColorInt int color) {
        mDateTextColorSet = true;
        mDateTextColor = color;
        if (!mExtended) {
            mDateTextView.setTextColor(color);
        }
    }
    
    public void setDataTextColor(@ColorInt int color) {
        mDataTextColorSet = true;
        mDataTextColor = color;

        if (mExtended) {
            mChannelTextView.setTextColor(mDataTextColor);
            mIdTextView.setTextColor(mDataTextColor);
            mDateTextView.setTextColor(mDataTextColor);
        }
    }

    public void setDateBannerTextColor(@ColorInt int color) {
        mDateBannerTextColorSet = true;
        mDateBannerTextColor = color;
        if (!mExtended) {
            mDateBannerTextView.setTextColor(color);
        }
    }

    public void setMessageTextColor(@ColorInt int color) {
        mMessageTextColorSet = true;
        mMessageTextColor = color;
        mMessageTextView.setTextColor(color);
        if (mKatex) mMessageMathView.setTextColor(color);
    }

    public void setNameTextSize(@Dimension(unit = Dimension.SP) float size) {
        mNameTextSizeSet = true;
        mNameTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());
        mNameTextView.setTextSize(size);
    }

    public void setDateTextSize(@Dimension(unit = Dimension.SP) int size) {
        mDateTextSizeSet = true;
        mDateTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());
        mDateTextView.setTextSize(size);
    }

    public void setDataTextSize(@Dimension(unit = Dimension.SP) int size) {
        mDataTextSizeSet = true;
        mDataTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());

        if (mExtended) {
            mChannelTextView.setTextSize(mDataTextSize);
            mIdTextView.setTextSize(mDataTextSize);
            mDateTextView.setTextSize(mDataTextSize);
        }
    }

    public void setDateBannerTextSize(@Dimension(unit = Dimension.SP) float size) {
        mDateBannerTextSizeSet = true;
        mDateBannerTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());

        if (!mExtended) {
            mDateBannerTextView.setTextSize(size);
        }
    }

    public void setMessageTextSize(@Dimension(unit = Dimension.SP) int size) {
        mMessageTextSizeSet = true;
        mMessageTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());
        mMessageTextView.setTextSize(size);
        if (mKatex) mMessageMathView.setTextSize(size);
    }

    public void setNameTextAppearance(@StyleRes int resId) {
        mNameTextAppearanceSet = true;
        mNameTextAppearance = resId;
        mNameTextView.setTextAppearance(resId);
    }

    public void setDateTextAppearance(@StyleRes int resId) {
        mDateTextAppearanceSet = true;
        mDateTextAppearance = resId;
        mDateTextView.setTextAppearance(resId);
    }

    public void setDataTextAppearance(@StyleRes int resId) {
        mDataTextAppearanceSet = true;
        mDataTextAppearance = resId;

        if (mExtended) {
            mChannelTextView.setTextAppearance(mDataTextAppearance);
            mIdTextView.setTextAppearance(mDataTextAppearance);
            mDateTextView.setTextAppearance(mDataTextAppearance);
        }
    }
    
    public void setDateBannerTextAppearance(@StyleRes int resId) {
        mDateBannerTextAppearanceSet = true;
        mDateBannerTextAppearance = resId;
        if (!mExtended) {
            mDateBannerTextView.setTextAppearance(resId);
        }
    }
    
    public void setMessageTextAppearance(@StyleRes int resId) {
        mMessageTextAppearanceSet = true;
        mMessageTextAppearance = resId;
        mMessageTextView.setTextAppearance(resId);
        if (mKatex) mMessageMathView.setTextAppearance(resId);
    }

    /**
     * If a message is colorful, then the message will have the same color as the name.
     */
    public void setColorful(boolean colorful) {
        mColorful = colorful;

        if (colorful) {
            mMessageTextColorNotColorful = mMessageTextColor;
            mMessageTextColorSetNotColorful = mMessageTextColorSet;
            mMessageTextColorsNotColorful = mMessageTextView.getTextColors();

            if (mNameTextColorSet) setMessageTextColor(mNameTextColor);
        } else {
            if (mMessageTextColorSetNotColorful) {
                mMessageTextView.setTextColor(mMessageTextColorNotColorful);
            } else if (mMessageTextColorsNotColorful != null) {
                mMessageTextView.setTextColor(mMessageTextColorsNotColorful);
            }

            mMessageTextColorSetNotColorful = false;
            mMessageTextColorNotColorful = 0;
            mMessageTextColorsNotColorful = null;
        }
    }

    public boolean isColorful() {
        return mColorful;
    }

    /**
     * Enables LaTeX rendering using the {@code KaTeX} library. Cannot be used on a extended message.
     */
    public void setKatex(boolean katex) {
        if (katex && mExtended) throw new IllegalStateException("Katex is not supported in extended message views!");

        if (katex == mKatex) return;

        this.mKatex = katex;

        if (katex) {
            inflateMathView();

            mMessageMathView.setVisibility(VISIBLE);
            mMessageTextView.setVisibility(GONE);

            ViewGroup.LayoutParams layoutParams = mMessageContent.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mMessageContent.setLayoutParams(layoutParams);

            layoutParams = mMessageLayout.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mMessageLayout.setLayoutParams(layoutParams);
        } else {
            if (mMessageMathView != null) mMessageMathView.setVisibility(GONE);
            mMessageTextView.setVisibility(VISIBLE);

            ViewGroup.LayoutParams layoutParams = mMessageContent.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mMessageContent.setLayoutParams(layoutParams);

            layoutParams = mMessageLayout.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mMessageLayout.setLayoutParams(layoutParams);
        }
    }

    public boolean isKatex() {
        return mKatex;
    }

    /**
     * Extended {@code MessageView}s show more data, like the channel, the extact data and the message id.
     * Cannot be used together with KaTeX rendering.
     *
     * <p>
     *     Since extended messages use another layout, changing this value requires a re-inflating
     *     of the layout. Therefore using {@link #MessageView(Context, Boolean)} with {@code extended}
     *     set to true is preferred.
     * </p>
     */
    public void setExtended(boolean extended) {
        if (extended && mKatex) throw new IllegalStateException("Extended messages are not allowed when KaTeX rendering is used!");

        if (mExtended != extended) {
            this.mExtended = extended;

            reload();
        }
    }

    public boolean isExtended() {
        return mExtended;
    }

    public void setBackgroundColor(@ColorInt int color) {
        this.setBackgroundColor(ColorStateList.valueOf(color));
    }

    public void setBackgroundColor(ColorStateList colorStateList) {
        mBackgroundColor = colorStateList;
        mBackgroundColorSet = true;

        Drawable mainBg = mMessageLayout.getBackground();
        mMessageLayout.setBackgroundTintList(colorStateList);
        if (mColorPressedHighlightSet && mainBg instanceof RippleDrawable) {
            ((RippleDrawable) mainBg).setColor(mColorPressedHighlight);
        }

        if (!mExtended) {
            Drawable triangleBg = mTriangleView.getBackground();
            mTriangleView.setBackgroundTintList(colorStateList);
            if (mColorPressedHighlightSet && triangleBg instanceof RippleDrawable) {
                ((RippleDrawable) triangleBg).setColor(mColorPressedHighlight);
            }
        }
    }

    private void inflateMathView() {
        if (mExtended) return;
        if (mMessageMathView != null) return;

        mMessageMathView = (MathView) mMessageMathViewStub.inflate();

        applyValues();
    }

    /*
     *  Stuff important for background ripple
     */
    private float x;
    private float y;
    private int mMainBgOffsetX;
    private int mMainBgOffsetY;
    private int mTriangleBgOffsetX;
    private int mTriangleBgOffsetY;

    @Override
    public void setPressed(boolean pressed) {
        Drawable mainBg = mMessageLayout.getBackground();
        mainBg.setHotspot(x - mMainBgOffsetX, y - mMainBgOffsetY);

        if (!mExtended) {
            Drawable triangleBg = mTriangleView.getBackground();
            triangleBg.setHotspot(x - mTriangleBgOffsetX, y - mTriangleBgOffsetY);
        }

        super.setPressed(pressed);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            x = ev.getX();
            y = ev.getY();
        }

        return false;
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mMainBgOffsetX = mMessageLayout.getLeft();
        mMainBgOffsetY = mMessageLayout.getTop();

        if (!mExtended) {
            mTriangleBgOffsetX = mTriangleView.getLeft();
            mTriangleBgOffsetY = mTriangleView.getTop();
        }

        int width = getWidth();
        int height = getHeight();

        Drawable mainBg = mMessageLayout.getBackground();
        mainBg.setHotspotBounds(- mMainBgOffsetX, - mMainBgOffsetY, width - mMainBgOffsetX, height - mMainBgOffsetY);

        if (!mExtended) {
            Drawable triangleBg = mTriangleView.getBackground();
            triangleBg.setHotspotBounds(-mTriangleBgOffsetX, -mTriangleBgOffsetY, width - mTriangleBgOffsetX, height - mTriangleBgOffsetY);
        }
    }

    /*
     *  Stuff making text click through apart from links
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setLinkify(boolean linkify) {
        mLinkify = linkify;
        if (linkify) {
            if (Linkify.addLinks(mMessageTextView, Linkify.WEB_URLS)) {
                mMessageTextView.setOnTouchListener(linkifiedMessageOnTouchListener);
            } else {
                mMessageTextView.setOnTouchListener(null);
            }
        } else {
            mMessageTextView.setOnTouchListener(null);
        }

        mMessageTextView.setMovementMethod(null);
    }
    public boolean isLinkify() {
        return mLinkify;
    }

    private static final LinkifiedMessageOnTouchListener linkifiedMessageOnTouchListener = new LinkifiedMessageOnTouchListener();
    private static class LinkifiedMessageOnTouchListener implements OnTouchListener {
        private static final LinkMovementMethod2 movementMethod = new LinkMovementMethod2();

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!(v instanceof TextView)) return false;
            TextView textView = (TextView) v;
            if (!(textView.getText() instanceof Spannable)) return false;

            return movementMethod.onTouchEvent(textView, (Spannable) textView.getText(), event);
        }
    }
    private static class LinkMovementMethod2 extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

                if (links.length != 0) {
                    ClickableSpan link = links[0];
                    if (action == MotionEvent.ACTION_UP) {
                        link.onClick(widget);
                    } else {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link),
                                buffer.getSpanEnd(link));
                    }

                    return true;
                } else {
                    Selection.removeSelection(buffer);
                }
            }

            return false;
        }
    }
}

