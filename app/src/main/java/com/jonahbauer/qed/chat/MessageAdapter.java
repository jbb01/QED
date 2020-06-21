package com.jonahbauer.qed.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.MathView;
import com.jonahbauer.qed.layoutStuff.MessageView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MessageAdapter extends ArrayAdapter<Message> {
    private final Context mContext;
    private final List<Message> mMessageList;

    private final HashSet<Integer> mDateBannerPositions;

    private final int mDp3;
    private boolean mLinkify;
    private boolean mColorful;
    private boolean mKatex;
    private final boolean mExtended;

    private boolean mKatexSet;
    private boolean mLinkifySet;

    private final LinearLayout mathPreload;

    public MessageAdapter(Context context, @NonNull List<Message> messageList, @Nullable LinearLayout mathPreload) {
        this(context, messageList, mathPreload, null, null, false);
    }

    public MessageAdapter(Context context, @NonNull List<Message> messageList, @Nullable LinearLayout mathPreload, @Nullable Boolean katex, @Nullable Boolean linkify, boolean extended) {
        super(context, 0, messageList);
        this.mContext = context;
        this.mMessageList = messageList;
        this.mDateBannerPositions = new HashSet<>();
        this.mathPreload = mathPreload;

        this.mExtended = extended;

        this.mLinkifySet = linkify != null;
        if (this.mLinkifySet) this.mLinkify = linkify;

        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        if (mExtended && mKatexSet && mKatex) throw new IllegalArgumentException("Extended message views do not support Katex!");

        if (!messageList.isEmpty()) findDateBanners();

        mDp3 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
        reload();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Message message = mMessageList.get(position);

        MessageView view = null;
        if (convertView instanceof MessageView) {
            view = (MessageView) convertView;
            if (view.isExtended() != mExtended) {
                view = null;
            }
        }

        if (view == null) {
            view = new MessageView(mContext, mExtended);
        }

        view.setPadding(mDp3, mDp3, mDp3, mDp3);
        view.setKatex(mKatex);
        view.setMessage(message);
        view.setFocusable(false);
        view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        view.setDateBanner(mDateBannerPositions.contains(position) ? message.dateNoTime : null);
        view.setLinkify(mLinkify);
        view.setColorful(mColorful);

        return view;
    }

    public List<Message> getData() {
        return new LinkedList<>(mMessageList);
    }

    @Override
    public void addAll(@NonNull Collection<? extends Message> collection) {
        super.addAll(collection);
        findDateBanners();

        if (mKatex) {
            float size = mContext.getResources().getDimensionPixelSize(R.dimen.message_text_size);
            for (Message message : collection) {
                MathView.extractAndPreload(mContext, message.message, size, message.id, mathPreload);
            }
        }
    }

    public void add(Message message) {
        super.add(message);
        checkDateBanner();

        if (mKatex) {
            float size = mContext.getResources().getDimensionPixelSize(R.dimen.message_text_size);
            MathView.extractAndPreload(mContext, message.message, size, message.id, mathPreload);
        }
    }

    private void findDateBanners() {
        mDateBannerPositions.clear();

        if (mMessageList.size() == 0) return;

        mDateBannerPositions.add(0);

        Iterator<Message> iterator = mMessageList.iterator();
        String lastDate = iterator.next().dateNoTime;
        String dateNoTime;

        int i = 1;
        while (iterator.hasNext()) {
            dateNoTime = iterator.next().dateNoTime;

            //noinspection StringEquality dateNoTime values are created with String.intern()
            if (lastDate != dateNoTime) {
                lastDate = dateNoTime;
                mDateBannerPositions.add(i);
            }

            i++;
        }
    }

    private void checkDateBanner() {
        int size = mMessageList.size();

        if (size < 2) return;

        String lastDateNoTime = mMessageList.get(size - 2).dateNoTime;
        String dateNoTime = mMessageList.get(size - 1).dateNoTime;

        //noinspection StringEquality dateNoTime values are created with String.intern()
        if (lastDateNoTime != dateNoTime) {
            mDateBannerPositions.add(size - 1);
        }
    }

    @Override
    public int getCount() {
        return mMessageList.size();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        if (position < mMessageList.size())
            return mMessageList.get(position).id;
        else
            return -1;
    }

    public void reload() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mColorful = sharedPreferences.getBoolean(Pref.Chat.COLORFUL_MESSAGES,false);
        if (!mLinkifySet)
            mLinkify = sharedPreferences.getBoolean(Pref.Chat.SHOW_LINKS,true);
        if (!mKatexSet)
            mKatex = sharedPreferences.getBoolean(Pref.Chat.KATEX, false);

        MathView.clearCache();
        if (mathPreload != null) mathPreload.removeAllViews();
        if (mKatex && !mExtended) {
            float size = mContext.getResources().getDimensionPixelSize(R.dimen.message_text_size);
            for (Message message : mMessageList) {
                MathView.extractAndPreload(mContext, message.message, size, message.id, mathPreload);
            }
        }
    }

    public void setKatex(Boolean katex) {
        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        reload();
    }

    @Override
    public void clear() {
        super.clear();
        reload();
    }
}
