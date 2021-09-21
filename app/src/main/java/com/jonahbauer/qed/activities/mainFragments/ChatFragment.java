package com.jonahbauer.qed.activities.mainFragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.databinding.FragmentChatBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.ChatWebSocket;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatFragment extends QEDFragment implements NetworkListener, AbsListView.OnScrollListener {
    private static final String LOG_TAG = ChatFragment.class.getName();

    private FragmentChatBinding mBinding;

    private ChatWebSocket mWebSocket;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private boolean mInitDone = false;
    private List<Message> mInitMessages;
    private MessageAdapter mMessageAdapter;

    private long mTopPosition = Long.MAX_VALUE;
    private long mLastPostId;

    @Nullable
    private MenuItem mRefreshButton;

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
        mBinding = FragmentChatBinding.bind(view);

        mBinding.editTextMessage.setOnClickListener(v -> {
            if (mBinding.messageBox.getLastVisiblePosition() >= mMessageAdapter.getCount() - 1) {
                mHandler.postDelayed(() -> mBinding.messageBox.setSelection(mMessageAdapter.getCount() - 1), 100);
            }
        });
        mBinding.buttonSend.setOnClickListener(v -> send());

        mBinding.quickSettingsName.hide();
        mBinding.quickSettingsChannel.hide();
        mBinding.quickSettings.setAlpha(0.35f);

        Context context = getContext();
        // setup quick settings
        mBinding.quickSettingsName.setOnClickListener(v -> {
            ViewUtils.showPreferenceDialog(
                    context,
                    R.string.preferences_chat_name_title,
                    () -> Preferences.chat().getName(),
                    (str) -> Preferences.chat().edit().setName(str).apply()
            );
        });
        mBinding.quickSettingsChannel.setOnClickListener(v -> {
            ViewUtils.showPreferenceDialog(
                    context,
                    R.string.preferences_chat_channel_title,
                    () -> Preferences.chat().getChannel(),
                    (str) -> {
                        Preferences.chat().edit().setChannel(str).apply();
                        reload();
                    }
            );
        });
        mBinding.quickSettings.setOnClickListener(new View.OnClickListener() {
            private boolean mShowQuickSettings = false;
            private final Runnable mFabFade = () -> {
                ObjectAnimator animation = ObjectAnimator.ofFloat(mBinding.quickSettings, "alpha", 0.35f);
                animation.setDuration(1000);
                animation.start();
            };

            @Override
            public void onClick(View v) {
                if (mShowQuickSettings) {
                    mBinding.quickSettings.postDelayed(mFabFade, 5000);
                    mBinding.quickSettingsName.hide();
                    mBinding.quickSettingsChannel.hide();
                } else {
                    mBinding.quickSettings.removeCallbacks(mFabFade);
                    mBinding.quickSettings.setAlpha(1f);
                    mBinding.quickSettingsName.show();
                    mBinding.quickSettingsChannel.show();
                }
                mShowQuickSettings = !mShowQuickSettings;
            }
        });

        var messageBox = mBinding.messageBox;
        mMessageAdapter = new MessageAdapter(view.getContext(), new ArrayList<>(), mBinding.mathPreload);
        messageBox.setAdapter(mMessageAdapter);
        messageBox.setOnScrollListener(this);
        messageBox.setOnItemClickListener((parent, v, position, id) -> setChecked(position, false));
        messageBox.setOnItemLongClickListener((parent, v, position, id) -> {
            if (!messageBox.isItemChecked(position)) {
                int checked = messageBox.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(position, true);
                return true;
            } else {
                return false;
            }
        });
        mInitMessages = new ArrayList<>(100);

        mBinding.scrollDownButton.setOnClickListener(v -> scrollDown());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        mRefreshButton = menu.findItem(R.id.chat_refresh);
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
     * Reconnects to the chat web socket
     */
    private void reload() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.returnAltToolbar();
        }

        mDisposable.clear();

        assert activity != null;

        mInitMessages.clear();
        mMessageAdapter.clear();
        mInitDone = false;
        mBinding.progressBar.setVisibility(View.VISIBLE);
        mBinding.messageBox.setVisibility(View.GONE);

        mWebSocket = new ChatWebSocket(Preferences.chat().getChannel());
        mDisposable.addAll(
                Observable.create(mWebSocket)
                          .subscribeOn(Schedulers.io())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(
                                  msg -> {
                                      addPost(msg, mInitDone);
                                      if (mLastPostId < msg.getId()) mLastPostId = msg.getId();
                                  },
                                  err -> error(getString(Reason.guess(err).getStringRes())),
                                  () -> error(getString(R.string.chat_websocket_closed))
                          ),
                Disposable.fromRunnable(() -> mWebSocket = null)
        );

        mBinding.editTextMessage.setEnabled(true);
        ViewUtils.setError(mBinding.editTextMessage, false);
        mBinding.buttonSend.setEnabled(true);

        mMessageAdapter.notifyDataSetChanged();

        if (mRefreshButton != null) {
            mRefreshButton.setEnabled(true);
        }
    }

    private void send() {
        send(false);
    }

    /**
     * Sends a chat post to the server
     * The content of the post comes from the {@code EditText} and various preferences.
     *
     * @param force if this is false the user will be prompted before sending empty posts
     */
    private void send(boolean force) {
        assert getContext() != null;

        String message = mBinding.editTextMessage.getText().toString();

        if (!force && message.trim().isEmpty()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setMessage(R.string.chat_empty_message);
            dialogBuilder.setPositiveButton(R.string.yes, (d, which) -> {
                send(true);
                d.dismiss();
            });
            dialogBuilder.setNegativeButton(R.string.no, (d, which) -> d.cancel());
            dialogBuilder.show();
            return;
        }

        boolean success = mWebSocket.send(
                Preferences.chat().getName(),
                message,
                Preferences.chat().isPublicId()
        );
        if (success) {
            ViewUtils.setError(mBinding.editTextMessage, false);
            mBinding.editTextMessage.setText("");
            mBinding.editTextMessage.requestFocus();
        } else {
            ViewUtils.setError(mBinding.editTextMessage, true);
        }
    }

    /**
     * Appends the given message to the list. The post will also be written to the chat database
     *
     * @param message the message to be appended
     * @param notify if {@link MessageAdapter#notifyDataSetChanged()} should be called
     */
    private void addPost(@NonNull Message message, boolean notify) {
        if (getContext() == null) return;

        // insert recent messages in bulk
        if (Message.PONG.equals(message)) {
            if (!mInitDone) {
                mInitDone = true;

                mMessageAdapter.clear();
                mMessageAdapter.addAll(mInitMessages);
                mMessageAdapter.notifyDataSetChanged();

                mBinding.messageBox.setSelection(mInitMessages.size() - 1);

                //noinspection ResultOfMethodCallIgnored
                Database.getInstance(requireContext()).messageDao().insert(mInitMessages)
                        .doFinally(() -> mInitMessages.clear())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                () -> {},
                                e -> Log.e(LOG_TAG, "Error inserting messages into database.", e)
                        );

                mBinding.progressBar.setVisibility(View.GONE);
                mBinding.messageBox.setVisibility(View.VISIBLE);
            }

            return;
        }

        if (!mInitDone) {
            mInitMessages.add(message);
            return;
        }

        //noinspection ResultOfMethodCallIgnored
        Database.getInstance(requireContext()).messageDao().insert(message)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, e -> Log.e(LOG_TAG, "Error inserting message into database.", e));

        if (message.getId() < mTopPosition) mTopPosition = message.getId();
        if (message.getBottag() == 1 && Preferences.chat().isSense()) return;

        mHandler.post(() -> {
            mMessageAdapter.add(message);
            if (notify) mMessageAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount - visibleItemCount - firstVisibleItem > 0) mBinding.scrollDownButton.show();
        else mBinding.scrollDownButton.hide();
    }

    @Override
    public void revokeAltToolbar() {
        int checked = mBinding.messageBox.getCheckedItemPosition();
        if (checked != -1) setChecked(checked, false);
    }

    //<editor-fold desc="Lifecycle" defaultstate="collapsed">
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        reload();
    }

    @Override
    public void onStop() {
        mDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mDisposable.clear();
        super.onDestroy();
    }
    //</editor-fold>

    //<editor-fold desc="Network Listener" defaultstate="collapsed">
    @Override
    public void onConnectionFail() {
        // network connection fails are already detected via websocket failure
    }

    @Override
    public void onConnectionRegain() {
        reload();
    }
    //</editor-fold>

    //<editor-fold desc="Utility Methods" defaultstate="collapsed">
    /**
     * Sets the checked item in the message list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     * @param value if the item is checked or not
     */
    private void setChecked(int position, boolean value) {
        MessageUtils.setChecked(this, mBinding.messageBox, mMessageAdapter, position, value);
    }

    private void scrollDown() {
        mBinding.messageBox.smoothScrollToPositionFromTop(mMessageAdapter.getCount(), 0, 250);
    }

    /**
     * Appends a chat post that resembles an error with the given error message
     *
     * @param message a meaningful error message
     */
    private void error(String message) {
        mDisposable.clear();
        mInitDone = true;

        addPost(new Message(Integer.MAX_VALUE, "Error", message, Calendar.getInstance().getTime(),503,"Error","220000", 0, ""), true);

        mBinding.editTextMessage.setEnabled(false);
        mBinding.buttonSend.setEnabled(false);
        mBinding.progressBar.setVisibility(View.GONE);
        mBinding.messageBox.setVisibility(View.VISIBLE);
    }
    //</editor-fold>
}
