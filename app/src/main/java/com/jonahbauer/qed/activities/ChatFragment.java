package com.jonahbauer.qed.activities;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.Internet;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment implements Internet, AbsListView.OnScrollListener {
    private WebSocket websocket = null;
    private MessageAdapter messageAdapter;

    private FloatingActionButton scrollDownButton;
    private ListView messageListView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private MenuItem refreshButton;
    private FloatingActionButton quickSettings;
    private FloatingActionButton quickSettingsName;
    private FloatingActionButton quickSettingsChannel;
    private Runnable fabFade = () -> {
        ObjectAnimator animation = ObjectAnimator.ofFloat(quickSettings, "alpha", 0.35f);
        animation.setDuration(1000);
        animation.start();
    };

    private long topPosition = Long.MAX_VALUE;
    private long position;
    private boolean sending = false;
    private long lastPostId;
    private ChatDatabase database;

    private boolean showQuickSettings = false;

    private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(getActivity(), Application.darkMode ? R.style.AppTheme_Dark_Chat : R.style.AppTheme_Chat);
        inflater = inflater.cloneInContext(context);

        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

//        ImageView background = view.findViewById(R.id.background);
        scrollDownButton = view.findViewById(R.id.scroll_down_Button);
        messageListView = view.findViewById(R.id.messageBox);
        messageEditText = view.findViewById(R.id.editText_message);
        sendButton = view.findViewById(R.id.button_send);
        quickSettings = view.findViewById(R.id.quick_settings);
        quickSettingsName = view.findViewById(R.id.quick_settings_name);
        quickSettingsChannel = view.findViewById(R.id.quick_settings_channel);

        assert getContext() != null;
        sharedPreferences = getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE);
        
//        TileDrawable tileDrawable  = new TileDrawable(view.getContext().getDrawable(R.drawable.background_part), Shader.TileMode.REPEAT);
//        background.setImageDrawable(tileDrawable);

        messageEditText.setOnClickListener(a -> editTextClicked());
        sendButton.setOnClickListener(a -> send());

        quickSettingsName.hide();
        quickSettingsChannel.hide();
        quickSettings.setAlpha(0.35f);

        quickSettingsName.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(getString(R.string.preferences_chat_name_title));

            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(sharedPreferences.getString(getString(R.string.preferences_chat_name_key), ""));

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                sharedPreferences.edit().putString(getString(R.string.preferences_chat_name_key), inputEditText.getText().toString()).apply();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        quickSettingsChannel.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(getString(R.string.preferences_chat_channel_title));

            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(sharedPreferences.getString(getString(R.string.preferences_chat_channel_key), ""));

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                sharedPreferences.edit().putString(getString(R.string.preferences_chat_channel_key), inputEditText.getText().toString()).apply();
                reload();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        quickSettings.setOnClickListener(a -> {
            if (showQuickSettings) {
                quickSettings.postDelayed(fabFade, 5000);
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
        if (websocket != null) websocket.close(1000, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (database != null) database.close();
        if (websocket != null) websocket.close(1000, null);
    }

    private void reload() {
        position = -100;
        if (websocket != null) websocket.close(1000, null);

        assert getActivity() != null;

        messageAdapter.clear();
        messageAdapter.notifyDataSetChanged();

        initSocket();
        if (messageEditText!=null) {
            messageEditText.setEnabled(true);
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (sendButton!=null) sendButton.setEnabled(true);

        messageAdapter = new MessageAdapter(getContext(), messageAdapter.getData());
        messageListView.setAdapter(messageAdapter);

        if (refreshButton != null) refreshButton.setEnabled(true);

        new Handler().postDelayed(() -> {
            if (!Application.online) onConnectionFail();
        }, 5000);
    }

    private void addPost(Message message, @SuppressWarnings("SameParameterValue") int position, boolean notify, boolean checkDate) {
        assert getContext() != null;

        if (message.id < topPosition) topPosition = message.id;
        if (message.bottag == 1 && getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).getBoolean(getString(R.string.preferences_chat_showSense_key),false)) return;

        if (messageAdapter!=null) {
            messageListView.post(() -> {
                if (checkDate) {
                    Message lastMessage = (Message) messageListView.getItemAtPosition((position == -1) ? messageListView.getCount() - 1 : position - 1);
                    String lastDate;
                    if (lastMessage != null)
                        lastDate = lastMessage.date.split(" ")[0];
                    else lastDate = "";
                    String date = message.date.split(" ")[0];

                    if (!date.equals(lastDate)) {
                        try {
                            date = android.text.format.DateFormat.getLongDateFormat(getContext()).format(new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse(date)).toUpperCase();
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

    private void addPost(Message message) {
        this.addPost(message, -1, true, true);
    }

    private void editTextClicked() {
        if (messageListView.getLastVisiblePosition() >= messageAdapter.getCount()-1)
            messageListView.postDelayed(() -> messageListView.setSelection(messageAdapter.getCount() -1),100);
    }

    public void onConnectionFail() {
        if (websocket!=null) websocket.close(1000,null);
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
        addPost(new Message("Error",getString(R.string.cant_connect), date,503,"Error","220000", Integer.MAX_VALUE, 0, null), -1, false, false);
        messageEditText.post(() -> messageEditText.setEnabled(false));
        sendButton.post(() -> sendButton.setEnabled(false));
    }

    @Override
    public void onConnectionRegain() {
        messageListView.post(this::reload);
    }

    private void initSocket() {
        assert getContext() != null;
        assert getActivity() != null;

        if (websocket!=null) websocket.close(1000,null);
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build();
        Request.Builder builder = new Request.Builder().url(
                getString(R.string.chat_websocket)
                        + "?channel=" + getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).getString(getString(R.string.preferences_chat_channel_key), "")
                        + "&version=" + "2"
                        + "&position=" + position);
        builder.addHeader("Origin", "https://chat.qed-verein.de");

        if (((Application)getActivity().getApplication()).loadData(Application.KEY_CHAT_PWHASH, true) == null) {
            startActivity(new Intent(getActivity(),LoginActivity.class));
            getActivity().finish();
            return;
        }

        builder.addHeader("Cookie", "userid=" + ((Application)getActivity().getApplication()).loadData(Application.KEY_USERID, false) + ", pwhash=" + ((Application)getActivity().getApplication()).loadData(Application.KEY_CHAT_PWHASH, true));
        Request request = builder.build();
        websocket = client.newWebSocket(request, new ChatWebSocketListener());
        websocket.send("{\"type\":\"ping\"}");
    }

    private void send() {
        send(false);
    }

    private void send(boolean force) {
        assert getContext() != null;

        if (sending || websocket==null) {
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_error_red,0));
            return;
        } else {
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }

        String message = messageEditText.getText().toString();

        if (!force && "".equals(message.trim())) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setMessage(getContext().getString(R.string.chat_empty_message));
            dialogBuilder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                send(true);
                dialogInterface.dismiss();
            });
            dialogBuilder.setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss());
            dialogBuilder.show();
            return;
        }

        sending = true;
        try {
            JSONObject json = new JSONObject();
            json.put("channel", getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).getString(getString(R.string.preferences_chat_channel_key), ""));
            json.put("name", getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).getString(getString(R.string.preferences_chat_name_key), ""));
            json.put("message", messageEditText.getText().toString());
            json.put("delay", Long.toString(position));
            json.put("publicid", getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).getBoolean(getString(R.string.preferences_chat_publicId_key), false) ? 1 : 0);
            websocket.send(json.toString());
            messageEditText.setText("");
            messageEditText.requestFocus();
        } catch (JSONException e) {
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_error_red,0));
            messageEditText.requestFocus();
            sending = false;
            e.printStackTrace();
        }
    }

