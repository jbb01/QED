package com.jonahbauer.qed.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDChatPages;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SameParameterValue")
public class LogFragment extends Fragment implements ChatDatabaseReceiver, QEDPageStreamReceiver {
    private static final String downloadProgressString = Application.getContext().getString(R.string.log_downloading);
    private static final String parseProgressString = Application.getContext().getString(R.string.log_parsing);
    static final String LOG_MODE_KEY = "mode";
    private static final String LOG_CHANNEL_KEY = "channel";
    static final String LOG_LAST_KEY = "last";
    static final String LOG_FROM_KEY = "from";
    static final String LOG_TO_KEY = "to";
    private static final String LOG_SKIP_KEY = "skip";

    private Mode mode;
    private String channel;
    private Map<String, Long> data;

    private String options;
    private AsyncTask logGetter = null;
    private MessageAdapter messageAdapter;

    private ProgressBar searchProgressBar;
    private TableLayout progressTable;
    private ProgressBar saveProgressBar;
    private ListView messageListView;
    private TextView subtitle;
    private TextView downloadProgress;
    private TextView parseProgress;

    private ChatDatabase database;

    static LogFragment newInstance(Mode mode, String channel, Long... data) {
        Bundle args = new Bundle();
        args.putString(LOG_MODE_KEY, mode.toString());
        args.putString(LOG_CHANNEL_KEY, channel);

        switch (mode) {
            case POST_RECENT:
            case DATE_RECENT:
                if (data.length > 0) args.putLong(LOG_LAST_KEY, data[0]);
                break;
            case DATE_INTERVAL:
                if (data.length > 0) args.putLong(LOG_FROM_KEY, data[0]);
                if (data.length > 1) args.putLong(LOG_TO_KEY, data[1]);
                break;
            case SINCE_OWN:
                if (data.length > 0) args.putLong(LOG_SKIP_KEY, data[0]);
        }

        LogFragment fragment = new LogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

//        ImageView background = view.findViewById(R.id.background);
        searchProgressBar = view.findViewById(R.id.search_progress);
        progressTable = view.findViewById(R.id.progress_table);
        saveProgressBar = view.findViewById(R.id.save_progress);
        messageListView = view.findViewById(R.id.log_message_container);
        subtitle = view.findViewById(R.id.log_subtitle);
        downloadProgress = view.findViewById(R.id.progress_download_text);
        parseProgress = view.findViewById(R.id.progress_parse_text);

        subtitle.setOnClickListener((View v) -> {
            Intent intent = new Intent(getActivity(), LogDialogActivity.class);
            startActivityForResult(intent, 0);
        });

//        TileDrawable tileDrawable  = new TileDrawable(view.getContext().getDrawable(R.drawable.background_part), Shader.TileMode.REPEAT);
//        background.setImageDrawable(tileDrawable);

        Bundle arguments = getArguments();
        assert arguments != null;

        mode = Mode.valueOf(arguments.getString(LOG_MODE_KEY));
        String channel = arguments.getString(LOG_CHANNEL_KEY);
        long last = arguments.getLong(LOG_LAST_KEY);
        long from = arguments.getLong(LOG_FROM_KEY);
        long to = arguments.getLong(LOG_TO_KEY);
        long skip = arguments.getLong(LOG_SKIP_KEY);

        data = new HashMap<>();
        if (channel != null) this.channel = channel;

        if (last != 0)
            data.put(LOG_LAST_KEY, last);
        else
            data.put(LOG_LAST_KEY, 200L);

        if (from != 0)
            data.put(LOG_FROM_KEY, from);
        else
            data.put(LOG_FROM_KEY, Calendar.getInstance().getTime().getTime());

        if (to != 0)
            data.put(LOG_TO_KEY, to);
        else
            data.put(LOG_TO_KEY, Calendar.getInstance().getTime().getTime() + 86400000L);

        data.put(LOG_SKIP_KEY, skip);

        messageAdapter = new MessageAdapter(view.getContext(), new ArrayList<>());
        messageListView.setAdapter(messageAdapter);

        messageListView.setPadding(messageListView.getPaddingLeft(),
                messageListView.getPaddingTop(),
                messageListView.getPaddingRight() + messageListView.getVerticalScrollbarWidth(),
                messageListView.getPaddingBottom());

        setHasOptionsMenu(true);

        reload();

        database = new ChatDatabase();
        database.init(view.getContext(), this);

        return view;
    }

