package eu.jonahbauer.qed.ui.views;

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

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.ViewUtils;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;

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

    private boolean mPaddingLeftOverride;
    private boolean mPaddingTopOverride;
    private boolean mPaddingRightOverride;
    private boolean mPaddingBottomOverride;

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
        this.mIconView.setScaleType(ImageView.ScaleType.CENTER_CROP);

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

        // analyze padding overrides
        if (array.hasValue(R.styleable.MaterialListItem_android_padding)) {
            mPaddingLeftOverride = true;
            mPaddingRightOverride = true;
            mPaddingTopOverride = true;
            mPaddingBottomOverride = true;
        } else {
            if (array.hasValue(R.styleable.MaterialListItem_android_paddingHorizontal)) {
                mPaddingLeftOverride = true;
                mPaddingRightOverride = true;
            } else {
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingLeft)) {
                    mPaddingLeftOverride = true;
                }
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingRight)) {
                    mPaddingRightOverride = true;
                }
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingStart)) {
                    if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                        mPaddingRightOverride = true;
                    } else {
                        mPaddingLeftOverride = true;
                    }
                }
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingEnd)) {
                    if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                        mPaddingLeftOverride = true;
                    } else {
                        mPaddingRightOverride = true;
                    }
                }
            }

            if (array.hasValue(R.styleable.MaterialListItem_android_paddingVertical)) {
                mPaddingTopOverride = true;
                mPaddingBottomOverride = true;
            } else {
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingTop)) {
                    mPaddingTopOverride = true;
                }
                if (array.hasValue(R.styleable.MaterialListItem_android_paddingBottom)) {
                    mPaddingBottomOverride = true;
                }
            }
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

        updatePadding(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        var width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        var widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            var targetWidth = MeasureSpec.getSize(widthMeasureSpec);
            targetWidth -= getPaddingLeft() + getPaddingRight();
            if (mIcon != null) {
                targetWidth -= getIconWidth() + (int) ViewUtils.dpToPx(this, 16);
            }

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
            mIconTextView.measure(
                    MeasureSpec.makeMeasureSpec(getIconWidth(), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(getIconHeight(), MeasureSpec.AT_MOST)
            );
        }

        int height = getPaddingTop() + getPaddingBottom() + getContentHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        var width = r - l;
        var height = b - t;

        var ps = getPaddingStart();
        var pe = getPaddingEnd();
        var pt = getPaddingTop();
        var iconWidth = getIconWidth();
        var iconHeight = getIconHeight();
        var iconPadding = (int) ViewUtils.dpToPx(this, 16);

        var dp4 = ViewUtils.dpToPx(this, 4);

        if (mIcon == null) {
            if (mSubtitle == null) {
                layoutCenter(ps, width - pe, width, height, mTitleTextView);
            } else {
                layoutBaseline(ps, width - pe, height / 2 - (int) dp4, width, mTitleTextView);
                layoutBaseline(ps, width - pe, height / 2 + (int) (4 * dp4), width, mSubtitleTextView);
            }
        } else {
            layoutIcon(ps, pt, ps + iconWidth, pt + iconHeight, width);
            if (mSubtitle == null) {
                layoutCenter(ps + iconWidth + iconPadding, width - pe, width, height, mTitleTextView);
            } else {
                layoutBaseline(ps + iconWidth + iconPadding, width - pe, height / 2 - (int) dp4, width, mTitleTextView);
                layoutBaseline(ps + iconWidth + iconPadding, width - pe, height / 2 + (int) (4 * dp4), width, mSubtitleTextView);
            }
        }
    }

    private void layoutBaseline(int start, int end, int baseline, int width, TextView textView) {
        int l, r;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            l = width - end;
            r = width - start;
        } else {
            l = start;
            r = end;
        }
        var textBaseline = textView.getBaseline();
        var textHeight = textView.getMeasuredHeight();
        textView.layout(l, baseline - textBaseline, r, baseline - textBaseline + textHeight);
    }

    private void layoutCenter(int start, int end, int width, int height, TextView textView) {
        int l, r;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            l = width - end;
            r = width - start;
        } else {
            l = start;
            r = end;
        }
        var textHeight = textView.getMeasuredHeight();
        textView.layout(l, (height - textHeight) / 2, r, (height + textHeight) / 2);
    }

    private void layoutIcon(int start, int t, int end, int b, int width) {
        int l, r;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            l = width - end;
            r = width - start;
        } else {
            l = start;
            r = end;
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

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeftOverride = true;
        mPaddingTopOverride = true;
        mPaddingRightOverride = true;
        mPaddingBottomOverride = true;
        super.setPadding(left, top, right, bottom);
    }

    /*
        lines | icon   | start | end | vertical
        ---------------------------------------
        1     | none   | 16    | 16  | 0
        1     | round  | 16    | 16  | 8
        1     | square | 16    | 16  | 8
        1     | wide   | 0     | 16  | 8
        ---------------------------------------
        2     | none   | 16    | 16  | 0
        2     | round  | 16    | 16  | 16
        2     | square | 16    | 16  | 16
        2     | wide   | 0     | 16  | 8
     */
    private void updatePadding(boolean force) {
        int left, right, top, bottom;

        var dp16 = (int) ViewUtils.dpToPx(this, 16);
        var dp8 = (int) ViewUtils.dpToPx(this, 8);

        // horizontal padding
        if (mIcon == null || mIconMode != ICON_WIDE) {
            left = right = dp16;
        } else if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            left = 0;
            right = dp16;
        } else {
            left = dp16;
            right = 0;
        }

        if (mIcon == null) {
            top = bottom = 0;
        } else if (mIconMode == ICON_WIDE || mSubtitle == null) {
            top = bottom = dp8;
        } else {
            top = bottom = dp16;
        }

        if (!force) {
            if (mPaddingLeftOverride) left = getPaddingLeft();
            if (mPaddingRightOverride) right = getPaddingRight();
            if (mPaddingTopOverride) top = getPaddingTop();
            if (mPaddingBottomOverride) bottom = getPaddingBottom();
        } else {
            mPaddingLeftOverride = false;
            mPaddingTopOverride = false;
            mPaddingRightOverride = false;
            mPaddingBottomOverride = false;
        }

        super.setPadding(left, top, right, bottom);
    }

    private @Px int getIconHeight() {
        if (mIconMode == ICON_WIDE || mIconMode == ICON_SQUARE && mSubtitle == null) {
            return (int) ViewUtils.dpToPx(this, 56);
        } else {
            return (int) ViewUtils.dpToPx(this, 40);
        }
    }

    private @Px int getIconWidth() {
        if (mIconMode == ICON_WIDE) {
            return (int) ViewUtils.dpToPx(this, 100);
        } else if (mIconMode == ICON_SQUARE && mSubtitle == null) {
            return (int) ViewUtils.dpToPx(this, 56);
        } else {
            return (int) ViewUtils.dpToPx(this, 40);
        }
    }

    private @Px int getContentHeight() {
        if (mIcon != null) {
            return getIconHeight();
        } else if (mSubtitle == null) {
            return (int) ViewUtils.dpToPx(this, 40);
        } else {
            return (int) ViewUtils.dpToPx(this, 56);
        }
    }

    public void useDefaultPadding() {
        updatePadding(true);
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
        var nullChange = mSubtitle == null ^ subtitle == null;
        this.mSubtitle = subtitle;
        this.mSubtitleTextView.setText(subtitle);
        this.mSubtitleTextView.setVisibility(subtitle != null ? View.VISIBLE : View.GONE);
        if (nullChange) updatePadding(false);
        invalidate();
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        var nullChange = mIcon == null ^ icon == null;
        this.mIcon = icon;
        this.mIconView.setImageDrawable(icon);
        this.mIconView.setVisibility(icon != null ? View.VISIBLE : View.GONE);
        if (nullChange) updatePadding(false);
        invalidate();
    }

    public void setIcon(@DrawableRes int icon) {
        setIcon(icon == 0 ? null : AppCompatResources.getDrawable(getContext(), icon));
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
        if (mIconMode != iconMode) {
            this.mIconMode = iconMode;
            updatePadding(false);
            invalidate();
        }
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
