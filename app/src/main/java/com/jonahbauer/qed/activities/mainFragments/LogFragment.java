package com.jonahbauer.qed.activities.mainFragments;

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
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.messageInfoSheet.MessageInfoBottomSheet;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.networking.QEDChatPages;
import com.jonahbauer.qed.networking.QEDPageStreamReceiver;
import com.jonahbauer.qed.networking.downloadManager.Download;
import com.jonahbauer.qed.networking.downloadManager.DownloadBroadcastReceiver;
import com.jonahbauer.qed.networking.downloadManager.DownloadListener;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.jonahbauer.qed.activities.mainFragments.LogFragment.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.activities.mainFragments.LogFragment.Mode.DATE_RECENT;
import static com.jonahbauer.qed.activities.mainFragments.LogFragment.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.activities.mainFragments.LogFragment.Mode.POST_RECENT;
import static com.jonahbauer.qed.activities.mainFragments.LogFragment.Mode.SINCE_OWN;

public class LogFragment extends QEDFragment implements ChatDatabaseReceiver, QEDPageStreamReceiver {
    private static final String DOWNLOAD_PROGRESS_STRING;
    private static final String DOWNLOAD_PENDING_STRING;
    private static final String DOWNLOAD_PAUSED_STRING;
    private static final String DOWNLOAD_SUCCESSFUL_STRING;
    private static final String DOWNLOAD_FAILED_STRING;
    private static final String PARSE_PENDING_STRING;
    private static final String PARSE_PROGRESS_STRING;
    private static final String PARSE_FAILED_STRING;

    private static final String LOG_DOWNLOAD_ID_KEY = "downloadId";

    private static final String ARGUMENT_MODE = "mode";
    private static final String ARGUMENT_CHANNEL = "channel";
    private static final String ARGUMENT_DATA = "data";

    private static final int KEY_DATE_RECENT_LAST = 0;

    private static final int KEY_POST_RECENT_LAST = 1;

    private static final int KEY_DATE_INTERVAL_FROM = 2;
    private static final int KEY_DATE_INTERVAL_TO = 3;

    private static final int KEY_POST_INTERVAL_FROM = 4;
    private static final int KEY_POST_INTERVAL_TO = 5;

    private static final int KEY_SINCE_OWN_SKIP = 6;

    static {
        SoftReference<Application> appRef = Application.getApplicationReference();
        Application application = appRef.get();
        if (application != null) {
            DOWNLOAD_PROGRESS_STRING = application.getString(R.string.log_downloading);
            DOWNLOAD_PENDING_STRING = application.getString(R.string.log_download_pending);
            DOWNLOAD_PAUSED_STRING = application.getString(R.string.log_download_paused);
            DOWNLOAD_SUCCESSFUL_STRING = application.getString(R.string.log_download_successful);
            DOWNLOAD_FAILED_STRING = application.getString(R.string.log_download_failed);
            PARSE_PROGRESS_STRING = application.getString(R.string.log_parsing);
            PARSE_PENDING_STRING = application.getString(R.string.log_parse_pending);
            PARSE_FAILED_STRING = application.getString(R.string.log_parse_failed);
        } else {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            DOWNLOAD_PROGRESS_STRING = "Error";
            PARSE_PROGRESS_STRING = "Error";
            PARSE_PENDING_STRING = "Error";
            PARSE_FAILED_STRING = "Error";
            DOWNLOAD_PENDING_STRING = "Error";
            DOWNLOAD_PAUSED_STRING = "Error";
            DOWNLOAD_FAILED_STRING = "Error";
            DOWNLOAD_SUCCESSFUL_STRING = "Error";
        }
    }

    private SharedPreferences mSharedPreferences;

    private Mode mMode;
    private String mChannel;
    private Long[] mData;

    private String mPathData;
    private EditText mPathEditText;

    private String mOptions;
    private Download mLogDownload = null;
    private MessageAdapter mMessageAdapter;

    private ProgressBar mSaveProgressBar;
    private ListView mMessageListView;
    private TextView mSubtitle;
    private TextView mLabelError;

    private TableLayout mProgressTable;
    private TextView[] mProgressText;
    private ProgressBar[] mProgressBars;
    private ImageView[] mProgressIcons;

    private ChatDatabase mDatabase;

