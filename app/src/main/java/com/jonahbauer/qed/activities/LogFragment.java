package com.jonahbauer.qed.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDChatPages;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;
import com.jonahbauer.qed.networking.downloadManager.Download;
import com.jonahbauer.qed.networking.downloadManager.DownloadBroadcastReceiver;
import com.jonahbauer.qed.networking.downloadManager.DownloadListener;

import java.io.File;
import java.lang.ref.SoftReference;
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
public class LogFragment extends QEDFragment implements ChatDatabaseReceiver, QEDPageStreamReceiver {
    private static final String downloadProgressString;
    private static final String downloadPendingString;
    private static final String downloadPausedString;
    private static final String downloadSuccessfulString;
    private static final String downloadFailedString;
    private static final String parsePendingString;
    private static final String parseProgressString;
    private static final String parseFailedString;

    private static final String LOG_LAST_KEY = "last";
    private static final String LOG_FROM_KEY = "from";
    private static final String LOG_TO_KEY = "to";
    private static final String LOG_SKIP_KEY = "skip";
    private static final String LOG_DOWNLOAD_ID_KEY = "downloadId";

    static {
        SoftReference<Application> appRef = Application.getApplicationReference();
        Application application = appRef.get();
        if (application != null) {
            downloadProgressString = application.getString(R.string.log_downloading);
            downloadPendingString = application.getString(R.string.log_download_pending);
            downloadPausedString = application.getString(R.string.log_download_paused);
            downloadSuccessfulString = application.getString(R.string.log_download_successful);
            downloadFailedString = application.getString(R.string.log_download_failed);
            parseProgressString = application.getString(R.string.log_parsing);
            parsePendingString = application.getString(R.string.log_parse_pending);
            parseFailedString = application.getString(R.string.log_parse_failed);
        } else {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            downloadProgressString = "Error";
            parseProgressString = "Error";
            parsePendingString = "Error";
            parseFailedString = "Error";
            downloadPendingString = "Error";
            downloadPausedString = "Error";
            downloadFailedString = "Error";
            downloadSuccessfulString = "Error";
        }
    }

    private SharedPreferences sharedPreferences;

    private Mode mode;
    private String channel;
    private Map<String, Long> data;

    private String pathData;
    private EditText pathEditText;

    private String options;
    private Download logDownload = null;
    private MessageAdapter messageAdapter;

    private ProgressBar saveProgressBar;
    private ListView messageListView;
    private TextView subtitle;
    private TextView labelError;

    private TableLayout progressTable;
    private TextView[] progressText;
    private ProgressBar[] progressBars;
    private ImageView[] progressIcons;


    private ChatDatabase database;

    private AlertDialog dropAlertDialog;
    private QEDChatPages.LogDownloadListener downloadListener;

