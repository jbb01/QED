package com.jonahbauer.qed.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Shader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.layoutStuff.TileDrawable;
import com.jonahbauer.qed.logs.LogGetter;
import com.jonahbauer.qed.logs.LogReceiver;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogFragment extends Fragment implements LogReceiver, ChatDatabaseReceiver {
    public static final String LOG_MODE_KEY = "mode";
    public static final String LOG_CHANNEL = "channel";
    public static final String LOG_LAST = "last";
    public static final String LOG_FROM = "from";
    public static final String LOG_TO = "to";

    public static final String LOG_MODE_RECENT_POSTS = "postrecent";
    public static final String LOG_MODE_RECENT_DATE = "daterecent";
    public static final String LOG_MODE_INTERVAL_DATE = "dateinterval";

    public static final Map<String, String> mode = new HashMap<>();

    private String options;
    private LogGetter logGetter = null;
    private MessageAdapter messageAdapter;

    private ProgressBar searchProgressBar;
    private ProgressBar saveProgressBar;
    private ListView messageListView;
    private TextView subtitle;

    private ChatDatabase database;

    public static LogFragment newInstance(String mode, String channel, String... data) {
        Bundle args = new Bundle();
        args.putString(LOG_MODE_KEY, mode);
        args.putString(LOG_CHANNEL, channel);

        switch (mode) {
            case LOG_MODE_RECENT_POSTS:
                if (data.length > 0) args.putString(LOG_LAST, data[0]);
                break;
            case LOG_MODE_RECENT_DATE:
                if (data.length > 0) args.putString(LOG_LAST, data[0]);
                break;
            case LOG_MODE_INTERVAL_DATE:
                if (data.length > 0) args.putString(LOG_FROM, data[0]);
                if (data.length > 1) args.putString(LOG_TO, data[1]);
                break;
        }

        LogFragment fragment = new LogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        ImageView background = view.findViewById(R.id.background);
        searchProgressBar = view.findViewById(R.id.search_progress);
        saveProgressBar = view.findViewById(R.id.save_progress);
        messageListView = view.findViewById(R.id.log_message_container);
        subtitle = view.findViewById(R.id.log_subtitle);

        subtitle.setOnClickListener((View v) -> {
            Intent intent = new Intent(getActivity(), LogDialogActivity.class);
            startActivityForResult(intent, 0);
        });

        TileDrawable tileDrawable  = new TileDrawable(view.getContext().getDrawable(R.drawable.background_part), Shader.TileMode.REPEAT);
        background.setImageDrawable(tileDrawable);

        Bundle arguments = getArguments();
        assert arguments != null;

        mode.put(LOG_MODE_KEY, arguments.getString(LOG_MODE_KEY));
        mode.put(LOG_LAST, arguments.getString(LOG_LAST));
        mode.put(LOG_FROM, arguments.getString(LOG_FROM));
        mode.put(LOG_TO, arguments.getString(LOG_TO));
        mode.put(LOG_CHANNEL, arguments.getString(LOG_CHANNEL));

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
        searchProgressBar.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);
        String subtitle = "";
        try {
            switch (mode.get(LOG_MODE_KEY)) {
                case LOG_MODE_RECENT_POSTS:
                    if (mode.get(LOG_LAST) == null || mode.get(LOG_LAST).trim().equals("")) mode.put(LOG_LAST, "1");
                    options = "mode=" + LOG_MODE_RECENT_POSTS;
                    options += "&last=" + mode.get(LOG_LAST);
                    subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_recent), Integer.valueOf(mode.get(LOG_LAST)));
                    break;
                case LOG_MODE_RECENT_DATE:
                    if (mode.get(LOG_LAST) == null || mode.get(LOG_LAST).trim().equals("")) mode.put(LOG_LAST, "1");
                    options = "mode=" + LOG_MODE_RECENT_DATE;
                    options += "&last=" + mode.get(LOG_LAST);
                    subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_recent), Integer.valueOf(mode.get(LOG_LAST)) / 3600);
                    break;
                case LOG_MODE_INTERVAL_DATE:
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

                    Date dateFrom = dateFormat.parse(mode.get(LOG_FROM));
                    Date dateTo = dateFormat.parse(mode.get(LOG_TO));

                    options = "mode=" + LOG_MODE_INTERVAL_DATE;
                    options += "&from=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateFrom);
                    options += "&to=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateTo);

                    subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_interval), dateFrom, dateTo);
                    break;
            }
            options += "&channel=" + mode.get(LOG_CHANNEL);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.subtitle.setText(subtitle);

        messageAdapter.clear();

        if (logGetter == null)
            logGetter = new LogGetter();
        logGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,options, this, getContext());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            mode.put(LOG_MODE_KEY, data.getStringExtra(LOG_MODE_KEY));
            mode.put(LOG_LAST, data.getStringExtra(LOG_LAST));
            mode.put(LOG_FROM, data.getStringExtra(LOG_FROM));
            mode.put(LOG_TO, data.getStringExtra(LOG_TO));
            logGetter.cancel(true);
            logGetter = null;
            reload();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_save:
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
    public void onReceiveLogs(List<Message> messages) {
        if (messages != null) {
            messageAdapter.addAll(messages);
            messageAdapter.notifyDataSetChanged();
            searchProgressBar.setVisibility(View.GONE);
            messageListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLogError() {
        searchProgressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), R.string.log_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onOutOfMemory() {
        Toast.makeText(getContext(), R.string.log_oom, Toast.LENGTH_LONG).show();

        searchProgressBar.setVisibility(View.GONE);
        messageListView.setVisibility(View.VISIBLE);
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
}
