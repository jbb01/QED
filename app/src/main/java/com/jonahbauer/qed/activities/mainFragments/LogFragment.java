package com.jonahbauer.qed.activities.mainFragments;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.DateIntervalLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.DateRecentLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.OnlineLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.PostIntervalLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.PostRecentLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.SinceOwnLogRequest;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogLogModeBinding;
import com.jonahbauer.qed.databinding.FragmentLogBinding;
import com.jonahbauer.qed.layoutStuff.CustomArrayAdapter;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.viewmodel.LogViewModel;
import com.jonahbauer.qed.model.viewmodel.LogViewModel.LogRequest;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Callback;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TimeUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogFragment extends QEDFragment implements Callback<LogRequest> {
    public static final String ARGUMENT_LOG_REQUEST = "logRequest";
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.logfragment.dialog";

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_DONE = 2;

    private MessageAdapter mMessageAdapter;
    private FragmentLogBinding mBinding;

    private LogViewModel mLogViewModel;

    private LogRequest mLogRequest;

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
            this.mLogRequest = (OnlineLogRequest) args.getSerializable(ARGUMENT_LOG_REQUEST);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentLogBinding.bind(view);
        mLogViewModel = new ViewModelProvider(this).get(LogViewModel.class);

        mMessageAdapter = new MessageAdapter(requireContext(), new ArrayList<>(), null, false, null, false);
        mBinding.list.setAdapter(mMessageAdapter);
        mBinding.list.setOnItemClickListener((parent, view1, position, id) -> setChecked(position, false));
        mBinding.list.setOnItemLongClickListener((parent, view1, position, id) -> {
            if (!mBinding.list.isItemChecked(position)) {
                int checked = mBinding.list.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(position, true);
                return true;
            }
            return false;
        });

        mBinding.subtitle.setOnClickListener(v -> {
            LogDialog logDialog = LogDialog.newInstance();
            logDialog.show(getChildFragmentManager(), FRAGMENT_TAG);
        });

        mLogViewModel.getDownloadStatus().observe(getViewLifecycleOwner(), pair -> {
            if (pair == null) {
                mBinding.setDownloadStatus(STATUS_PENDING);
                mBinding.setDownloadText(getString(R.string.log_status_download_pending));
            } else {
                double done = pair.leftLong() / 1_048_576d;

                if (done > Application.MEMORY_CLASS) {
                    mBinding.setStatusText(getString(R.string.log_status_likely_oom));
                }

                if (pair.leftLong() == pair.rightLong()) {
                    mBinding.setDownloadStatus(STATUS_DONE);
                    mBinding.setDownloadText(getString(R.string.log_status_download_successful, done));
                } else {
                    mBinding.setDownloadStatus(STATUS_RUNNING);
                    mBinding.setDownloadText(getString(R.string.log_status_downloading, done));
                }
            }
        });

        mLogViewModel.getParseStatus().observe(getViewLifecycleOwner(), pair -> {
            if (pair == null) {
                mBinding.setParseStatus(STATUS_PENDING);
                mBinding.setParseText(getString(R.string.log_status_parse_pending));
            } else {
                long done = pair.leftLong();
                long total = pair.rightLong();

                if (done == total) {
                    mBinding.setParseStatus(STATUS_DONE);
                } else {
                    mBinding.setParseStatus(STATUS_RUNNING);
                }
                mBinding.setParseText(getString(R.string.log_status_parsing, done, total));
            }
        });

        mLogViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            mBinding.setStatus(messages.getCode());

            mMessageAdapter.clear();
            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                mMessageAdapter.addAll(messages.getValue());
            } else if (messages.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = messages.getReason();
                mBinding.setError(getString(reason.getStringRes()));
            }
            mMessageAdapter.notifyDataSetChanged();
        });

        reload();
    }

    private void reload() {
        reload(null);
    }

    private void reload(LogRequest logRequest) {
        if (logRequest != null) {
            mLogRequest = logRequest;
        }
        if (mLogRequest == null) {
            mLogRequest = new DateRecentLogRequest(Preferences.chat().getChannel(), 24, TimeUnit.HOURS);
        }

        mBinding.setSubtitle(mLogRequest.getSubtitle(getResources()));
        mBinding.setStatusText(null);
        mLogViewModel.load(mLogRequest);
    }

    /**
     * Sets the checked item in the list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     * @param value if the item is checked or not
     */
    private void setChecked(int position, boolean value) {
        MessageUtils.setChecked(this, mBinding.list, mMessageAdapter, position, value);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.log_save) {
            mBinding.setSaving(true);
            item.setEnabled(false);
            //noinspection ResultOfMethodCallIgnored
            Database.getInstance(requireContext()).messageDao()
                    .insert(mMessageAdapter.getData())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                        mBinding.setSaving(false);
                        item.setEnabled(true);
                    })
                    .subscribe(
                            () -> Snackbar.make(requireView(), R.string.saved, Snackbar.LENGTH_SHORT).show(),
                            (e) -> Snackbar.make(requireView(), R.string.save_error, Snackbar.LENGTH_SHORT).show()
                    );
            return true;
        }
        return false;
    }

    @Override
    public void onResult(LogRequest logRequest) {
        if (logRequest != null) {
            reload(logRequest);
        }
    }

    public static class LogDialog extends DialogFragment implements AdapterView.OnItemSelectedListener,
                                                                     DialogInterface.OnClickListener,
                                                                     DialogInterface.OnDismissListener,
                                                                     DialogInterface.OnCancelListener
    {

        // date interval
        private final MutableLiveData<LocalDate> mDateFrom = new MutableLiveData<>(LocalDate.now());
        private final MutableLiveData<LocalTime> mTimeFrom = new MutableLiveData<>(LocalTime.now());
        private final MutableLiveData<LocalDate> mDateTo = new MutableLiveData<>(LocalDate.now());
        private final MutableLiveData<LocalTime> mTimeTo = new MutableLiveData<>(LocalTime.now());
        private final LiveData<LocalDateTime> mDateTimeFrom = TimeUtils.combine(mDateFrom, mTimeFrom);
        private final LiveData<LocalDateTime> mDateTimeTo = TimeUtils.combine(mDateTo, mTimeTo);

        // file
        private final ActivityResultLauncher<String[]> mFileChooser;
        private final MutableLiveData<Uri> mFile = new MutableLiveData<>();

        private AlertDialogLogModeBinding mBinding;

        public static LogDialog newInstance() {
            return new LogDialog();
        }

        public LogDialog() {
            mFileChooser = registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    mFile::setValue
            );
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.title_activity_log_dialog);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ok, this);
            builder.setNegativeButton(R.string.cancel, this);
            builder.setView(createView(requireContext()));
            return builder.create();
        }

        @NonNull
        public View createView(@NonNull Context context) {
            mBinding = AlertDialogLogModeBinding.inflate(LayoutInflater.from(context));

            // setup mode spinner
            var adapter = new CustomArrayAdapter<>(context, android.R.layout.simple_spinner_item, LogViewModel.Mode.values());
            adapter.setToString((mode) -> context.getString(mode.toStringRes()));
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            mBinding.logDialogModeSpinner.setAdapter(adapter);
            mBinding.logDialogModeSpinner.setSelection(0);
            mBinding.logDialogModeSpinner.setOnItemSelectedListener(this);

            // setup file
            mBinding.logDialogFile.setOnClickListener(v -> {
                mFileChooser.launch(new String[] {"*/*"});
                mFile.observe(this, uri -> mBinding.logDialogFile.setText(uri != null ? uri.getPath() : ""));
            });

            // setup date interval
            ViewUtils.setupDateEditText(mBinding.logDialogDateintervalDateFrom, mDateFrom);
            ViewUtils.setupTimeEditText(mBinding.logDialogDateintervalTimeFrom, mTimeFrom);
            ViewUtils.setupDateEditText(mBinding.logDialogDateintervalDateTo, mDateTo);
            ViewUtils.setupTimeEditText(mBinding.logDialogDateintervalTimeTo, mTimeTo);

            Function<LocalDateTime, String> dateTimeWarning = (dateTime) -> {
                LocalDateTime converted = TimeUtils.check(dateTime);
                if (!converted.isEqual(dateTime)) {
                    return MessageFormat.format(
                            context.getString(R.string.log_dialog_date_convert_timezone),
                            TimeUtils.format(dateTime),
                            TimeUtils.format(converted)
                    );
                }
                return null;
            };
            mDateTimeFrom.observe(this, dateTime -> mBinding.setErrorDateFrom(dateTimeWarning.apply(dateTime)));
            mDateTimeTo.observe(this, dateTime -> mBinding.setErrorDateTo(dateTimeWarning.apply(dateTime)));

            return mBinding.getRoot();
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = (Spinner) parent;
            LogViewModel.Mode item = (LogViewModel.Mode) spinner.getItemAtPosition(position);
            mBinding.setMode(item);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == BUTTON_POSITIVE) {
                String channel = mBinding.logDialogChannel.getText().toString();
                LogViewModel.Mode mode = (LogViewModel.Mode) mBinding.logDialogModeSpinner.getSelectedItem();

                LogRequest logRequest = null;
                switch (mode) {
                    case POST_RECENT:
                        try {
                            long last = Long.parseLong(mBinding.logDialogPostrecent.getText().toString());
                            logRequest = new PostRecentLogRequest(channel, last);
                            ViewUtils.setError(mBinding.logDialogPostrecent, false);
                        } catch (NumberFormatException e) {
                            ViewUtils.setError(mBinding.logDialogPostrecent, true);
                        }
                        break;
                    case DATE_RECENT:
                        try {
                            long last = Long.parseLong(mBinding.logDialogDaterecent.getText().toString());
                            logRequest = new DateRecentLogRequest(channel, last, TimeUnit.HOURS);
                            ViewUtils.setError(mBinding.logDialogDaterecent, false);
                        } catch (NumberFormatException e) {
                            ViewUtils.setError(mBinding.logDialogDaterecent, true);
                        }
                        break;
                    case POST_INTERVAL:
                        long from = 0, to = 0;
                        boolean error = false;
                        try {
                            from = Long.parseLong(mBinding.logDialogPostintervalFrom.getText().toString());
                            ViewUtils.setError(mBinding.logDialogPostintervalFrom, false);
                        } catch (NumberFormatException e) {
                            ViewUtils.setError(mBinding.logDialogPostintervalFrom, true);
                            error = true;
                        }
                        try {
                            to = Long.parseLong(mBinding.logDialogPostintervalTo.getText().toString());
                            ViewUtils.setError(mBinding.logDialogPostintervalTo, false);
                        } catch (NumberFormatException e) {
                            ViewUtils.setError(mBinding.logDialogPostintervalTo, true);
                            error = true;
                        }

                        if (error) break;

                        logRequest = new PostIntervalLogRequest(channel, from, to);
                        break;
                    case DATE_INTERVAL:
                        Instant instantFrom = ZonedDateTime.of(mDateTimeFrom.getValue(), ZoneId.systemDefault()).toInstant();
                        Instant instantTo = ZonedDateTime.of(mDateTimeTo.getValue(), ZoneId.systemDefault()).toInstant();

                        logRequest = new DateIntervalLogRequest(
                                channel,
                                instantFrom,
                                instantTo
                        );
                        break;
                    case FILE:
                        Uri uri = mFile.getValue();
                        if (uri == null) {
                            ViewUtils.setError(mBinding.logDialogFile, true);
                        } else {
                            logRequest = new LogViewModel.FileLogRequest(uri);
                            ViewUtils.setError(mBinding.logDialogFile, false);
                        }
                        break;
                    case SINCE_OWN:
                        try {
                            long skip = Long.parseLong(mBinding.logDialogSinceown.getText().toString());
                            logRequest = new SinceOwnLogRequest(channel, skip);
                            ViewUtils.setError(mBinding.logDialogSinceown, false);
                        } catch (NumberFormatException e) {
                            ViewUtils.setError(mBinding.logDialogSinceown, true);
                        }
                        break;
                }
                if (logRequest != null) {
                    sendResult(logRequest);
                    dialog.dismiss();
                }
            } else if (which == BUTTON_NEGATIVE) {
                dialog.cancel();
            }
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog) {
            this.dismiss();
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            sendResult(null);
            dialog.dismiss();
        }

        private void sendResult(LogRequest request) {
            Fragment fragment = getParentFragment();
            Activity activity = getActivity();
            if (fragment instanceof Callback) {
                //noinspection unchecked
                ((Callback<LogRequest>) fragment).onResult(request);
            } else if (activity instanceof Callback) {
                //noinspection unchecked
                ((Callback<LogRequest>) activity).onResult(request);
            }
        }
    }
}
