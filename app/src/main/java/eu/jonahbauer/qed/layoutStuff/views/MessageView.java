package eu.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;

import android.text.style.StyleSpan;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.util.MessageUtils;

import java.time.Instant;

@SuppressWarnings("unused")
public interface MessageView {
    StyleSpan USERNAME_STYLE = new StyleSpan(Typeface.ITALIC);

    void setName(CharSequence name);
    CharSequence getName();

    void setMessage(CharSequence message);
    CharSequence getMessage();

    void setTimestamp(CharSequence timestamp);
    void setTimestamp(Instant instant);
    CharSequence getTimestamp();

    void setMessage(Message message);

    void setNameTextAppearance(@StyleRes int textAppearance);
    void setMessageTextAppearance(@StyleRes int textAppearance);
    void setDataTextAppearance(@StyleRes int textAppearance);

    void setNameColor(ColorStateList color);
    void setMessageColor(ColorStateList color);
    void setDataColor(ColorStateList color);

    void setColorful(boolean colorful);
    boolean isColorful();

    void setLinkify(boolean linkify);
    boolean isLinkify();

    static CharSequence formatName(Context context, String name, @Nullable String username) {
        SpannableStringBuilder out = new SpannableStringBuilder();

        out.append(MessageUtils.formatName(context, name));

        if (username != null) {
            out.append(" | ").append(username, USERNAME_STYLE, 0);
        }

        return out;
    }

    static CharSequence formatChannel(Context context, String channel) {
        if (channel.isEmpty()) {
            return context.getText(R.string.message_channel_main);
        } else {
            return channel;
        }
    }
}
