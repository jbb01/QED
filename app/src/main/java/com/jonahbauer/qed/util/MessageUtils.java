package com.jonahbauer.qed.util;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.networking.NetworkConstants;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageUtils {
    private static final String COPY_FORMAT = "%3$s\t%4$s\t%1$s:\t%2$s";
    private static final DateTimeFormatter COPY_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final String LOG_TAG = MessageUtils.class.getName();

    /**
     * Sets the checked item in the list and shows an appropriate toolbar.
     *
     * @param fragment a fragment for context
     * @param listView the list view
     * @param adapter the list adapter
     * @param infoCallback a callback for when the action mode's info button is pressed
     * @param replyCallback a callback for when the action mode's reply button is pressed
     * @param position the position of the checked item in the {@link MessageAdapter}
     * @param value if the item is checked or not
     */
    public static void setChecked(@NonNull Fragment fragment,
                                  @NonNull ListView listView,
                                  @NonNull MessageAdapter adapter,
                                  @Nullable BiConsumer<ActionMode, Message> infoCallback,
                                  @Nullable BiConsumer<ActionMode, Message> replyCallback,
                                  int position, boolean value) {
        listView.setItemChecked(position, value);

        var activity = fragment.getActivity();
        if (activity instanceof AppCompatActivity) {
            var appCompatActivity = (AppCompatActivity) activity;

            if (value) {
                listView.playSoundEffect(SoundEffectConstants.CLICK);
                Message msg = adapter.getItem(position);
                if (msg == null) return;

                var actionModeCallBack = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        mode.getMenuInflater().inflate(R.menu.menu_message, menu);

                        var reply = menu.findItem(R.id.message_reply);
                        if (reply != null) reply.setVisible(replyCallback != null);

                        var info = menu.findItem(R.id.message_info);
                        if (info != null) info.setVisible(infoCallback != null);

                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        if (item.getItemId() == R.id.message_info) {
                            if (infoCallback != null) {
                                infoCallback.accept(mode, msg);
                                return true;
                            }
                        } else if (item.getItemId() == R.id.message_copy) {
                            Actions.copy(fragment.requireContext(), listView, msg.getName(), msg.getMessage());
                            return true;
                        } else if (item.getItemId() == R.id.message_reply) {
                            if (replyCallback != null) {
                                replyCallback.accept(mode, msg);
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        listView.setItemChecked(position, false);
                    }
                };

                var actionMode = appCompatActivity.startSupportActionMode(actionModeCallBack);
                if (actionMode == null) {
                    Log.e(LOG_TAG, "Unexpected null value for action mode");
                } else {
                    if (msg.isAnonymous()) {
                        actionMode.setTitle(R.string.message_name_anonymous);
                    } else {
                        actionMode.setTitle(msg.getName());
                    }
                }
            } else if (appCompatActivity instanceof MainActivity) {
                ((MainActivity) appCompatActivity).finishActionMode();
            }
        }
    }

    /**
     * The messages sent from the server contain only {@link java.time.LocalDateTime} information.
     * When trying to convert {@code LocalDateTime} to {@link java.time.Instant} on the day of
     * setting the clocks back both, the period from 02:00 CEST to 03:00 CEST and 02:00 to 03:00 CET
     * get mapped to 00:00 to 01:00 UTC.
     * Without further context this problem cannot be solved, but since the messages are ordered by
     * their id it is possible in most cases to find out which messages was sent from 02:00 to
     * 03:00 CEST and which from 02:00 to 03:00 CET.
     * <br>
     * The {@link Function} returned by this method keeps track of the most recent received timestamp
     * and shifts messages that are determined to be mistakenly assigned CEST one hour into the future.
     * <br>
     * For this {@code Function} to work, it must be applied to all the messages in the order of
     * their {@code id}.
     */
    public static Function<Message, Message> dateFixer() {
        // usually the timestamp should be increasing with increasing id
        // only when there is a local time overlap or a race condition or someone messed with the
        // database, can there be a higher timestamp before any message
        return new Function<>() {
            long max = Long.MIN_VALUE;

            @Override
            public Message apply(Message message) {
                long timestamp = message.getDate().getEpochSecond();

                max = Math.max(max, timestamp);
                if (max > timestamp) {
                    // to prevent detection of race conditions only mark messages within the correct
                    // time frame as needing a fix
                    // the timeframe is on the first sunday after (or on) october 25th of each year
                    // from 00:00:00 to 00:59:59 UTC
                    OffsetDateTime dateTime = OffsetDateTime.ofInstant(message.getDate(), ZoneId.of("UTC"));
                    if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY
                            && dateTime.getMonth() == Month.OCTOBER
                            && dateTime.getDayOfMonth() >= 25
                            && dateTime.getHour() == 0) {
                        return new Message(
                                message.getId(),
                                message.getName(),
                                message.getMessage(),
                                message.getDate().plusSeconds(3600),
                                message.getUserId(),
                                message.getUserName(),
                                message.getColor(),
                                message.getChannel(),
                                message.getBottag()
                        );
                    }
                }

                return message;
            }
        };
    }

    /**
     * Returns a string that mimics what one would get when trying to copy a message on the
     * browser chat client.
     */
    public static String copyFormat(Message message) {
        return String.format(
                Locale.GERMANY,
                COPY_FORMAT,
                message.getName(),
                message.getMessage(),
                ZonedDateTime.ofInstant(message.getDate(), NetworkConstants.SERVER_TIME_ZONE).format(COPY_TIME_FORMAT),
                message.getUserName() != null ? "✓" : ""
        );
    }
}