    LogFragment(@NonNull Mode mode, String channel, Long... data) {
        this.data = new HashMap<>();

        this.mode = mode;
        this.channel = channel;

        switch (mode) {
            case POST_RECENT:
            case DATE_RECENT:

                if (data.length > 0) this.data.put(LOG_LAST_KEY, data[0]);
                else this.data.put(LOG_LAST_KEY, 200L);

                break;
            case DATE_INTERVAL:
                if (data.length > 0) this.data.put(LOG_FROM_KEY, data[0]);
                else this.data.put(LOG_FROM_KEY, Calendar.getInstance().getTime().getTime());

                if (data.length > 1) this.data.put(LOG_TO_KEY, data[1]);
                else this.data.put(LOG_TO_KEY, Calendar.getInstance().getTime().getTime() + 86400000L); // now + 1 day

                break;
            case POST_INTERVAL:
                if (data.length > 0) this.data.put(LOG_FROM_KEY, data[0]);
                else this.data.put(LOG_FROM_KEY, Long.MAX_VALUE);

                if (data.length > 1) this.data.put(LOG_TO_KEY, data[1]);
                else this.data.put(LOG_TO_KEY, 0L);
                break;
            case SINCE_OWN:
                if (data.length > 0) this.data.put(LOG_SKIP_KEY, data[0]);
                else this.data.put(LOG_SKIP_KEY, 50L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        saveProgressBar = view.findViewById(R.id.save_progress);
        messageListView = view.findViewById(R.id.log_message_container);
        subtitle = view.findViewById(R.id.log_fragment_subtitle);
        labelError = view.findViewById(R.id.label_offline);

        progressTable = view.findViewById(R.id.log_fragment_progress_table);
        progressText = new TextView[2];
        progressText[0] = view.findViewById(R.id.log_fragment_text_download);
        progressText[1] = view.findViewById(R.id.log_fragment_text_json_parse);

        progressBars = new ProgressBar[2];
        progressBars[0] = view.findViewById(R.id.log_fragment_progress_download);
        progressBars[1] = view.findViewById(R.id.log_fragment_progress_json_parse);

        progressIcons = new ImageView[2];
        progressIcons[0] = view.findViewById(R.id.log_fragment_icon_download);
        progressIcons[1] = view.findViewById(R.id.log_fragment_icon_json_parse);



        Context context = getActivity();

        if (context != null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        subtitle.setOnClickListener((View v) -> showDialog());

        messageAdapter = new MessageAdapter(context, new ArrayList<>());
        messageListView.setAdapter(messageAdapter);

        messageListView.setPadding(messageListView.getPaddingLeft(),
                messageListView.getPaddingTop(),
                messageListView.getPaddingRight() + messageListView.getVerticalScrollbarWidth(),
                messageListView.getPaddingBottom());

        setHasOptionsMenu(true);

        init: {
            if (sharedPreferences != null) {
                long downloadId = sharedPreferences.getLong(LOG_DOWNLOAD_ID_KEY, -1);
                if (downloadId != -1) {
                    logDownload = new Download(downloadId);
                    if ((logDownload.getDownloadStatus() & DownloadManager.STATUS_FAILED) == 0) {
                        break init;
                    }
                }
            }

            reload();
        }

        database = new ChatDatabase();
        database.init(view.getContext(), this);

        return view;
    }

    private void reload() {
        reload(null, null, null);
    }
    private void reload(Mode mode, String channel, Map<String,Long> data) {
        if (data != null) {
            this.data = data;
        }
        if (mode != null) this.mode = mode;
        if (channel != null) this.channel = channel;

        setStatus(0, 0, 0);

        progressTable.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);
        labelError.setVisibility(View.GONE);
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
            case POST_INTERVAL: {
                Long from = this.data.get(LOG_FROM_KEY);
                Long to = this.data.get(LOG_TO_KEY);

                options = "mode=" + this.mode.modeStr;
                options += "&from=" + from;
                options += "&to=" + to;

                subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_interval), from != null ? from : Long.MAX_VALUE, to != null ? to : Long.MIN_VALUE);
                break;
            }
            case FILE: {
                subtitle = getString(R.string.log_subtitle_file);
                break;
            }
        }

        options += "&channel=" + this.channel;

        this.subtitle.setText(subtitle);

        messageAdapter.clear();

        // handling of running downloads is done before reload is called
        if (logDownload != null) Download.stopDownload(logDownload);

        if (mode == Mode.FILE) options = "FILE" + pathData;

        String tag = getClass().toString();

        QEDChatPages.interrupt(getClass().toString());

