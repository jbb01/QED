package com.jonahbauer.qed.activities.mainFragments;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.*;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentChatBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.viewmodel.ChatViewModel;
import com.jonahbauer.qed.util.*;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import static com.jonahbauer.qed.util.MessageUtils.formatName;
import static com.jonahbauer.qed.util.MessageUtils.isMainChannel;

public class ChatFragment extends Fragment implements AbsListView.OnScrollListener {
    private ChatViewModel mChatViewModel;
    private FragmentChatBinding mBinding;

    private MessageAdapter mMessageAdapter;
    private boolean mQuickSettingsShown = false;

    @Nullable
    private MenuItem mRefreshButton;

    private @NonNull Disposable mDisposable = Disposable.disposed();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentChatBinding.inflate(inflater, container, false);
        mChatViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_chat).get(ChatViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.buttonSend.setOnClickListener(v -> send());

        // setup quick settings
        mBinding.quickSettingsName.setOnClickListener(v -> {
            ViewUtils.showPreferenceDialog(
                    requireContext(),
                    R.string.preferences_chat_name_title,
                    () -> Preferences.getChat().getName(),
                    (str) -> Preferences.getChat().setName(str)
            );
        });
        mBinding.quickSettingsChannel.setOnClickListener(v -> {
            ViewUtils.showPreferenceDialog(
                    requireContext(),
                    R.string.preferences_chat_channel_title,
                    () -> Preferences.getChat().getChannel(),
                    (str) -> Preferences.getChat().setChannel(str)
            );
        });
        mBinding.quickSettings.setOnClickListener(this::toggleQuickSettingsShown);
        setQuickSettingsShown(mQuickSettingsShown);

        mMessageAdapter = new MessageAdapter(mBinding.list, mBinding.mathPreload);
        mBinding.list.setAdapter(mMessageAdapter);
        mBinding.list.setOnScrollListener(this);
        mBinding.list.setOnItemClickListener((parent, v, position, id) -> {
            setCheckedItem(MessageAdapter.INVALID_POSITION);
        });
        mBinding.list.setOnItemLongClickListener((parent, v, position, id) -> {
            setCheckedItem(position);
            return true;
        });

        mBinding.scrollDownButton.setOnClickListener(v -> smoothScrollDown());

        mChatViewModel.getMessageRX().observe(getViewLifecycleOwner(), observable -> {
            if (observable != null) {
                reloadView(observable);
            }
        });
        mChatViewModel.getReady().observe(getViewLifecycleOwner(), mBinding::setReady);
        mChatViewModel.getChannel().observe(getViewLifecycleOwner(), channel -> {
            if (isMainChannel(channel)) {
                ViewUtils.setActionBarText(this, getString(R.string.title_fragment_chat));
            } else {
                ViewUtils.setActionBarText(this, getString(R.string.title_fragment_chat_with_channel, channel));
            }
        });

        mChatViewModel.getName().observe(getViewLifecycleOwner(), name -> {
            var formattedName = formatName(requireContext(), name);
            var builder = new SpannableStringBuilder();
            builder.append(getString(R.string.chat_message_hint));
            builder.append(' ');
            builder.append(formattedName);
            mBinding.setHint(builder);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mChatViewModel.isOpen()) {
            reconnect();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposable.dispose();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        mRefreshButton = menu.findItem(R.id.menu_refresh);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mRefreshButton = item;
            mRefreshButton.setEnabled(false);

            Drawable icon = mRefreshButton.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            reconnect();
            return true;
        }
        return false;
    }

    /**
     * Reconnects to the chat web socket
     */
    private void reconnect() {
        mChatViewModel.disconnect();
        mChatViewModel.connect();
    }

    /**
     * Reloads the view and reconnect to the view model without affecting the underlying websocket connection
     */
    private void reloadView(Observable<Message> messageRx) {
        setCheckedItem(MessageAdapter.INVALID_POSITION);
        mMessageAdapter.clear();
        mMessageAdapter.notifyDataSetChanged();
        if (mRefreshButton != null) mRefreshButton.setEnabled(true);
        ViewUtils.setError(mBinding.messageInput, false);
        mDisposable.dispose();

        mBinding.setLoading(true);
        mDisposable = messageRx
                .filter(message -> !message.isBot() || !Preferences.getChat().isSense())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message -> {
                    var oldCount = mMessageAdapter.getCount();

                    switch (message.getType()) {
                        case POST: {
                            mMessageAdapter.add(message);
                            if (!mBinding.getLoading()) {
                                mMessageAdapter.notifyDataSetChanged();
                                maybeScrollDown(oldCount);
                            }
                            break;
                        }
                        case ERROR:
                            mMessageAdapter.add(message);
                            // fall through
                        case PONG: {
                            mBinding.setLoading(false);
                            mMessageAdapter.notifyDataSetChanged();
                            scrollDown();
                            break;
                        }
                    }
                });
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

        String message = mBinding.messageInput.getText().toString();

        if (!force && TextUtils.isNullOrBlank(message)) {
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

        if (mChatViewModel.send(message)) {
            ViewUtils.setError(mBinding.messageInput, false);
            mBinding.messageInput.setText("");
            mBinding.messageInput.requestFocus();
        } else {
            ViewUtils.setError(mBinding.messageInput, true);
        }
    }

    /**
     * Copies the message in {@linkplain MessageUtils#copyFormat(Message) reply format} and inserts
     * it at the beginning of the message input.
     */
    private void reply(@NonNull ActionMode actionMode, @NonNull Message message) {
        var reply = MessageUtils.copyFormat(message);
        var text = mBinding.messageInput.getEditableText();

        if (!text.toString().startsWith(reply)) {
            text.insert(0, reply + "\n\n");
            Snackbar.make(mBinding.list, R.string.message_reply_prepended, Snackbar.LENGTH_SHORT).show();
            actionMode.finish();
        } else {
            Snackbar.make(mBinding.list, R.string.message_reply_present, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void toggleQuickSettingsShown(@Nullable View view) {
        setQuickSettingsShown(!mQuickSettingsShown);
    }

    private void setQuickSettingsShown(boolean shown) {
        mQuickSettingsShown = shown;
        if (shown) {
            mBinding.quickSettingsName.show();
            mBinding.quickSettingsChannel.show();
        } else {
            mBinding.quickSettingsName.hide();
            mBinding.quickSettingsChannel.hide();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount - visibleItemCount - firstVisibleItem > 0) {
            mBinding.scrollDownButton.show();
        } else {
            mBinding.scrollDownButton.hide();
        }
    }

    //<editor-fold desc="Utility Methods" defaultstate="collapsed">
    /**
     * Sets the checked item in the message list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     */
    private void setCheckedItem(int position) {
        MessageUtils.setCheckedItem(
                this,
                mBinding.list,
                mMessageAdapter,
                (mode, msg) -> NavHostFragment.findNavController(this)
                                              .navigate(ChatFragmentDirections.showMessage(msg)),
                this::reply,
                position
        );
    }

    private void smoothScrollDown() {
        mBinding.list.smoothScrollToPositionFromTop(mMessageAdapter.getCount(), 0, 250);
    }

    private void maybeScrollDown(int oldCount) {
        int newCount = mMessageAdapter.getCount();
        mBinding.list.post(() -> {
            if (mBinding.list.getLastVisiblePosition() >= oldCount) {
                mBinding.list.setSelection(newCount - 1);
            }
        });
    }

    private void scrollDown() {
        mBinding.list.setSelection(mMessageAdapter.getCount() - 1);
    }
    //</editor-fold>
}
