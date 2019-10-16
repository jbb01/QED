package com.jonahbauer.qed.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.icu.util.TimeZone;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.jonahbauer.qed.activities.LogFragment.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.activities.LogFragment.Mode.DATE_RECENT;
import static com.jonahbauer.qed.activities.LogFragment.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.activities.LogFragment.Mode.POST_RECENT;
import static com.jonahbauer.qed.activities.LogFragment.Mode.SINCE_OWN;

@SuppressWarnings("SameParameterValue")
public class LogFragment extends Fragment implements ChatDatabaseReceiver, QEDPageStreamReceiver {
    private static final String downloadProgressString = Application.getContext().getString(R.string.log_downloading);
    private static final String parseProgressString = Application.getContext().getString(R.string.log_parsing);
    private static final String LOG_MODE_KEY = "mode";
    private static final String LOG_CHANNEL_KEY = "channel";
    private static final String LOG_LAST_KEY = "last";
    private static final String LOG_FROM_KEY = "from";
    private static final String LOG_TO_KEY = "to";
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

        searchProgressBar = view.findViewById(R.id.search_progress);
        progressTable = view.findViewById(R.id.progress_table);
        saveProgressBar = view.findViewById(R.id.save_progress);
        messageListView = view.findViewById(R.id.log_message_container);
        subtitle = view.findViewById(R.id.log_subtitle);
        downloadProgress = view.findViewById(R.id.progress_download_text);
        parseProgress = view.findViewById(R.id.progress_parse_text);

        Context context = getActivity();

        subtitle.setOnClickListener((View v) -> showDialog());

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

