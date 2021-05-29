package com.jonahbauer.qed.activities.mainFragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.messageInfoSheet.MessageInfoBottomSheet;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.networking.ChatWebSocket;
import com.jonahbauer.qed.networking.ChatWebSocketListener;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.util.Preferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatFragment extends QEDFragment implements NetworkListener, AbsListView.OnScrollListener, ChatWebSocketListener {
    private Resources mRes;
    private ChatDatabase mDatabase;

    private final Object mSocketLock = new Object();
    private ChatWebSocket mWebSocket = null;
    private MessageAdapter mMessageAdapter;

    private boolean mInitDone = false;
    private List<Message> mInitMessages;

    private long mTopPosition = Long.MAX_VALUE;
    private long mLastPostId;

    private FloatingActionButton mScrollDownButton;
    private ListView mMessageListView;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageButton mSendButton;

    private MenuItem mRefreshButton;

    private boolean mShowQuickSettings = false;
    private FloatingActionButton mQuickSettings;
    private FloatingActionButton mQuickSettingsName;
    private FloatingActionButton mQuickSettingsChannel;
    private final Runnable mFabFade = () -> {
        ObjectAnimator animation = ObjectAnimator.ofFloat(mQuickSettings, "alpha", 0.35f);
        animation.setDuration(1000);
        animation.start();
    };

    private final AtomicBoolean mNetworkError = new AtomicBoolean();

    @NonNull
    public static ChatFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_chat);

        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LinearLayout mathPreload = view.findViewById(R.id.math_preload);
        mScrollDownButton = view.findViewById(R.id.scroll_down_Button);
        mMessageListView = view.findViewById(R.id.messageBox);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mMessageEditText = view.findViewById(R.id.editText_message);
        mSendButton = view.findViewById(R.id.button_send);
        mQuickSettings = view.findViewById(R.id.quick_settings);
        mQuickSettingsName = view.findViewById(R.id.quick_settings_name);
        mQuickSettingsChannel = view.findViewById(R.id.quick_settings_channel);

        mRes = getResources();

        mMessageEditText.setOnClickListener(a -> editTextClicked());
        mSendButton.setOnClickListener(a -> send());

        mQuickSettingsName.hide();
        mQuickSettingsChannel.hide();
        mQuickSettings.setAlpha(0.35f);

        Context context = getContext();
        mQuickSettingsName.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(mRes.getString(R.string.preferences_chat_name_title));

            @SuppressLint("InflateParams")
            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(Preferences.chat().getName());

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(mRes.getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(mRes.getString(R.string.ok), (dialog, which) -> {
                Preferences.chat().edit().setName(inputEditText.getText().toString()).apply();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        mQuickSettingsChannel.setOnClickListener(a -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);

            inputDialog.setTitle(mRes.getString(R.string.preferences_chat_channel_title));

            @SuppressLint("InflateParams")
            View editTextView = LayoutInflater.from(inputDialog.getContext()).inflate(R.layout.alert_dialog_edit_text, null);

            EditText inputEditText = editTextView.findViewById(R.id.input);
            inputEditText.setText(Preferences.chat().getChannel());

            inputDialog.setView(editTextView);

            inputDialog.setNegativeButton(mRes.getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            inputDialog.setPositiveButton(mRes.getString(R.string.ok), (dialog, which) -> {
                Preferences.chat().edit().setChannel(inputEditText.getText().toString()).apply();
                reload();
                dialog.dismiss();
            });

            inputDialog.show();
        });

        mQuickSettings.setOnClickListener(a -> {
            if (mShowQuickSettings) {
                mQuickSettingsName.postDelayed(mFabFade, 5000);
                mQuickSettingsName.hide();
                mQuickSettingsChannel.hide();
            } else {
                mQuickSettings.removeCallbacks(mFabFade);
                mQuickSettings.setAlpha(1f);
                mQuickSettingsName.show();
                mQuickSettingsChannel.show();
            }
            mShowQuickSettings = !mShowQuickSettings;
        });

        mMessageAdapter = new MessageAdapter(view.getContext(), new ArrayList<>(), mathPreload);
        mMessageListView.setAdapter(mMessageAdapter);
        mMessageListView.setOnScrollListener(this);
        mMessageListView.setOnItemClickListener((adapterView, view1, i, l) -> setChecked(i, false));
        mMessageListView.setOnItemLongClickListener((adapterView, view1, i, l) -> {
            if (!mMessageListView.isItemChecked(i)) {
                int checked = mMessageListView.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(i, true);
                return true;
            } else return false;
        });
        mInitMessages = new LinkedList<>();

        mScrollDownButton.setOnClickListener(a -> scrollDown());

        mDatabase = new ChatDatabase();
        mDatabase.init(getContext(), null);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.chat_refresh) {
            mRefreshButton = item;
            item.setEnabled(false);
            item.setChecked(true);

            Drawable icon = mRefreshButton.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the checked item in the {@link #mMessageListView} and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     * @param value if the item is checked or not
     */
    private void setChecked(int position, boolean value) {
        mMessageListView.setItemChecked(position, value);

        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;

            if (value) {
                Message msg = mMessageAdapter.getItem(position);

                Toolbar toolbar = mainActivity.borrowAltToolbar();
                toolbar.setNavigationOnClickListener(v -> setChecked(position, false));

                toolbar.inflateMenu(R.menu.menu_chat_message);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.message_info) {
                        MessageInfoBottomSheet sheet = MessageInfoBottomSheet.newInstance(msg, R.style.AppTheme_BottomSheetDialog);
                        sheet.show(getChildFragmentManager(), sheet.getTag());
                    } else if (item.getItemId() == R.id.message_reply) {
                        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
                    }

                    return false;
                });

                if (msg != null) toolbar.setTitle(msg.getName());
            } else {
                mainActivity.returnAltToolbar();
            }
        }
    }

    @Override
    public void revokeAltToolbar() {
        int checked = mMessageListView.getCheckedItemPosition();
        if (checked != -1) setChecked(checked, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        reload();
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        mMessageListView.setItemChecked(mMessageListView.getCheckedItemPosition(), false);
//    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWebSocket != null) mWebSocket.closeSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDatabase != null) mDatabase.close();
        if (mWebSocket != null) mWebSocket.closeSocket();
    }

    /**
     * Reconnects to the chat web socket
     */
    private void reload() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.returnAltToolbar();
        }

        if (mWebSocket != null) mWebSocket.closeSocket();
        mNetworkError.set(false);

        assert getActivity() != null;

        mInitMessages.clear();
        mMessageAdapter.clear();
        mInitDone = false;
        mProgressBar.setVisibility(View.VISIBLE);
        mMessageListView.setVisibility(View.GONE);

        initSocket();
        if (mMessageEditText != null) {
            mMessageEditText.setEnabled(true);
            mMessageEditText.post(() -> mMessageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (mSendButton != null) mSendButton.setEnabled(true);

        mMessageAdapter.notifyDataSetChanged();

        if (mRefreshButton != null) mRefreshButton.setEnabled(true);
    }

    /**
     * Appends the given message to the {@link #mMessageListView}. The post will also be written to the chat database
     *
     * @param message the message to be appended
     * @param notify if {@link MessageAdapter#notifyDataSetChanged()} should be called
     */
    private void addPost(@NonNull Message message, boolean notify) {
        if (getContext() == null) return;

        if (Message.PONG.equals(message)) {
            mInitDone = true;
            mHandler.post(() -> {
                mMessageAdapter.clear();
                mMessageAdapter.addAll(mInitMessages);
                mDatabase.insertAll(mInitMessages);
                mMessageAdapter.notifyDataSetChanged();
                mMessageListView.setSelection(mInitMessages.size() - 1);

                mProgressBar.setVisibility(View.GONE);
                mMessageListView.setVisibility(View.VISIBLE);
            });
            return;
        }

        if (!mInitDone) {
            mInitMessages.add(message);
            return;
        }

        mDatabase.insert(message);

        if (message.getId() < mTopPosition) mTopPosition = message.getId();
        if (message.getBottag() == 1 && Preferences.chat().isSense()) return;

        mHandler.post(() -> {
            mMessageAdapter.add(message);
            if (notify) mMessageAdapter.notifyDataSetChanged();
        });
    }

    private void editTextClicked() {
        if (mMessageListView.getLastVisiblePosition() >= mMessageAdapter.getCount()-1)
            mHandler.postDelayed(() -> mMessageListView.setSelection(mMessageAdapter.getCount() -1),100);
    }

    public void onConnectionFail() {
        onError(REASON_NETWORK, null);
    }

    @Override
    public void onConnectionRegain() {
        mHandler.post(this::reload);
    }

    /**
     * Opens a web socket connection to the chat server
     */
    private void initSocket() {
        new Thread(() -> {
            synchronized (mSocketLock) {
                if (mWebSocket == null) mWebSocket = new ChatWebSocket(this);

                mWebSocket.openSocket();
            }
        }).start();
    }

    private void send() {
        send(false);
    }

    /**
     * Sends a chat post to the server
     * The content of the post comes from {@link #mMessageEditText} and various preferences.
     *
     * @param force if this is false the user will be prompted before sending empty posts
     */
    private void send(boolean force) {
        assert getContext() != null;

        String message = mMessageEditText.getText().toString();

        if (!force && "".equals(message.trim())) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setMessage(mRes.getString(R.string.chat_empty_message));
            dialogBuilder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                send(true);
                dialogInterface.dismiss();
            });
            dialogBuilder.setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss());
            dialogBuilder.show();
            return;
        }

        if (mWebSocket.send(message)) {
            mHandler.post(() -> mMessageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
            mMessageEditText.setText("");
            mMessageEditText.requestFocus();
        } else {
            mHandler.post(() -> mMessageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_error_red,0));
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mScrollDownButton != null) if (totalItemCount-visibleItemCount-firstVisibleItem > 0) mScrollDownButton.show();
        else mScrollDownButton.hide();
    }

    private void scrollDown() {
        mMessageListView.smoothScrollToPositionFromTop(mMessageAdapter.getCount(), 0, 250);
    }

    @Override
    public void onMessage(@NonNull Message message) {
        addPost(message, mInitDone);

        if (mLastPostId < message.getId()) mLastPostId = message.getId();
    }

    @Override
    public void onError(@Nullable String reason, @Nullable Throwable cause) {
        ChatWebSocketListener.super.onError(reason, cause);

        if (mNetworkError.getAndSet(true)) return;

        if (REASON_NETWORK.equals(reason))
            error(mRes.getString(R.string.cant_connect));
        else
            error(mRes.getString(R.string.error_unknown));

        mHandler.postDelayed(() -> mNetworkError.set(false), 1000);
    }

    /**
     * Appends a chat post that resembles an error with the given error message
     *
     * @param message a meaningful error message
     */
    private void error(String message) {
        if (mWebSocket != null) mWebSocket.closeSocket();
        mInitDone = true;
        Calendar cal = Calendar.getInstance();

        Locale locale = mRes.getConfiguration().getLocales().get(0);

        String date = String.format(locale, "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", cal);
        addPost(new Message("Error", message, date,503,"Error","220000", Integer.MAX_VALUE, 0, ""), true);
        mHandler.post(() -> {
            mMessageEditText.setEnabled(false);
            mSendButton.setEnabled(false);

            mProgressBar.setVisibility(View.GONE);
            mMessageListView.setVisibility(View.VISIBLE);
        });
    }
}
