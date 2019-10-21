package com.jonahbauer.qed.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.LayerDrawable;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.R;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends ArrayAdapter<Message> {
    private final Context context;
    private final List<Message> messageList;
    private final boolean extended;
    private SharedPreferences sharedPreferences;

    public MessageAdapter(Context context, @NonNull List<Message> messageList, boolean extended) {
        super(context, R.layout.list_item_message, messageList);
        this.context = context;
        this.messageList = messageList;
        this.extended = extended;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public MessageAdapter(Context context, List<Message> messageList) {
        this(context, messageList, false);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Message message = messageList.get(position);

        boolean isDate = false;
        if (message.bottag == -6473 && message.id == -6473 && message.channel.equals("date")) {
            isDate = true;
        }

        View view;
        if (convertView != null && ((isDate && convertView.findViewById(R.id.date) != null) || (!isDate && convertView.findViewById(R.id.message) != null))) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (extended) view = Objects.requireNonNull(inflater).inflate(R.layout.extended_message, parent, false);
            else view = Objects.requireNonNull(inflater).inflate(isDate ? R.layout.date_inline : R.layout.list_item_message, parent, false);
        }

        if (isDate) {
            view.findViewById(R.id.date).setFocusable(false);
            ((TextView)view.findViewById(R.id.date_text)).setText(message.date);
            return view;
        }

        TextView nameView = view.findViewById(R.id.message_name);
        TextView messageView = view.findViewById(R.id.message_message);
        TextView dateView = view.findViewById(R.id.message_date);


        String name = message.name;
        if (message.userName != null) name += " | " + message.userName;

        if (extended) {
            String messageId = "#" + message.id;
            ((TextView) view.findViewById(R.id.message_id)).setText(messageId);
            ((TextView) view.findViewById(R.id.message_channel)).setText("".equals(message.channel) ? "(main)" : ("(" + message.channel + ")"));
        }

        String date = message.date;
        if (!extended) {
            String[] dates = message.date.split("(:|\\s)");
            date = dates[1] + ":" + dates[2];
        }

        StringBuilder messageText = new StringBuilder(message.message);
        if (!extended) {
            float dateWidth = dateView.getPaint().measureText(date);
            float spaceWidth = messageView.getPaint().measureText(" ");
            for (int i = 0; i < Math.ceil((dateWidth + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics())) / spaceWidth); i++)
                messageText.append(" ");
        }

        nameView.setText(name);
        dateView.setText(date);
        messageView.setText(messageText.toString());

        if (!extended) {
            LinearLayout.LayoutParams dateParams = (LinearLayout.LayoutParams) dateView.getLayoutParams();
            dateParams.setMargins(0, -(int) context.getResources().getDimension(R.dimen.message_text_size), 0, 0);
            dateView.setLayoutParams(dateParams);
        }

        if (!extended) {
            view.findViewById(R.id.message_layout).setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    ((LayerDrawable)view.getBackground()).findDrawableByLayerId(R.id.message_background_bubble).getOutline(outline);
                }
            });
        }


        if (sharedPreferences.getBoolean(view.getContext().getString(R.string.preferences_chat_showLinks_key),true)) {
            messageView.setLinksClickable(true);
            Linkify.addLinks(messageView, Linkify.WEB_URLS);
        }

        int color = -Color.parseColor("#" + message.color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r *= 255-(255-r)*0.6;
        g *= 255-(255-g)*0.6;
        b *= 255-(255-b)*0.6;

        color = Color.rgb(r,g,b);

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        hsv[1] = 1-(1-hsv[1])*0.3f;

        color = Color.HSVToColor(hsv);

        nameView.setTextColor(color);
        if (!extended && sharedPreferences.getBoolean(view.getContext().getString(R.string.preferences_chat_colorful_messages_key), false)) {
            messageView.setTextColor(color);
        }

        return view;
    }

    public List<Message> getData() {
        return messageList;
    }

    @SuppressWarnings("unused")
    public void addAll(int index, Collection<? extends Message> collection) {
        messageList.addAll(index, collection);
    }

    public void add(int index, Message message) {
        messageList.add(index, message);
    }
}