        messageAdapter = new MessageAdapter(context, new ArrayList<>());
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
        reload(null, null, null);
    }
    public void reload(Mode mode, String channel, Map<String,Long> data) {
        if (data != null) {
            this.data = data;
        }
        if (mode != null) this.mode = mode;
        if (channel != null) this.channel = channel;

        downloadProgress.setText(String.format(downloadProgressString, "0"));
        parseProgress.setText(String.format(parseProgressString, "0/0"));
        searchProgressBar.setVisibility(View.VISIBLE);
        progressTable.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);
        String subtitle = "";

        switch (this.mode) {
            case POST_RECENT: {
                Long last = this.data.get(LOG_LAST_KEY);
                options = "mode=" + this.mode.modeStr;
                options += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_recent), last != null ? last : 200);
                break;
            }
            case DATE_RECENT: {
                Long last = this.data.get(LOG_LAST_KEY);
                options = "mode=" + this.mode.modeStr;
                options += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_recent), (last != null ? last : 86400) / 3600);
                break;
            }
            case DATE_INTERVAL: {
                Long from = this.data.get(LOG_FROM_KEY);
                Long to = this.data.get(LOG_TO_KEY);

                Date dateFrom = new Date(from != null ? from : Calendar.getInstance().getTime().getTime());
                Date dateTo = new Date(to != null ? to : (Calendar.getInstance().getTime().getTime() + 86_400_000));

                options = "mode=" + this.mode.modeStr;
                options += "&from=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateFrom);
                options += "&to=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateTo);

                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_interval), dateFrom, dateTo);
                break;
            }
        }

        options += "&channel=" + this.channel;

        this.subtitle.setText(subtitle);

        messageAdapter.clear();

        if (logGetter != null) logGetter.cancel(true);

        logGetter = QEDChatPages.getChatLog(getClass().toString(), options, messageAdapter.getData(), this);
    }

    private void showDialog() {
        Context context = getContext();
        if (context == null) return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setTitle(R.string.title_activity_log_dialog);

        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.alert_dialog_log_mode, null);

        Map<String, Long> data = new HashMap<>();
        AtomicReference<Mode> mode = new AtomicReference<>();
        AtomicReference<Date> dateStart = new AtomicReference<>();
        AtomicReference<Date> dateEnd = new AtomicReference<>();
        String channel = "";

        Date date = android.icu.util.Calendar.getInstance(TimeZone.getDefault()).getTime();
        dateStart.set(date);
        dateEnd.set(date);

        View fragmentPostRecent = view1.findViewById(R.id.log_fragment_postrecent);
        View fragmentDateRecent = view1.findViewById(R.id.log_fragment_daterecent);
        View fragmentDateInterval = view1.findViewById(R.id.log_fragment_dateinterval);

        EditText postRecentLast = view1.findViewById(R.id.log_dialog_postrecent_editText);
        EditText dateRecentLast = view1.findViewById(R.id.log_dialog_daterecent_editText);
        EditText dateIntervalStart = view1.findViewById(R.id.log_dialog_dateinterval_editText_from);
        EditText dateIntervalEnd = view1.findViewById(R.id.log_dialog_dateinterval_editText_to);


        DatePickerDialog.OnDateSetListener dateSetListener = (view2, year, month, dayOfMonth) -> {
            android.icu.util.Calendar cal = android.icu.util.Calendar.getInstance();
            cal.set(android.icu.util.Calendar.YEAR,year);
            cal.set(android.icu.util.Calendar.MONTH,month);
            cal.set(android.icu.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            Date dateTemp = cal.getTime();
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            if (view2.getId() == R.id.log_dialog_datepicker_start) {
                dateStart.set(dateTemp);
                dateIntervalStart.setText(dateFormat.format(dateTemp));
            } else if (view2.getId() == R.id.log_dialog_datepicker_end) {
                dateEnd.set(dateTemp);
                dateIntervalEnd.setText(dateFormat.format(dateTemp));
            }
        };

        View.OnClickListener dateEditTextClickListener = v1 -> {
            android.icu.util.Calendar calendar = android.icu.util.Calendar.getInstance(TimeZone.getDefault());

            DatePickerDialog dialog = new DatePickerDialog(context, R.style.AppTheme_Dialog, dateSetListener,
                    calendar.get(android.icu.util.Calendar.YEAR), calendar.get(android.icu.util.Calendar.MONTH),
                    calendar.get(android.icu.util.Calendar.DAY_OF_MONTH));

            dialog.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.ok), (dialog1, which) -> {
                DatePicker datePicker = dialog.getDatePicker();
                dateSetListener.onDateSet(datePicker, datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            });
            dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (android.os.Message) null);

            dialog.show();

            if (v1.equals(dateIntervalStart)) {
                dialog.getDatePicker().setId(R.id.log_dialog_datepicker_start);
            } else if (v1.equals(dateIntervalEnd)) {
                dialog.getDatePicker().setId(R.id.log_dialog_datepicker_end);
            }
        };

        dateIntervalStart.setOnClickListener(dateEditTextClickListener);
        dateIntervalEnd.setOnClickListener(dateEditTextClickListener);


        Spinner spinner = view1.findViewById(R.id.log_dialog_mode_spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getItemAtPosition(position);

                if (POST_RECENT.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.VISIBLE);
                    fragmentDateRecent.setVisibility(View.GONE);
                    fragmentDateInterval.setVisibility(View.GONE);
                    mode.set(POST_RECENT);
                } else if (DATE_RECENT.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.VISIBLE);
                    fragmentDateInterval.setVisibility(View.GONE);
                    mode.set(DATE_RECENT);
                } else if (DATE_INTERVAL.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.GONE);
                    fragmentDateInterval.setVisibility(View.VISIBLE);
                    mode.set(DATE_INTERVAL);
                } else if (POST_INTERVAL.modeStr.equals(item)) {
                    // TODO fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
                    mode.set(POST_INTERVAL);
                } else if (SINCE_OWN.modeStr.equals(item)) {
                    // TODO fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
                    mode.set(SINCE_OWN);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        alertDialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {
            switch (mode.get()) {
                case POST_RECENT:
                    data.put(LOG_LAST_KEY, Long.valueOf(postRecentLast.getText().toString().trim()));
                    break;
                case DATE_RECENT:
                    data.put(LOG_LAST_KEY, 3600 * Long.valueOf(dateRecentLast.getText().toString().trim()));
                    break;
                case DATE_INTERVAL:
                    data.put(LOG_FROM_KEY, dateStart.get().getTime());
                    data.put(LOG_TO_KEY, dateEnd.get().getTime());
                    break;
            }

            reload(mode.get(), channel, data);
            dialog.dismiss();
        });

        alertDialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        alertDialogBuilder.setView(view1);

        alertDialogBuilder.show();
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
