package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.*;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.util.ViewUtils;

/**
 * A material styled one or two lined list item with an optional icon.
 * @see <a href="https://material.io/components/lists">https://material.io/components/lists</a>
 */
public class MaterialListItem extends ViewGroup {
    @IntDef({ICON_ROUND, ICON_SQUARE, ICON_WIDE})
    private @interface IconMode {}

    /**
     * A round 40dp x 40dp icon.
     */
    public static final int ICON_ROUND = 0;
    /**
     * A square icon with a size of 56dp x 56dp when no subtitle is present or 40dp x 40dp otherwise.
     */
    public static final int ICON_SQUARE = 1;
    /**
     * A rectangular icon with a size of 100dp x 56dp and no start padding.
     */
    public static final int ICON_WIDE = 2;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Drawable mIcon;
    private CharSequence mIconText;

    private @IconMode int mIconMode;

    private final TextView mTitleTextView;
    private final TextView mSubtitleTextView;
    private final ImageView mIconView;
    private final TextView mIconTextView;

    public MaterialListItem(@NonNull Context context) {
        this(context, null);
    }

    public MaterialListItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialListItem(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_App_MaterialListItem);
    }

    public MaterialListItem(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.mTitleTextView = new TextView(context);
        this.mTitleTextView.setSingleLine();
        this.mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        this.mTitleTextView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

        this.mSubtitleTextView = new TextView(context);
        this.mSubtitleTextView.setSingleLine();
        this.mSubtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        this.mSubtitleTextView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

        this.mIconView = new ImageView(context);
        this.mIconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        this.mIconTextView = new TextView(context);
        this.mIconTextView.setSingleLine();
        this.mIconTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        this.mIconTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        addView(mTitleTextView);
        addView(mSubtitleTextView);
        addView(mIconView);
        addView(mIconTextView);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        var array = context.obtainStyledAttributes(attrs, R.styleable.MaterialListItem, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.MaterialListItem, attrs, array, defStyleAttr, defStyleRes);
        }

        if (array.hasValue(R.styleable.MaterialListItem_title)) {
            setTitle(array.getText(R.styleable.MaterialListItem_title));
        }

        if (array.hasValue(R.styleable.MaterialListItem_subtitle)) {
            setSubtitle(array.getText(R.styleable.MaterialListItem_subtitle));
        }

        if (array.hasValue(R.styleable.MaterialListItem_iconText)) {
            setIconText(array.getText(R.styleable.MaterialListItem_iconText));
        }

        if (array.hasValue(R.styleable.MaterialListItem_icon)) {
            setIcon(array.getDrawable(R.styleable.MaterialListItem_icon));
        }

        if (array.hasValue(R.styleable.MaterialListItem_titleTextAppearance)) {
            setTitleTextAppearance(array.getResourceId(R.styleable.MaterialListItem_titleTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.MaterialListItem_subtitleTextAppearance)) {
            setSubtitleTextAppearance(array.getResourceId(R.styleable.MaterialListItem_subtitleTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.MaterialListItem_iconTextAppearance)) {
            setIconTextAppearance(array.getResourceId(R.styleable.MaterialListItem_iconTextAppearance, 0));
        }

        if (array.hasValue(R.styleable.MaterialListItem_iconMode)) {
            setIconMode(array.getInteger(R.styleable.MaterialListItem_iconMode, ICON_ROUND));
        }

        if (array.hasValue(R.styleable.MaterialListItem_iconTint)) {
            setIconTint(array.getColorStateList(R.styleable.MaterialListItem_iconTint));
        }

        array.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        var width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        int heightDp;
        if (mIcon == null) {
            heightDp = mSubtitle == null ? 48 : 64;
        } else if (mIconMode == ICON_ROUND) {
            heightDp = mSubtitle == null ? 56 : 72;
        } else if (mIconMode == ICON_SQUARE || mIconMode == ICON_WIDE) {
            heightDp = 72;
        } else {
            throw new IllegalArgumentException("Unknown icon mode " + mIconMode + ".");
        }

        var widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            var targetWidth = MeasureSpec.getSize(widthMeasureSpec);
            var paddingDp = 32;
            if (mIcon != null) {
                paddingDp += 16;
                if (mIconMode == ICON_ROUND) {
                    paddingDp += 40;
                } else if (mIconMode == ICON_SQUARE) {
                    paddingDp += (mSubtitle == null ? 56 : 40);
                } else if (mIconMode == ICON_WIDE) {
                    paddingDp += 84;
                }
            }
            targetWidth -= ViewUtils.dpToPx(this, paddingDp);

            var childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.AT_MOST);
            var childHeightMeasureSpec = MeasureSpec.UNSPECIFIED;
            if (mTitleTextView.getVisibility() != View.GONE) {
                mTitleTextView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
            if (mSubtitleTextView.getVisibility() != View.GONE) {
                mSubtitleTextView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        if (mIcon != null && mIconText != null) {
            var targetWidthDp = mIconMode == ICON_ROUND ? 40
                    : mIconMode == ICON_SQUARE ? (mSubtitle == null ? 56 : 40)
                    : mIconMode == ICON_WIDE ? 100
                    : 0;
            var targetHeightDp = (mIconMode == ICON_WIDE || mIconMode == ICON_SQUARE && mSubtitle == null) ? 56 : 40;

            mIconTextView.measure(
                    MeasureSpec.makeMeasureSpec((int) ViewUtils.dpToPx(this, targetWidthDp), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec((int) ViewUtils.dpToPx(this, targetHeightDp), MeasureSpec.AT_MOST)
            );
        }

        var height = (int) ViewUtils.dpToPx(this, heightDp);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        var width = r - l;
        var height = b - t;

        var dp4 = ViewUtils.dpToPx(this, 4);

        if (mIcon == null) {
            if (mSubtitle == null) {
                layoutCenter((int) (4 * dp4), (int) (width - 4 * dp4), width, height, mTitleTextView);
            } else {
                layoutBaseline((int) (4 * dp4), (int) (width - 4 * dp4), (int) (7 * dp4), width, mTitleTextView);
                layoutBaseline((int) (4 * dp4), (int) (width - 4 * dp4), (int) (12 * dp4), width, mSubtitleTextView);
            }
        } else if (mIconMode == ICON_ROUND) {
            if (mSubtitle == null) {
                layoutIcon((int) (4 * dp4), (int) (2 * dp4), (int) (14 * dp4), (int) (12 * dp4), width);
                layoutCenter((int) (18 * dp4), (int) (width - 4 * dp4), width, height, mTitleTextView);
            } else {
                layoutIcon((int) (4 * dp4), (int) (4 * dp4), (int) (14 * dp4), (int) (14 * dp4), width);
                layoutBaseline((int) (18 * dp4), (int) (width - 4 * dp4), (int) (8 * dp4), width, mTitleTextView);
                layoutBaseline((int) (18 * dp4), (int) (width - 4 * dp4), (int) (13 * dp4), width, mSubtitleTextView);
            }
        } else if (mIconMode == ICON_SQUARE) {
            if (mSubtitle == null) {
                layoutIcon((int) (4 * dp4), (int) (2 * dp4), (int) (18 * dp4), (int) (16 * dp4), width);
                layoutCenter((int) (22 * dp4), (int) (width - 4 * dp4), width, height, mTitleTextView);
            } else {
                layoutIcon((int) (4 * dp4), (int) (4 * dp4), (int) (14 * dp4), (int) (14 * dp4), width);
                layoutBaseline((int) (18 * dp4), (int) (width - 4 * dp4), (int) (8 * dp4), width, mTitleTextView);
                layoutBaseline((int) (18 * dp4), (int) (width - 4 * dp4), (int) (13 * dp4), width, mSubtitleTextView);
            }
        } else if (mIconMode == ICON_WIDE) {
            if (mSubtitle == null) {
                layoutIcon(0, (int) (2 * dp4), (int) (25 * dp4), (int) (16 * dp4), width);
                layoutCenter((int) (29 * dp4), (int) (width - 4 * dp4), width, height, mTitleTextView);
            } else {
                layoutIcon(0, (int) (2 * dp4), (int) (25 * dp4), (int) (16 * dp4), width);
                layoutBaseline((int) (29 * dp4), (int) (width - 4 * dp4), (int) (8 * dp4), width, mTitleTextView);
                layoutBaseline((int) (29 * dp4), (int) (width - 4 * dp4), (int) (13 * dp4), width, mSubtitleTextView);
            }
        } else {
            throw new IllegalArgumentException("Unknown icon mode " + mIconMode + ".");
        }
    }

    private void layoutBaseline(int l, int r, int baseline, int width, TextView textView) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            var tmpL = width - r;
            var tmpR = width - l;
            l = tmpL;
            r = tmpR;
        }
        var textBaseline = textView.getBaseline();
        var textHeight = textView.getMeasuredHeight();
        textView.layout(l, baseline - textBaseline, r, baseline - textBaseline + textHeight);
    }

    private void layoutCenter(int l, int r, int width, int height, TextView textView) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            var tmpL = width - r;
            var tmpR = width - l;
            l = tmpL;
            r = tmpR;
        }
        var textHeight = textView.getMeasuredHeight();
        textView.layout(l, (height - textHeight) / 2, r, (height + textHeight) / 2);
    }

    private void layoutIcon(int l, int t, int r, int b, int width) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            var tmpL = width - r;
            var tmpR = width - l;
            l = tmpL;
            r = tmpR;
        }
        mIconView.layout(l, t, r, b);
        if (mIconText != null) {
            var iconWidth = r - l;
            var iconHeight = b - t;
            var textWidth = mIconTextView.getMeasuredWidth();
            var textHeight = mIconTextView.getMeasuredHeight();
            mIconTextView.layout(l + (iconWidth - textWidth) / 2, t + (iconHeight - textHeight) / 2, l + (iconWidth + textWidth) / 2, t + (iconHeight + textHeight) / 2);
        }
    }

    //<editor-fold desc="Getter/Setter" defaultstate="collapsed">
    public CharSequence getIconText() {
        return mIconText;
    }

    public void setIconText(CharSequence iconText) {
        this.mIconText = iconText;
        this.mIconTextView.setText(iconText);
        this.mIconTextView.setVisibility(iconText != null ? View.VISIBLE : View.GONE);
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setTitle(CharSequence title) {
        this.mTitle = title;
        this.mTitleTextView.setText(title);
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(CharSequence subtitle) {
        this.mSubtitle = subtitle;
        this.mSubtitleTextView.setText(subtitle);
        this.mSubtitleTextView.setVisibility(subtitle != null ? View.VISIBLE : View.GONE);
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
        this.mIconView.setImageDrawable(icon);
        this.mIconView.setVisibility(icon != null ? View.VISIBLE : View.GONE);
    }

    public void setIcon(@DrawableRes int icon) {
        setIcon(getContext().getDrawable(icon));
    }

    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        this.mTitleTextView.setTextAppearance(textAppearance);
    }

    public void setSubtitleTextAppearance(@StyleRes int textAppearance) {
        this.mSubtitleTextView.setTextAppearance(textAppearance);
    }

    public void setIconTextAppearance(@StyleRes int textAppearance) {
        this.mIconTextView.setTextAppearance(textAppearance);
    }

    public void setIconMode(@IconMode int iconMode) {
        this.mIconMode = iconMode;
        invalidate();
    }

    public int getIconMode() {
        return mIconMode;
    }

    public void setIconTint(ColorStateList tint) {
        this.mIconView.setImageTintList(tint);
    }

    public void setIconTint(@ColorInt int tint) {
        setIconTint(ColorStateList.valueOf(tint));
    }
    //</editor-fold>
}
