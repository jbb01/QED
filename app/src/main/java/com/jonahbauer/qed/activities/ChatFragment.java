package com.jonahbauer.qed.activities;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jonahbauer.qed.NetworkListener;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.networking.ChatWebSocket;
import com.jonahbauer.qed.networking.ChatWebSocketListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatFragment extends QEDFragment implements NetworkListener, AbsListView.OnScrollListener, ChatWebSocketListener {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);

    private ChatWebSocket webSocket = null;
    private MessageAdapter messageAdapter;

    private FloatingActionButton scrollDownButton;
    private ListView messageListView;
    private ProgressBar progressBar;
    private EditText messageEditText;
    private ImageButton sendButton;
    private MenuItem refreshButton;
    private FloatingActionButton quickSettings;
    private FloatingActionButton quickSettingsName;
    private FloatingActionButton quickSettingsChannel;
    private final Runnable fabFade = () -> {
        ObjectAnimator animation = ObjectAnimator.ofFloat(quickSettings, "alpha", 0.35f);
        animation.setDuration(1000);
        animation.start();
    };

    private long topPosition = Long.MAX_VALUE;
    private long lastPostId;
    private ChatDatabase database;

    private boolean showQuickSettings = false;

    private boolean initDone = false;
    private List<Message> initMessages;

    private SharedPreferences sharedPreferences;
    private Resources res;

    private final AtomicBoolean networkError = new AtomicBoolean();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        scrollDownButton = view.findViewById(R.id.scroll_down_Button);
        messageListView = view.findViewById(R.id.messageBox);
        progressBar = view.findViewById(R.id.progress_bar);
        messageEditText = view.findViewById(R.id.editText_message);
        sendButton = view.findViewById(R.id.button_send);
        quickSettings = view.findViewById(R.id.quick_settings);
        quickSettingsName = view.findViewById(R.id.quick_settings_name);
        quickSettingsChannel = view.findViewById(R.id.quick_settings_channel);

        assert getContext() != null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        res = getResources();

        messageEditText.setOnClickListener(a -> editTextClicked());
        sendButton.setOnClickListener(a -> send());

        quickSettingsName.hide();
        quickSettingsChannel.hide();
        quickSettings.setAlpha(0.35f);

        Context context = getContext();
        quickSettingsName.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(res.getString(R.string.preferences_chat_name_title));

            @SuppressLint("InflateParams")
            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(sharedPreferences.getString(getString(R.string.preferences_chat_name_key), ""));

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(res.getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(res.getString(R.string.ok), (dialog, which) -> {
                sharedPreferences.edit().putString(res.getString(R.string.preferences_chat_name_key), inputEditText.getText().toString()).apply();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        quickSettingsChannel.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(res.getString(R.string.preferences_chat_channel_title));

            @SuppressLint("InflateParams")
            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(sharedPreferences.getString(getString(R.string.preferences_chat_channel_key), ""));

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(res.getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(res.getString(R.string.ok), (dialog, which) -> {
                sharedPreferences.edit().putString(res.getString(R.string.preferences_chat_channel_key), inputEditText.getText().toString()).apply();
                reload();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        quickSettings.setOnClickListener(a -> {
            if (showQuickSettings) {
                quickSettingsName.postDelayed(fabFade, 5000);
                quickSettingsName.hide();
                quickSettingsChannel.hide();
                showQuickSettings = !showQuickSettings;
            } else {
                quickSettings.removeCallbacks(fabFade);
                quickSettings.setAlpha(1f);
                quickSettingsName.show();
                quickSettingsChannel.show();
                showQuickSettings = !showQuickSettings;
            }
        });

        messageAdapter = new MessageAdapter(view.getContext(), new ArrayList<>());
        messageListView.setAdapter(messageAdapter);
        messageListView.setOnScrollListener(this);
        messageListView.setItemsCanFocus(false);
        initMessages = new LinkedList<>();

        scrollDownButton.setOnClickListener(a -> scrollDown());

        database = new ChatDatabase();
        database.init(getContext(), null);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.chat_refresh) {
            refreshButton = item;
            item.setEnabled(false);
            item.setChecked(true);

            Drawable icon = refreshButton.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        reload();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (webSocket != null) webSocket.closeSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (database != null) database.close();
        if (webSocket != null) webSocket.closeSocket();
    }

    private void reload() {
        if (webSocket != null) webSocket.closeSocket();
        networkError.set(false);

        assert getActivity() != null;

        messageAdapter.clear();
        initDone = false;
        progressBar.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);

        initSocket();
        if (messageEditText!=null) {
            messageEditText.setEnabled(true);
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (sendButton!=null) sendButton.setEnabled(true);

        messageAdapter = new MessageAdapter(getContext(), messageAdapter.getData());
        messageListView.setAdapter(messageAdapter);

        if (refreshButton != null) refreshButton.setEnabled(true);
    }

    private void addPost(@NonNull Message message, @SuppressWarnings("SameParameterValue") int position, boolean notify, boolean checkDate) {
        assert getContext() != null;

        if (Message.PONG.equals(message)) {
            initDone = true;
            handler.post(() -> {
                messageAdapter.clear();
                messageAdapter.addAll(initMessages);
                database.insertAll(initMessages);
                messageAdapter.notifyDataSetChanged();
                messageListView.setSelection(initMessages.size() - 1);

                progressBar.setVisibility(View.GONE);
                messageListView.setVisibility(View.VISIBLE);
            });
            return;
        }

        if (!initDone) {
            initMessages.add(message);
            return;
        }

        database.insert(message);

        if (message.id < topPosition) topPosition = message.id;
        if (message.bottag == 1 && sharedPreferences.getBoolean(res.getString(R.string.preferences_chat_showSense_key),false)) return;

        if (messageAdapter != null) {
            handler.post(() -> {
                if (checkDate) {
                    Message lastMessage = (Message) messageListView.getItemAtPosition((position == -1) ? messageListView.getCount() - 1 : position - 1);
                    String lastDate;
                    if (lastMessage != null)
                        lastDate = lastMessage.date.split(" ")[0];
                    else lastDate = "";
                    String date = message.date.split(" ")[0];

                    if (!date.equals(lastDate)) {
                        try {
                            Date d = sdf.parse(date);
                            if (d != null)
                                date = android.text.format.DateFormat.getLongDateFormat(getContext()).format(d).toUpperCase();
                        } catch (ParseException ignored) {}
                        if (position == -1)
                            messageAdapter.add(new Message("date", "date", date, -6473, "date", "date", -6473, -6473, "date"));
                        else
                            messageAdapter.add(position, new Message("date", "date", date, -6473, "date", "date", -6473, -6473, "date"));
                    }
                }

                if (position==-1) messageAdapter.add(message);
                else messageAdapter.add(position,message);
                if (notify) {
                    messageAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void addPost(Message message, boolean notify) {
        this.addPost(message, -1, notify, true);
    }

    private void editTextClicked() {
        if (messageListView.getLastVisiblePosition() >= messageAdapter.getCount()-1)
            handler.postDelayed(() -> messageListView.setSelection(messageAdapter.getCount() -1),100);
    }

    public void onConnectionFail() {
        onError(REASON_NETWORK, null);
    }

    @Override
    public void onConnectionRegain() {
        handler.post(this::reload);
    }

    private void initSocket() {
        if (webSocket == null) webSocket = new ChatWebSocket(this);

        webSocket.openSocket();
    }

    private void send() {
        send(false);
    }

    private void send(boolean force) {
        assert getContext() != null;

        String message = messageEditText.getText().toString();

        if (!force && "".equals(message.trim())) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setMessage(res.getString(R.string.chat_empty_message));
            dialogBuilder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                send(true);
                dialogInterface.dismiss();
            });
            dialogBuilder.setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss());
            dialogBuilder.show();
            return;
        }

        if (webSocket.send(message)) {
            handler.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
            messageEditText.setText("");
            messageEditText.requestFocus();
        } else {
            handler.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_error_red,0));
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (scrollDownButton != null) if (totalItemCount-visibleItemCount-firstVisibleItem > 0) scrollDownButton.show();
        else scrollDownButton.hide();
    }

    private void scrollDown() {
        handler.post(() -> messageListView.smoothScrollToPositionFromTop(messageAdapter.getCount(),0,250));
    }

    @Override
    public void onMessage(@NonNull Message message) {
        addPost(message, initDone);

        if (lastPostId < message.id) lastPostId = message.id;
    }

    @Override
    public void onError(@Nullable String reason, @Nullable Throwable cause) {
        ChatWebSocketListener.super.onError(reason, cause);

        if (networkError.getAndSet(true)) return;

        if (REASON_NETWORK.equals(reason))
            error(res.getString(R.string.cant_connect));
        else
            error(res.getString(R.string.unknown_error));

        handler.postDelayed(() -> networkError.set(false), 1000);
    }

    private void error(String message) {
        if (webSocket != null) webSocket.closeSocket();
        initDone = true;
        Calendar cal = Calendar.getInstance();

        String year = "0000" + cal.get(Calendar.YEAR);
        year = year.substring(year.length()-4);
        String month = "00" + cal.get(Calendar.MONTH);
        month = month.substring(month.length()-2);
        String day = "00" + cal.get(Calendar.DAY_OF_MONTH);
        day = day.substring(day.length()-2);
        String hour = "00" + cal.get(Calendar.HOUR_OF_DAY);
        hour = hour.substring(hour.length()-2);
        String minute = "00" + cal.get(Calendar.MINUTE);
        minute = minute.substring(minute.length()-2);
        String second = "00" + cal.get(Calendar.SECOND);
        second = second.substring(second.length()-2);
        String date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
        addPost(new Message("Error", message, date,503,"Error","220000", Integer.MAX_VALUE, 0, null), -1, true, false);
        handler.post(() -> {
            messageEditText.setEnabled(false);
            sendButton.setEnabled(false);

            progressBar.setVisibility(View.GONE);
            messageListView.setVisibility(View.VISIBLE);
        });
    }
}