        downloadListener = new QEDChatPages.LogDownloadListener(tag, messageAdapter, requireActivity(), this);
        logDownload = QEDChatPages.getChatLog(tag, options, this, downloadListener);
    }






    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.log_save) {
            database.insertAll(messageAdapter.getData());
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (logDownload != null && (downloadListener == null || !downloadListener.equals(logDownload.getListener()))) {
            int downloadStatus = logDownload.getDownloadStatus();
            if ((downloadStatus & (DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED)) == 0) {
                downloadListener = new QEDChatPages.LogDownloadListener(getClass().toString(), messageAdapter, requireActivity(), this);

                logDownload.attachListener(downloadListener);
            } else if ((downloadStatus & DownloadManager.STATUS_SUCCESSFUL) == DownloadManager.STATUS_SUCCESSFUL) {
                downloadListener = new QEDChatPages.LogDownloadListener(getClass().toString(), messageAdapter, requireActivity(), this);

                logDownload.attachListener(downloadListener);
                DownloadBroadcastReceiver.cancelNotification(requireActivity(), logDownload.getDownloadId());
            } else if ((downloadStatus & DownloadManager.STATUS_FAILED) == DownloadManager.STATUS_FAILED) {
                onError(getClass().toString(), "download status is STATUS_FAILED", null);
            }
        }
    }

    @Override
    public void onPause() {
        if (logDownload != null && sharedPreferences != null) {
            sharedPreferences.edit().putLong(LOG_DOWNLOAD_ID_KEY, logDownload.getDownloadId()).apply();
            downloadListener = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        database.close();
        super.onDestroy();
    }


    @Override
    Boolean onDrop(boolean force) {
        Application application = applicationReference.get();

        Activity activity = getActivity();
        if (!(activity instanceof MainActivity)) return true;

        int asyncStatus = QEDChatPages.getStatus(getClass().toString());

        boolean downloadRunning = (asyncStatus & (4 | 2)) != 0; // 4 = download running, 2 = download pending
        boolean parseRunning = (asyncStatus & (16)) != 0; // 16 = parse thread alive

        final MainActivity mainActivity = (MainActivity) activity;

        if (force && application != null && downloadRunning) { // force closing
            Toast.makeText(activity, R.string.log_download_continue_in_background_toast, Toast.LENGTH_LONG).show();
            downloadContinueInBackground();
            return true;
        } else if (application != null && downloadRunning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setMessage(R.string.log_download_running);

            // do nothing
            builder.setPositiveButton(R.string.log_download_stay, (dialog, which) -> {
                mainActivity.onDropResult(false);
                dialog.dismiss();
            });

            // interrupt download manger, ui async task and parse thread
            builder.setNegativeButton(R.string.log_download_cancel, (dialog, which) -> {
                mainActivity.onDropResult(true);
                downloadGoAndCancel();
                dialog.dismiss();
            });

            // interrupt ui async task
            builder.setNeutralButton(R.string.log_download_continue_in_background, (dialog, which) -> {
                mainActivity.onDropResult(true);
                downloadContinueInBackground();
                dialog.dismiss();
            });

            // stay on cancel
            builder.setOnCancelListener(dialog -> {
                mainActivity.onDropResult(false);
                dialog.dismiss();
            });

            dropAlertDialog = builder.show();
            return null;
        } else if (application != null && parseRunning) {
            QEDChatPages.interrupt(getClass().toString());
            return true;
        } else {
            return true;
        }
    }
    private void downloadContinueInBackground() {
        DownloadListener listener = logDownload.getListener();
        if (listener != null)
            logDownload.detachListener(listener);
    }
    private void downloadGoAndCancel() {
        QEDChatPages.interrupt(getClass().toString());
    }






    @Override
    public void onPageReceived(String tag, File file) {
        if (dropAlertDialog != null) dropAlertDialog.cancel();

        sharedPreferences.edit().putLong(LOG_DOWNLOAD_ID_KEY, -1).apply();
        logDownload = null;

        messageAdapter.notifyDataSetChanged();
        progressTable.setVisibility(View.GONE);
        messageListView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(tag, reason, cause);

        handler.post(() -> {
            if (REASON_NETWORK.equals(reason))
                labelError.setText(R.string.cant_connect);
            else
                labelError.setText(R.string.unknown_error);


            progressTable.setVisibility(View.GONE);
            labelError.setVisibility(View.VISIBLE);
        });
    }

    /*
     * (Integer.MIN_VALUE, Integer.MIN_VALUE) -> download complete
     * (_ < 0, _) -> (status, bytes downloaded)
     * (_, _) -> parse progress
     */
    @Override
    public void onProgressUpdate(String tag, long done, long total) {
        //noinspection StatementWithEmptyBody
        if (done == Integer.MIN_VALUE && total == Integer.MIN_VALUE) {
        } else if (done < 0) { // download
            long mib = total / 1_048_576;
            long dot = (int)((total % 1_048_576) / 1_048_576d * 100);

            switch ( - (int) done) {
                case DownloadManager.STATUS_PENDING:
                    setStatus(0, 0, 0);
                    break;
                case DownloadManager.STATUS_RUNNING:
                    setStatus(2, mib, dot);
                    break;
                case DownloadManager.STATUS_PAUSED:
                    setStatus(1, mib, dot);
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    setStatus(3, mib, dot);
                    break;
                case DownloadManager.STATUS_FAILED:
                    setStatus(4, mib, dot);
                    break;

            }
        } else { // parsing
            handler.post(() -> setStatus(5 + (1 << 4), done, total));
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





    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 0b1001001001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                pathEditText.setText(uri.getPath());
                pathData = uri.toString();
            }
        }
    }

    private void showDialog() {
        Context context = getContext();
        if (context == null) return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setTitle(R.string.title_activity_log_dialog);

        @SuppressLint("InflateParams")
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
        View fragmentFile = view1.findViewById(R.id.log_fragment_file);
        View fragmentPostInterval = view1.findViewById(R.id.log_fragment_postinterval);

        TextView runningDownload = view1.findViewById(R.id.log_dialog_running_download);
        runningDownload.setVisibility((logDownload != null && ((logDownload.getDownloadStatus() & (DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED)) == 0)) ? View.VISIBLE : View.GONE);

        EditText postRecentLast = view1.findViewById(R.id.log_dialog_postrecent_editText);
        EditText dateRecentLast = view1.findViewById(R.id.log_dialog_daterecent_editText);
        EditText dateIntervalStart = view1.findViewById(R.id.log_dialog_dateinterval_editText_from);
        EditText dateIntervalEnd = view1.findViewById(R.id.log_dialog_dateinterval_editText_to);
        EditText postIntervalStart = view1.findViewById(R.id.log_dialog_postinterval_editText_from);
        EditText postIntervalEnd = view1.findViewById(R.id.log_dialog_postinterval_editText_to);
        pathEditText = view1.findViewById(R.id.log_dialog_file_editText);

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

        pathEditText.setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            Intent intent2 = Intent.createChooser(intent, "Choose a file");
            startActivityForResult(intent2, 0b1001001001);
        });


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
                    fragmentFile.setVisibility(View.GONE);
                    fragmentPostInterval.setVisibility(View.GONE);
                    mode.set(POST_RECENT);
                } else if (DATE_RECENT.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.VISIBLE);
                    fragmentDateInterval.setVisibility(View.GONE);
                    fragmentFile.setVisibility(View.GONE);
                    fragmentPostInterval.setVisibility(View.GONE);
                    mode.set(DATE_RECENT);
                } else if (DATE_INTERVAL.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.GONE);
                    fragmentDateInterval.setVisibility(View.VISIBLE);
                    fragmentFile.setVisibility(View.GONE);
                    fragmentPostInterval.setVisibility(View.GONE);
                    mode.set(DATE_INTERVAL);
                } else if (POST_INTERVAL.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.GONE);
                    fragmentDateInterval.setVisibility(View.GONE);
                    fragmentFile.setVisibility(View.GONE);
                    fragmentPostInterval.setVisibility(View.VISIBLE);
                    mode.set(POST_INTERVAL);
                } else if (SINCE_OWN.modeStr.equals(item)) {
                    // TODO fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
                    mode.set(SINCE_OWN);
                } else if (Mode.FILE.modeStr.equals(item)) {
                    fragmentPostRecent.setVisibility(View.GONE);
                    fragmentDateRecent.setVisibility(View.GONE);
                    fragmentDateInterval.setVisibility(View.GONE);
                    fragmentFile.setVisibility(View.VISIBLE);
                    mode.set(Mode.FILE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        alertDialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {
            switch (mode.get()) {
                case POST_RECENT:
                    try {
                        data.put(LOG_LAST_KEY, Long.valueOf(postRecentLast.getText().toString().trim()));
                    } catch (NumberFormatException e) {
                        data.put(LOG_LAST_KEY, 0L);
                    }
                    break;
                case DATE_RECENT:
                    try {
                        data.put(LOG_LAST_KEY, 3600 * Long.valueOf(dateRecentLast.getText().toString().trim()));
                    } catch (NumberFormatException e) {
                        data.put(LOG_LAST_KEY, 0L);
                    }
                    break;
                case DATE_INTERVAL:
                    data.put(LOG_FROM_KEY, dateStart.get().getTime());
                    data.put(LOG_TO_KEY, dateEnd.get().getTime());
                    break;
                case FILE:
                    break;
                case POST_INTERVAL:
                    try {
                        data.put(LOG_FROM_KEY, Long.valueOf(postIntervalStart.getText().toString().trim()));
                    } catch (NumberFormatException e) {
                        data.put(LOG_FROM_KEY, Long.MAX_VALUE);
                    }
                    try {
                        data.put(LOG_TO_KEY, Long.valueOf(postIntervalEnd.getText().toString().trim()));
                    } catch (NumberFormatException e) {
                        data.put(LOG_TO_KEY, Long.MIN_VALUE);
                    }
                    break;
            }

            reload(mode.get(), channel, data);
            dialog.dismiss();
        });

        alertDialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        alertDialogBuilder.setView(view1);

        alertDialogBuilder.show();
    }

    // status = [4 bit parse status][4 bit download status]
    // parse status: 0: pending, 1: progress, 2: failed, 3: keep
    // download status: 0: pending, 1: paused, 2: downloading, 3: success, 4: failed, 5: keep
    private void setStatus(int status, long data0, long data1) {
        int downloadStatus = status & 0b1111;
        int parseStatus = (status >> 4) & 0b1111;

        switch (downloadStatus) {
            case 0:
                progressText[0].setText(downloadPendingString);
                progressText[0].setEnabled(false);

                progressIcons[0].setVisibility(View.VISIBLE);
                progressIcons[0].setEnabled(false);

                progressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 1:
                progressText[0].setText(String.format(downloadPausedString, data0 + "." + data1 + " MiB"));
                progressText[0].setEnabled(true);

                progressIcons[0].setVisibility(View.VISIBLE);
                progressIcons[0].setEnabled(true);

                progressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 2:
                progressText[0].setText(String.format(downloadProgressString, data0 + "." + data1 + " MiB"));
                progressText[0].setEnabled(true);

                progressIcons[0].setVisibility(View.INVISIBLE);
                progressIcons[0].setEnabled(false);

                progressBars[0].setVisibility(View.VISIBLE);
                break;
            case 3:
                progressText[0].setText(String.format(downloadSuccessfulString, data0 + "." + data1 + " MiB"));
                progressText[0].setEnabled(true);

                progressIcons[0].setVisibility(View.VISIBLE);
                progressIcons[0].setEnabled(true);

                progressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 4:
                progressText[0].setText(String.format(downloadFailedString, data0 + "." + data1 + " MiB"));
                progressText[0].setEnabled(true);

                progressIcons[0].setVisibility(View.VISIBLE);
                progressIcons[0].setEnabled(true);

                progressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 5:
                break;
        }

        switch (parseStatus) {
            case 0:
                progressText[1].setText(parsePendingString);
                progressText[1].setEnabled(false);

                progressIcons[1].setVisibility(View.VISIBLE);
                progressIcons[1].setEnabled(false);

                progressBars[1].setVisibility(View.INVISIBLE);
                break;
            case 1:
                progressText[1].setText(String.format(parseProgressString, data0 + "/" + data1));
                progressText[1].setEnabled(true);

                progressIcons[1].setVisibility(View.INVISIBLE);
                progressIcons[1].setEnabled(false);

                progressBars[1].setVisibility(View.VISIBLE);
                break;
            case 2:
                progressText[1].setText(String.format(parseFailedString, data0 + "/" + data1));
                progressText[1].setEnabled(true);

                progressIcons[1].setVisibility(View.VISIBLE);
                progressIcons[1].setEnabled(true);

                progressBars[1].setVisibility(View.INVISIBLE);
                break;
            case 3:
                break;
        }
    }

    public enum Mode {
        POST_RECENT("postrecent"), DATE_RECENT("daterecent"), DATE_INTERVAL("dateinterval"), POST_INTERVAL("postinterval"), SINCE_OWN("fromownpost"), FILE("file");

        final String modeStr;

        Mode(String modeStr) {
            this.modeStr = modeStr;
        }
    }
}
