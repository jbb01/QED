package eu.jonahbauer.qed.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.util.LinkifyCompat;

import com.google.android.material.shape.CornerTreatment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.RoundedCornerTreatment;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapePath;
import com.google.android.material.theme.overlay.MaterialThemeOverlay;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.util.ViewUtils;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SimpleMessageView extends ConstraintLayout implements MessageView {
    private CharSequence mName;
    private CharSequence mMessage;
    private CharSequence mTimestamp;

    private final TextView mNameTextView;
    private final TextView mMessageTextView;
    private final TextView mTimestampTextView;

    private final LinearLayout mMessageContent;

    private View mTip;

    private MathView mMessageMathView;
    private final ViewStub mMessageMathViewStub;
    private WeakReference<LinearLayout> mMathPreload;

    private boolean mLinkify;
    private boolean mColorful;
    private boolean mKatex;

    private @StyleRes Integer mMessageTextAppearance = null;

    //<editor-fold desc="Constructors" defaultstate="collapsed">
    public SimpleMessageView(@NonNull Context context) {
        this(context, null);
    }

    public SimpleMessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.messageStyle);
    }

    public SimpleMessageView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_App_Message);
    }

    public SimpleMessageView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(
                MaterialThemeOverlay.wrap(context, attrs, defStyleAttr, defStyleRes),
                attrs, defStyleAttr, defStyleRes
        );
        context = getContext();

        var array = context.obtainStyledAttributes(attrs, R.styleable.SimpleMessageView, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.SimpleMessageView, attrs, array, defStyleAttr, defStyleRes);
        }

        // config
        mLinkify = array.getBoolean(R.styleable.SimpleMessageView_linkify, false);
        mKatex = array.getBoolean(R.styleable.SimpleMessageView_katex, false);
        mColorful = array.getBoolean(R.styleable.SimpleMessageView_colorful, false);

        // inflate views
        LayoutInflater.from(this.getContext()).inflate(R.layout.message_view, this, true);

        mNameTextView = findViewById(R.id.message_name);
        mMessageTextView = findViewById(R.id.message_message);
        mMessageMathViewStub = findViewById(R.id.message_message_math_stub);
        mTimestampTextView = findViewById(R.id.message_timestamp);
        mMessageContent = findViewById(R.id.message_content);

        // values
        if (array.hasValue(R.styleable.SimpleMessageView_name)) {
            setName(array.getText(R.styleable.SimpleMessageView_name));
        }

        if (array.hasValue(R.styleable.SimpleMessageView_message)) {
            setMessage(array.getText(R.styleable.SimpleMessageView_message));
        }

        if (array.hasValue(R.styleable.SimpleMessageView_timestamp)) {
            setTimestamp(array.getText(R.styleable.SimpleMessageView_timestamp));
        }

        // style
        if (array.hasValue(R.styleable.SimpleMessageView_nameTextAppearance)) {
            setNameTextAppearance(array.getResourceId(R.styleable.SimpleMessageView_nameTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.SimpleMessageView_messageTextAppearance)) {
            setMessageTextAppearance(array.getResourceId(R.styleable.SimpleMessageView_messageTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.SimpleMessageView_dataTextAppearance)) {
            setDataTextAppearance(array.getResourceId(R.styleable.SimpleMessageView_dataTextAppearance, 0));
        }


        // below Q native shadows support only convex outlines
        // therefore we have to add an extra view to achieve
        // native shadows for the tip of the message box
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            var size = (int) ViewUtils.dpToPx(this, 10);
            var tip = new View(context, attrs, defStyleAttr, defStyleRes);
            var lp = new LayoutParams(size, size);
            tip.setLayoutParams(lp);
            tip.setBackgroundResource(R.drawable.message_triangle);
            tip.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    var path = new Path();
                    path.moveTo(0, 0);
                    path.lineTo(view.getWidth(), view.getHeight());
                    path.lineTo(view.getWidth(), 0);

                    outline.setConvexPath(path);
                }
            });

            // wrap tip and clip on the right side so the shadow doesn't interfere with the background
            var tipWrapper = new LinearLayout(context);
            tipWrapper.setClipBounds(new Rect(
                    Integer.MIN_VALUE, Integer.MIN_VALUE, size, Integer.MAX_VALUE
            ));
            tipWrapper.addView(tip);

            this.mTip = tipWrapper;

            addView(tipWrapper);

            setClipToPadding(false);
            setClipChildren(false);
        }

        setBackground(createBackground(this));
    }
    //</editor-fold>

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // adjust sizing to prevent overlap of timestamp and message
        View lastChild;
        if (mKatex && mMessageMathView != null) {
            lastChild = mMessageMathView.getChildAt(mMessageMathView.getChildCount() - 1);
        } else {
            lastChild = mMessageTextView;
        }

        if (lastChild instanceof TextView && ((TextView) lastChild).getLayout() != null) {
            var layout = ((TextView) lastChild).getLayout();
            var lineCount = layout.getLineCount();
            var lastLineWidth = layout.getLineMax(lineCount - 1);
            var dateWidth = mTimestampTextView.getMeasuredWidth() + ViewUtils.dpToPx(this, 4);
            var padding = getPaddingLeft() + getPaddingRight();

            var width = getMeasuredWidth() - padding;
            var fits = (lastLineWidth + dateWidth <= width);

            var maxWidth = MeasureSpec.getSize(widthMeasureSpec) - padding;
            var fitsMax = (lastLineWidth + dateWidth <= maxWidth);

            if (!fits) {
                if (fitsMax && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                    setMeasuredDimension((int) (dateWidth + lastLineWidth + padding), getMeasuredHeight());
                } else {
                    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + mTimestampTextView.getMeasuredHeight());
                }
            }
        } else {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + mTimestampTextView.getMeasuredHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // fixed position for compat tip
        if (mTip != null) {
            mTip.layout(0, 0, mTip.getMeasuredWidth(), mTip.getMeasuredHeight());
        }

        // timestamp goes into the bottom end corner
        var r = getMeasuredWidth() - getPaddingRight();
        var b = getMeasuredHeight() - getPaddingBottom();
        mTimestampTextView.layout(
                 r - mTimestampTextView.getMeasuredWidth(),
                 b - mTimestampTextView.getMeasuredHeight(),
                 r,
                 b
        );
    }

    //<editor-fold desc="Getters/Setters" defaultstate="collapsed">

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
        if (mKatex) {
            this.mMessageMathView.setText(message.toString());
        }
    }

    @Override
    public CharSequence getMessage() {
        return mMessage;
    }

    @Override
    public void setTimestamp(Instant instant) {
        setTimestamp(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
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

    @Override
    public void setMessage(Message message) {
        var color = ColorStateList.valueOf(message.getColor(getContext()));
        setNameColor(color);
        if (mColorful) setMessageColor(color);

        setName(MessageView.formatName(getContext(), message.getName(), message.getUserName()));
        setMessage(message.getMessage());
        setTimestamp(message.getDate());
    }

    @Override
    public void setNameTextAppearance(@StyleRes int textAppearance) {
        mNameTextView.setTextAppearance(textAppearance);
    }

    @Override
    public void setMessageTextAppearance(@StyleRes int textAppearance) {
        mMessageTextAppearance = textAppearance;
        mMessageTextView.setTextAppearance(textAppearance);
        if (mMessageMathView != null) mMessageMathView.setTextAppearance(textAppearance);
    }

    @Override
    public void setDataTextAppearance(@StyleRes int textAppearance) {
        mTimestampTextView.setTextAppearance(textAppearance);
    }

    @Override
    public void setNameColor(ColorStateList color) {
        mNameTextView.setTextColor(color);
    }

    @Override
    public void setMessageColor(ColorStateList color) {
        mMessageTextView.setTextColor(color);
        if (mMessageMathView != null) mMessageMathView.setTextColor(color.getDefaultColor());
    }

    @Override
    public void setDataColor(ColorStateList color) {
        mTimestampTextView.setTextColor(color);
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
                mMessageTextView.setMovementMethod(BetterLinkMovementMethod.getInstance());
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

    public void setKatex(boolean katex) {
        if (katex == this.mKatex) return;

        this.mKatex = katex;

        if (katex) {
            inflateMathView();

            mMessageMathView.setVisibility(VISIBLE);
            mMessageTextView.setVisibility(GONE);

            ViewGroup.LayoutParams layoutParams = mMessageContent.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mMessageContent.setLayoutParams(layoutParams);
        } else {
            if (mMessageMathView != null) mMessageMathView.setVisibility(GONE);
            mMessageTextView.setVisibility(VISIBLE);

            ViewGroup.LayoutParams layoutParams = mMessageContent.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mMessageContent.setLayoutParams(layoutParams);
        }
    }

    public boolean isKatex() {
        return mKatex;
    }

    public void setMathPreload(LinearLayout mathPreload) {
        this.mMathPreload = new WeakReference<>(mathPreload);
        if (this.mMessageMathView != null) {
            this.mMessageMathView.setMathPreload(mathPreload);
        }
    }
    //</editor-fold>

    private void inflateMathView() {
        if (mMessageMathView != null) return;

        mMessageMathView = (MathView) mMessageMathViewStub.inflate();
        if (mMathPreload != null) mMessageMathView.setMathPreload(mMathPreload.get());

        if (mMessage != null) mMessageMathView.setText(mMessage.toString());
        if (mMessageTextAppearance != null) mMessageMathView.setTextAppearance(mMessageTextAppearance);
    }

    private static Drawable createBackground(View view) {
        var metrics = view.getResources().getDisplayMetrics();

        var shapeBuilder = ShapeAppearanceModel
                .builder()
                .setAllCorners(new RoundedCornerTreatment())
                .setAllCornerSizes(ViewUtils.dpToPx(metrics, 10))
                .setBottomLeftCorner(new CornerTreatment() {
                    @Override
                    public void getCornerPath(@NonNull ShapePath shapePath, float angle, float interpolation, float radius) {
                        // translate corner by size
                        var size = radius * interpolation;
                        shapePath.reset(0, 2 * size, 180, 180 - angle);
                        shapePath.addArc(0, size, 2 * size, 3 * size, 180, angle);
                    }
                });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // convex background on
            shapeBuilder.setTopLeftCorner(new CornerTreatment() {
                @Override
                public void getCornerPath(@NonNull ShapePath shapePath, float angle, float interpolation, float radius) {
                    // translate corner by size
                    var size = radius * interpolation;
                    shapePath.reset(size, 0, 180, 180 - angle);
                }
            });
        } else {
            shapeBuilder.setTopLeftCorner(new CornerTreatment() {
                    @Override
                    public void getCornerPath(@NonNull ShapePath shapePath, float angle, float interpolation, float radius) {
                        var size = radius * interpolation;
                        var tipSize = .25f * size;
                        shapePath.reset(size, size);
                        shapePath.lineTo(tipSize, tipSize);
                        shapePath.addArc(
                                0.82842714F * tipSize, // (2 * sqrt(2) - 2) * tipSize
                                0,
                                2 * tipSize,
                                1.1715729F * tipSize, // 2 * (2 - sqrt(2)) * tipSize
                                135,
                                135
                        );
                    }
                });
        }

        var shape = shapeBuilder.build();

        var drawable = new MaterialShapeDrawable(shape);
        drawable.setFillColor(ColorStateList.valueOf(Color.WHITE));
        drawable.setPadding(
                (int) ViewUtils.dpToPx(metrics, 18),
                (int) ViewUtils.dpToPx(metrics, 5),
                (int) ViewUtils.dpToPx(metrics, 8),
                (int) ViewUtils.dpToPx(metrics, 5)
        );
        return drawable;
    }

    static class BetterLinkMovementMethod extends LinkMovementMethod {
        private static BetterLinkMovementMethod INSTANCE;

        public static BetterLinkMovementMethod getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new BetterLinkMovementMethod();
            }
            return INSTANCE;
        }

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