    private AlertDialog mDropAlertDialog;
    private QEDChatPages.LogDownloadListener mDownloadListener;


    public static LogFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_log);

        LogFragment fragment = new LogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            this.mMode = (Mode) args.getSerializable(ARGUMENT_MODE);
            this.mChannel = args.getString(ARGUMENT_CHANNEL);
            this.mData = ((Long[]) args.getSerializable(ARGUMENT_DATA));
        }

        fillWithStandard();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSaveProgressBar = view.findViewById(R.id.save_progress);
        mMessageListView = view.findViewById(R.id.log_message_container);
        mSubtitle = view.findViewById(R.id.log_fragment_subtitle);
        mLabelError = view.findViewById(R.id.label_offline);

        mProgressTable = view.findViewById(R.id.log_fragment_progress_table);
        mProgressText = new TextView[2];
        mProgressText[0] = view.findViewById(R.id.log_fragment_text_download);
        mProgressText[1] = view.findViewById(R.id.log_fragment_text_json_parse);

        mProgressBars = new ProgressBar[2];
        mProgressBars[0] = view.findViewById(R.id.log_fragment_progress_download);
        mProgressBars[1] = view.findViewById(R.id.log_fragment_progress_json_parse);

        mProgressIcons = new ImageView[2];
        mProgressIcons[0] = view.findViewById(R.id.log_fragment_icon_download);
        mProgressIcons[1] = view.findViewById(R.id.log_fragment_icon_json_parse);


        Context context = getActivity();

        if (context != null)
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mSubtitle.setOnClickListener((View v) -> showDialog());

        mMessageAdapter = new MessageAdapter(context, new ArrayList<>(), null, false, null, false);
        mMessageListView.setAdapter(mMessageAdapter);
        mMessageListView.setOnItemClickListener((adapterView, view1, i, l) -> setChecked(i, false));
        mMessageListView.setOnItemLongClickListener((adapterView, view1, i, l) -> {
            if (!mMessageListView.isItemChecked(i)) {
                int checked = mMessageListView.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(i, true);
                return true;
            } else return false;
        });
        mMessageListView.setPadding(mMessageListView.getPaddingLeft(),
                mMessageListView.getPaddingTop(),
                mMessageListView.getPaddingRight() + mMessageListView.getVerticalScrollbarWidth(),
                mMessageListView.getPaddingBottom());

        setHasOptionsMenu(true);

        init: {
            if (mSharedPreferences != null) {
                long downloadId = mSharedPreferences.getLong(LOG_DOWNLOAD_ID_KEY, -1);
                if (downloadId != -1) {
                    mLogDownload = new Download(downloadId);
                    if ((mLogDownload.getDownloadStatus() & DownloadManager.STATUS_FAILED) == 0) {
                        break init;
                    }
                }
            }

            reload();
        }

        mDatabase = new ChatDatabase();
        mDatabase.init(view.getContext(), this);
    }

    private void fillWithStandard() {
        if (mMode == null) mMode = DATE_RECENT;
        if (mChannel == null) mChannel = "";
        if (mData == null) mData = new Long[8];

        long now = Calendar.getInstance().getTimeInMillis();

        if (mData[KEY_DATE_RECENT_LAST] == null) mData[KEY_DATE_RECENT_LAST] = 86400L;

        if (mData[KEY_POST_RECENT_LAST] == null) mData[KEY_POST_RECENT_LAST] = 200L;

        if (mData[KEY_DATE_INTERVAL_FROM] == null) mData[KEY_DATE_INTERVAL_FROM] = now - 86400L;
        if (mData[KEY_DATE_INTERVAL_TO] == null) mData[KEY_DATE_INTERVAL_TO] = now;

        if (mData[KEY_POST_INTERVAL_FROM] == null) mData[KEY_POST_INTERVAL_FROM] = 0L;
        if (mData[KEY_POST_INTERVAL_TO] == null) mData[KEY_POST_INTERVAL_TO] = 0L;

        if (mData[KEY_SINCE_OWN_SKIP] == null) mData[KEY_SINCE_OWN_SKIP] = 20L;
    }

    private void reload() {
        reload(null, null, null);
    }
    private void reload(Mode mode, String channel, Long[] data) {
        if (data != null) this.mData = data;

        if (mode != null) this.mMode = mode;

        if (channel != null) this.mChannel = channel;

        fillWithStandard();

        setStatus(0, 0, 0);

        mProgressTable.setVisibility(View.VISIBLE);
        mMessageListView.setVisibility(View.GONE);
        mLabelError.setVisibility(View.GONE);
        String subtitle = "";

        switch (this.mMode) {
            case POST_RECENT: {
                long last = this.mData[KEY_POST_RECENT_LAST];
                mOptions = "mode=" + this.mMode.modeStr;
                mOptions += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_recent), last);
                break;
            }
            case DATE_RECENT: {
                long last = this.mData[KEY_DATE_RECENT_LAST];
                mOptions = "mode=" + this.mMode.modeStr;
                mOptions += "&last=" + last;
                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_recent), last / 3600);
                break;
            }
            case DATE_INTERVAL: {
                long from = this.mData[KEY_DATE_INTERVAL_FROM];
                long to = this.mData[KEY_DATE_INTERVAL_TO];

                Date dateFrom = new Date(from);
                Date dateTo = new Date(to);

                mOptions = "mode=" + this.mMode.modeStr;
                mOptions += "&from=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateFrom);
                mOptions += "&to=" + MessageFormat.format("{0,date,yyyy-MM-dd}", dateTo);

                subtitle = MessageFormat.format(getString(R.string.log_subtitle_date_interval), dateFrom, dateTo);
                break;
            }
            case POST_INTERVAL: {
                long from = this.mData[KEY_POST_INTERVAL_FROM];
                long to = this.mData[KEY_POST_INTERVAL_TO];

                mOptions = "mode=" + this.mMode.modeStr;
                mOptions += "&from=" + from;
                mOptions += "&to=" + to;

                subtitle = MessageFormat.format(getString(R.string.log_subtitle_post_interval), from, to);
                break;
            }
            case FILE: {
                subtitle = getString(R.string.log_subtitle_file);
                break;
            }
        }

        mOptions += "&channel=" + this.mChannel;

        this.mSubtitle.setText(subtitle);

        mMessageAdapter.clear();

        // handling of running downloads is done before reload is called
        if (mLogDownload != null) Download.stopDownload(mLogDownload);

        if (mode == Mode.FILE) mOptions = "FILE" + mPathData;

        String tag = getClass().toString();

        QEDChatPages.interrupt(getClass().toString());

        mDownloadListener = new QEDChatPages.LogDownloadListener(tag, mMessageAdapter, Objects.requireNonNull(mApplicationReference.get()), this);
        mLogDownload = QEDChatPages.getChatLog(tag, mOptions, this, mDownloadListener);
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

                toolbar.inflateMenu(R.menu.menu_message);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.message_info) {
                        MessageInfoBottomSheet sheet = MessageInfoBottomSheet.newInstance(msg, R.style.AppTheme_BottomSheetDialog);
                        sheet.show(getChildFragmentManager(), sheet.getTag());
                    }

                    return false;
                });

                if (msg != null) toolbar.setTitle(msg.name);
            } else {
                mainActivity.returnAltToolbar();
            }
        }
    }



    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.log_save) {
            mDatabase.insertAll(mMessageAdapter.getData());
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLogDownload != null && (mDownloadListener == null || !mDownloadListener.equals(mLogDownload.getListener()))) {
            int downloadStatus = mLogDownload.getDownloadStatus();
            if ((downloadStatus & (DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED)) == 0) {
                mDownloadListener = new QEDChatPages.LogDownloadListener(getClass().toString(), mMessageAdapter, Objects.requireNonNull(mApplicationReference.get()), this);

                mLogDownload.attachListener(mDownloadListener);
            } else if ((downloadStatus & DownloadManager.STATUS_SUCCESSFUL) == DownloadManager.STATUS_SUCCESSFUL) {
                mDownloadListener = new QEDChatPages.LogDownloadListener(getClass().toString(), mMessageAdapter, Objects.requireNonNull(mApplicationReference.get()), this);

                mLogDownload.attachListener(mDownloadListener);
                DownloadBroadcastReceiver.cancelNotification(requireActivity(), mLogDownload.getDownloadId());
            } else if ((downloadStatus & DownloadManager.STATUS_FAILED) == DownloadManager.STATUS_FAILED) {
                onError(getClass().toString(), "download status is STATUS_FAILED", null);
            }
        }
    }

    @Override
    public void onPause() {
        if (mLogDownload != null && mSharedPreferences != null) {
            mSharedPreferences.edit().putLong(LOG_DOWNLOAD_ID_KEY, mLogDownload.getDownloadId()).apply();
            mDownloadListener = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mDatabase.close();
        super.onDestroy();
    }


    @Override
    public Boolean onDrop(boolean force) {
        Application application = mApplicationReference.get();

        Activity activity = getActivity();
        if (!(activity instanceof MainActivity)) return true;

        @QEDChatPages.Status
        int asyncStatus = QEDChatPages.getStatus(getClass().toString());

        boolean downloadRunning = (asyncStatus & (QEDChatPages.STATUS_DOWNLOAD_RUNNING | QEDChatPages.STATUS_DOWNLOAD_PENDING)) != 0;
        boolean parseRunning = (asyncStatus & (QEDChatPages.STATUS_PARSE_THREAD_ALIVE)) != 0;

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

            mDropAlertDialog = builder.show();
            return null;
        } else if (application != null && parseRunning) {
            QEDChatPages.interrupt(getClass().toString());
            return true;
        } else {
            return true;
        }
    }
    private void downloadContinueInBackground() {
        DownloadListener listener = mLogDownload.getListener();
        if (listener != null)
            mLogDownload.detachListener(listener);
    }
    private void downloadGoAndCancel() {
        QEDChatPages.interrupt(getClass().toString());
    }






    @Override
    public void onPageReceived(String tag) {
        if (mDropAlertDialog != null) mDropAlertDialog.cancel();

        mSharedPreferences.edit().putLong(LOG_DOWNLOAD_ID_KEY, -1).apply();
        mLogDownload = null;

        mMessageAdapter.notifyDataSetChanged();
        mProgressTable.setVisibility(View.GONE);
        mMessageListView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageStreamReceiver.super.onError(tag, reason, cause);

        mHandler.post(() -> {
            if (REASON_NETWORK.equals(reason))
                mLabelError.setText(R.string.cant_connect);
            else
                mLabelError.setText(R.string.unknown_error);


            mProgressTable.setVisibility(View.GONE);
            mLabelError.setVisibility(View.VISIBLE);
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
            mHandler.post(() -> setStatus(5 + (1 << 4), done, total));
        }
    }




    @Override
    public void onReceiveResult(List<Message> messages) {}

    @Override
    public void onDatabaseError() {}

    @Override
    public void onInsertAllUpdate(int done, int total) {
        if (done < total) {
            mSaveProgressBar.setVisibility(View.VISIBLE);
            mSaveProgressBar.setProgress(done * 100 / total, true);
        } else {
            mSaveProgressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), getString(R.string.database_save_done), Toast.LENGTH_SHORT).show();
        }
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 0b1001001001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                mPathEditText.setText(uri.getPath());
                mPathData = uri.toString();
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

        Long[] data = new Long[8];
        AtomicReference<Mode> mode = new AtomicReference<>();
        AtomicReference<Date> dateStart = new AtomicReference<>();
        AtomicReference<Date> dateEnd = new AtomicReference<>();
        String channel = null;

        Date date = android.icu.util.Calendar.getInstance(TimeZone.getDefault()).getTime();
        dateStart.set(date);
        dateEnd.set(date);

        // get all views
        View fragmentPostRecent = view1.findViewById(R.id.log_fragment_postrecent);
        View fragmentDateRecent = view1.findViewById(R.id.log_fragment_daterecent);
        View fragmentDateInterval = view1.findViewById(R.id.log_fragment_dateinterval);
        View fragmentFile = view1.findViewById(R.id.log_fragment_file);
        View fragmentPostInterval = view1.findViewById(R.id.log_fragment_postinterval);

        TextView runningDownload = view1.findViewById(R.id.log_dialog_running_download);
        runningDownload.setVisibility((mLogDownload != null && ((mLogDownload.getDownloadStatus() & (DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED)) == 0)) ? View.VISIBLE : View.GONE);

        EditText postRecentLast = view1.findViewById(R.id.log_dialog_postrecent_editText);
        EditText dateRecentLast = view1.findViewById(R.id.log_dialog_daterecent_editText);
        EditText dateIntervalStart = view1.findViewById(R.id.log_dialog_dateinterval_editText_from);
        EditText dateIntervalEnd = view1.findViewById(R.id.log_dialog_dateinterval_editText_to);
        EditText postIntervalStart = view1.findViewById(R.id.log_dialog_postinterval_editText_from);
        EditText postIntervalEnd = view1.findViewById(R.id.log_dialog_postinterval_editText_to);
        mPathEditText = view1.findViewById(R.id.log_dialog_file_editText);

        // set listeners
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

        mPathEditText.setOnClickListener(v1 -> {
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


        // Create dialog
        alertDialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {
            switch (mode.get()) {
                case POST_RECENT:
                    try {
                        data[KEY_POST_RECENT_LAST] = Long.parseLong(postRecentLast.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
                    break;
                case DATE_RECENT:
                    try {
                        data[KEY_DATE_RECENT_LAST] = 3600 * Long.parseLong(dateRecentLast.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
                    break;
                case DATE_INTERVAL:
                    data[KEY_DATE_INTERVAL_FROM] = dateStart.get().getTime();
                    data[KEY_DATE_INTERVAL_TO] = dateEnd.get().getTime();
                    break;
                case FILE:
                    break;
                case POST_INTERVAL:
                    try {
                        data[KEY_POST_INTERVAL_FROM] = Long.parseLong(postIntervalStart.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}

                    try {
                        data[KEY_POST_INTERVAL_TO] = Long.parseLong(postIntervalEnd.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
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
                mProgressText[0].setText(DOWNLOAD_PENDING_STRING);
                mProgressText[0].setEnabled(false);

                mProgressIcons[0].setVisibility(View.VISIBLE);
                mProgressIcons[0].setEnabled(false);

                mProgressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 1:
                mProgressText[0].setText(String.format(DOWNLOAD_PAUSED_STRING, data0 + "." + data1 + " MiB"));
                mProgressText[0].setEnabled(true);

                mProgressIcons[0].setVisibility(View.VISIBLE);
                mProgressIcons[0].setEnabled(true);

                mProgressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 2:
                mProgressText[0].setText(String.format(DOWNLOAD_PROGRESS_STRING, data0 + "." + data1 + " MiB"));
                mProgressText[0].setEnabled(true);

                mProgressIcons[0].setVisibility(View.INVISIBLE);
                mProgressIcons[0].setEnabled(false);

                mProgressBars[0].setVisibility(View.VISIBLE);
                break;
            case 3:
                mProgressText[0].setText(String.format(DOWNLOAD_SUCCESSFUL_STRING, data0 + "." + data1 + " MiB"));
                mProgressText[0].setEnabled(true);

                mProgressIcons[0].setVisibility(View.VISIBLE);
                mProgressIcons[0].setEnabled(true);

                mProgressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 4:
                mProgressText[0].setText(String.format(DOWNLOAD_FAILED_STRING, data0 + "." + data1 + " MiB"));
                mProgressText[0].setEnabled(true);

                mProgressIcons[0].setVisibility(View.VISIBLE);
                mProgressIcons[0].setEnabled(true);

                mProgressBars[0].setVisibility(View.INVISIBLE);
                break;
            case 5:
                break;
        }

        switch (parseStatus) {
            case 0:
                mProgressText[1].setText(PARSE_PENDING_STRING);
                mProgressText[1].setEnabled(false);

                mProgressIcons[1].setVisibility(View.VISIBLE);
                mProgressIcons[1].setEnabled(false);

                mProgressBars[1].setVisibility(View.INVISIBLE);
                break;
            case 1:
                mProgressText[1].setText(String.format(PARSE_PROGRESS_STRING, data0 + "/" + data1));
                mProgressText[1].setEnabled(true);

                mProgressIcons[1].setVisibility(View.INVISIBLE);
                mProgressIcons[1].setEnabled(false);

                mProgressBars[1].setVisibility(View.VISIBLE);
                break;
            case 2:
                mProgressText[1].setText(String.format(PARSE_FAILED_STRING, data0 + "/" + data1));
                mProgressText[1].setEnabled(true);

                mProgressIcons[1].setVisibility(View.VISIBLE);
                mProgressIcons[1].setEnabled(true);

                mProgressBars[1].setVisibility(View.INVISIBLE);
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