    public void reload() {
        downloadProgress.setText(String.format(downloadProgressString, "0"));
        parseProgress.setText(String.format(parseProgressString, "0/0"));
        searchProgressBar.setVisibility(View.VISIBLE);
        progressTable.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);
        String subtitle = "";

        switch (mode) {
            case POST_RECENT: {
                Long last = data.get(LOG_LAST_KEY);
                options = "mode=" + mode.modeStr;
                options += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_recent), last != null ? last : 200);
                break;
            }
            case DATE_RECENT: {
                Long last = data.get(LOG_LAST_KEY);
                options = "mode=" + mode.modeStr;
                options += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_recent), (last != null ? last : 86400) / 3600);
                break;
            }
            case DATE_INTERVAL: {
                Long from = data.get(LOG_FROM_KEY);
                Long to = data.get(LOG_TO_KEY);

                Date dateFrom = new Date(from != null ? from : Calendar.getInstance().getTime().getTime());
                Date dateTo = new Date(to != null ? to : (Calendar.getInstance().getTime().getTime() + 86_400_000));

                options = "mode=" + mode.modeStr;
                options += "&from=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateFrom);
                options += "&to=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateTo);

                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_interval), dateFrom, dateTo);
                break;
            }
        }

        options += "&channel=" + channel;

        this.subtitle.setText(subtitle);

        messageAdapter.clear();

        logGetter = QEDChatPages.getChatLog(getClass().toString(), options, messageAdapter.getData(), this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            mode = Mode.valueOf(data.getStringExtra(LOG_MODE_KEY));
            this.data.put(LOG_LAST_KEY, data.getLongExtra(LOG_LAST_KEY, 0));
            this.data.put(LOG_FROM_KEY, data.getLongExtra(LOG_FROM_KEY, 0));
            this.data.put(LOG_TO_KEY, data.getLongExtra(LOG_TO_KEY, 0));
            this.data.put(LOG_SKIP_KEY, data.getLongExtra(LOG_SKIP_KEY, 0));

            if (logGetter != null) logGetter.cancel(true);
            logGetter = null;
            reload();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.log_save) {
            database.insertAll(messageAdapter.getData());
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        database.close();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (logGetter!=null) logGetter.cancel(true);
    }

    @Override
    public void onPageReceived(String tag) {
        Log.d(Application.LOG_TAG_DEBUG, tag + ": onPageReceived");
        messageAdapter.notifyDataSetChanged();
        searchProgressBar.setVisibility(View.GONE);
        progressTable.setVisibility(View.GONE);
        messageListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNetworkError(String tag) {
        searchProgressBar.post(() -> {
            searchProgressBar.setVisibility(View.GONE);
            progressTable.setVisibility(View.GONE);
            Toast.makeText(getContext(), R.string.error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onStreamError(String tag) {
        onNetworkError(tag);
    }

    @Override
    public void onProgressUpdate(String tag, long done, long total) {
        if (total == Integer.MIN_VALUE && done == Integer.MIN_VALUE) {
            Log.d(Application.LOG_TAG_DEBUG, tag + ": download complete");
        } else if (total == Integer.MIN_VALUE) {
            String mib = String.valueOf(done / 1_048_576);
            String dot = String.valueOf((done % 1_048_576) / 1_048_576d).substring(2, 3);
            if (dot.equals("")) dot = "0";
            downloadProgress.setText(String.format(downloadProgressString, mib + "." + dot));

            Log.d(Application.LOG_TAG_DEBUG, tag + ": downloading: " + done / 1_048_576 + "MiB");
        } else {
            String parseProgressString = String.format(LogFragment.parseProgressString, done + "/" + total);
            parseProgress.setText(parseProgressString);
            Log.d(Application.LOG_TAG_DEBUG, tag + ": converting " + parseProgressString);
        }
    }

    @Override
    public void onReceiveResult(List<Message> messages) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {
        if (done < total) {
            saveProgressBar.setVisibility(View.VISIBLE);
            saveProgressBar.setProgress(done * 100 / total, true);
        } else {
            saveProgressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), getString(R.string.database_save_done), Toast.LENGTH_SHORT).show();
        }
    }

    public enum Mode {
        POST_RECENT("postrecent"), DATE_RECENT("daterecent"), DATE_INTERVAL("dateinterval"), POST_INTERVAL("postinterval"), SINCE_OWN("fromownpost");

        public String modeStr;

        Mode(String modeStr) {
            this.modeStr = modeStr;
        }
    }
}