//    @Override
//    public void onReceiveLogs(List<Message> messages) {
//        database.insertAll(messages);
//    }
//
//    @Override
//    public void onLogError() {}
//
//    @Override
//    public void onOutOfMemory() {}

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (scrollDownButton != null) if (totalItemCount-visibleItemCount-firstVisibleItem > 0) scrollDownButton.show();
        else scrollDownButton.hide();
    }

    private void scrollDown() {
        messageListView.post(() -> messageListView.smoothScrollToPositionFromTop(messageAdapter.getCount(),0,250));
    }

    private final class ChatWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            assert getView() != null;
            try {
                JSONObject json = new JSONObject(text);
                switch (json.getString("type")) {
                    case "ping":
                        websocket.send("{\"type\":\"pong\"}");
                        break;
                    case "pong":
//                        messageListView.post(() -> {
//                            String options = null;
//                            try {
//                                options = URLEncoder.encode("mode", "UTF-8") + "=" + URLEncoder.encode("postinterval", "UTF-8");
//                                options += "&" + URLEncoder.encode("from", "UTF-8") + "=" + lastDatabasePostId;
//                                options += "&" + URLEncoder.encode("to", "UTF-8") + "=" + lastPostId;
//                            } catch (UnsupportedEncodingException e) {
//                                e.printStackTrace();
//                            }
//                            LogGetter logGetter = new LogGetter();
//                            logGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, options, ChatFragment.this, ChatFragment.this);
//                        });
//                        messageListView.post(() -> messageListView.smoothScrollToPositionFromTop(messageAdapter.getCount(),0,250));
                        break;
                    case "ack":
                        sending = false;
                        break;
                    case "post":
                        //Log.d("test", text);
                        int id = json.getInt("id");
                        if (id < position) break;
                        position = id + 1;
                        String name = json.getString("name");
                        String messageStr = json.getString("message");
                        String username = json.getString("username");
                        String color = json.getString("color");
                        String date = json.getString("date");
                        String channel = json.getString("channel");
                        int userid = json.optInt("user_id", -1);
                        int bot = json.getInt("bottag");
                        name = name.trim();
                        if ("null".equals(username)) username = null;

                        Message message = new Message(name, messageStr, date, userid, username, color, id, bot, channel);

                        addPost(message);
                        database.insert(message);

                        if (lastPostId < message.id) lastPostId = message.id;

                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            if (reason.contains("Ungültige Anmeldedaten.")) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.chat_login_failed));
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            if (sending) {
                sending = false;
            }
        }
    }
}
