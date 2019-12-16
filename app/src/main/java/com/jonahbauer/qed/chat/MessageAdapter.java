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

import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.MathView;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends ArrayAdapter<Message> {
    private final Context context;
    private final List<Message> messageList;
    private final boolean extended;
    private final boolean mathMode;
    private final SharedPreferences sharedPreferences;

    private LinearLayout mathPreload;

    public MessageAdapter(Context context, @NonNull List<Message> messageList, boolean extended) {
        this(context, messageList, extended, false, null);
    }
    private MessageAdapter(Context context, @NonNull List<Message> messageList, boolean extended, boolean mathMode, LinearLayout mathPreload) {
        super(context, R.layout.list_item_message, messageList);
        this.context = context;
        this.messageList = messageList;
        this.extended = extended;
        this.mathMode = mathMode;
        this.mathPreload = mathPreload;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (mathMode) for (Message message : messageList)
            MathView.extractAndPreload(context, message, context.getResources().getDimensionPixelSize(R.dimen.message_text_size), mathPreload);
    }

    public MessageAdapter(Context context, @NonNull List<Message> messageList) {
        this(context, messageList, false, false, null);
    }

    public MessageAdapter(Context context, @NonNull List<Message> messageList, boolean mathMode, LinearLayout mathPreload) {
        this(context, messageList, false, mathMode, mathPreload);
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
            else view = Objects.requireNonNull(inflater).inflate(isDate ? R.layout.date_inline : (mathMode ? R.layout.list_item_message_math : R.layout.list_item_message), parent, false);
        }

        if (isDate) {
            view.findViewById(R.id.date).setFocusable(false);
            ((TextView)view.findViewById(R.id.date_text)).setText(message.date);
            return view;
        }

        TextView nameView = view.findViewById(R.id.message_name);

        MathView messageViewMath = null;
        TextView messageView = null;

        if (mathMode) messageViewMath = view.findViewById(R.id.message_message);
        else messageView = view.findViewById(R.id.message_message);

        if (BuildConfig.DEBUG && !((mathMode && messageViewMath != null) || (!mathMode && messageView != null))) {
            throw new AssertionError();
        }

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
            if (!mathMode) {
                float spaceWidth = messageView.getPaint().measureText(" ");
                for (int i = 0; i < Math.ceil((dateWidth + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics())) / spaceWidth); i++)
                    messageText.append(" ");
            } else {
                float spaceWidth = messageViewMath.getSpaceWidth();
                for (int i = 0; i < Math.ceil((dateWidth + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics())) / spaceWidth); i++)
                    messageText.append(" ");
            }
        }

        nameView.setText(name);
        dateView.setText(date);

        if (mathMode) messageViewMath.setMessage(message);
        else messageView.setText(messageText.toString());

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


        if (!mathMode && sharedPreferences.getBoolean(view.getContext().getString(R.string.preferences_chat_showLinks_key),true)) {
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
            if (mathMode) messageViewMath.setTextColor(color);
            else messageView.setTextColor(color);
        }

        return view;
    }

    public List<Message> getData() {
        return new LinkedList<>(messageList);
    }

    @Override
    public void addAll(@NonNull Collection<? extends Message> collection) {
        if (mathMode) for (Message message : collection) {
            MathView.extractAndPreload(context, message, context.getResources().getDimensionPixelSize(R.dimen.message_text_size), mathPreload);
        }
        super.addAll(collection);
    }

    public void add(Message message) {
        super.add(message);
    }
}