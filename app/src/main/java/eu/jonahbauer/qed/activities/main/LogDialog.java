package eu.jonahbauer.qed.activities.main;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.AlertDialogLogModeBinding;
import eu.jonahbauer.qed.ui.CustomArrayAdapter;
import eu.jonahbauer.qed.model.LogRequest;
import eu.jonahbauer.qed.model.LogRequest.DateIntervalLogRequest;
import eu.jonahbauer.qed.model.LogRequest.DateRecentLogRequest;
import eu.jonahbauer.qed.model.LogRequest.FileLogRequest;
import eu.jonahbauer.qed.model.LogRequest.Mode;
import eu.jonahbauer.qed.model.LogRequest.PostIntervalLogRequest;
import eu.jonahbauer.qed.model.LogRequest.PostRecentLogRequest;
import eu.jonahbauer.qed.model.LogRequest.SinceOwnLogRequest;
import eu.jonahbauer.qed.util.TimeUtils;
import eu.jonahbauer.qed.util.ViewUtils;

import java.text.MessageFormat;
import java.time.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LogDialog extends DialogFragment implements AdapterView.OnItemSelectedListener {
    private static final String ARGUMENTS_LOG_REQUEST = "log_request";
    private static final String SAVED_FILE = "file";
    private static final String SAVED_DATE_FROM = "date_from";
    private static final String SAVED_TIME_FROM = "time_from";
    private static final String SAVED_DATE_TO = "date_to";
    private static final String SAVED_TIME_TO = "time_to";

    // date interval
    private final MutableLiveData<LocalDateTime> mDateTimeFrom = new MutableLiveData<>(LocalDateTime.now());
    private final MutableLiveData<LocalDateTime> mDateTimeTo = new MutableLiveData<>(LocalDateTime.now());

    // file
    private final ActivityResultLauncher<String[]> mFileChooser;
    private final MutableLiveData<Uri> mFile = new MutableLiveData<>();

    private AlertDialogLogModeBinding mBinding;

    public static LogDialog newInstance(LogRequest logRequest) {
        var fragment = new LogDialog();
        var arguments = new Bundle();
        arguments.putParcelable(ARGUMENTS_LOG_REQUEST, logRequest);
        fragment.setArguments(arguments);
        return fragment;
    }

    public LogDialog() {
        mFileChooser = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                mFile::setValue
        );
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFile.setValue(savedInstanceState.getParcelable(SAVED_FILE));

            var dateFrom = LocalDate.ofEpochDay(savedInstanceState.getLong(SAVED_DATE_FROM));
            var timeFrom = LocalTime.ofNanoOfDay(savedInstanceState.getLong(SAVED_TIME_FROM));
            var dateTo = LocalDate.ofEpochDay(savedInstanceState.getLong(SAVED_DATE_TO));
            var timeTo = LocalTime.ofNanoOfDay(savedInstanceState.getLong(SAVED_TIME_TO));
            mDateTimeFrom.setValue(LocalDateTime.of(dateFrom, timeFrom));
            mDateTimeTo.setValue(LocalDateTime.of(dateTo, timeTo));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.title_activity_log_dialog);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, (d, w) -> {}); // listener is set in onStart
        builder.setNegativeButton(R.string.cancel, null);
        builder.setView(createView(requireContext()));
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        var dialog = (AlertDialog) requireDialog();
        var positiveButton = Objects.requireNonNull(dialog.getButton(BUTTON_POSITIVE));
        positiveButton.setOnClickListener(this::onPositiveClicked);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_FILE, mFile.getValue());

        var dateTimeFrom = Objects.requireNonNull(mDateTimeFrom.getValue());
        var dateTimeTo = Objects.requireNonNull(mDateTimeTo.getValue());
        outState.putLong(SAVED_DATE_FROM, dateTimeFrom.toLocalDate().toEpochDay());
        outState.putLong(SAVED_TIME_FROM, dateTimeFrom.toLocalTime().toNanoOfDay());
        outState.putLong(SAVED_DATE_TO, dateTimeTo.toLocalDate().toEpochDay());
        outState.putLong(SAVED_TIME_TO, dateTimeTo.toLocalTime().toNanoOfDay());
    }

    @NonNull
    public View createView(@NonNull Context context) {
        mBinding = AlertDialogLogModeBinding.inflate(getLayoutInflater());

        // setup mode spinner
        var adapter = new CustomArrayAdapter<>(context, android.R.layout.simple_spinner_item, Mode.values());
        adapter.setToString((mode) -> context.getString(mode.getStringRes()));
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        mBinding.logDialogModeSpinner.setAdapter(adapter);
        mBinding.logDialogModeSpinner.setSelection(0);
        mBinding.logDialogModeSpinner.setOnItemSelectedListener(this);

        // setup file
        mFile.observe(this, uri -> mBinding.logDialogFile.setText(uri != null ? uri.getPath() : ""));
        mBinding.logDialogFile.setOnClickListener(v -> {
            mFileChooser.launch(new String[]{"*/*"});
        });

        // setup date interval
        ViewUtils.setupDateEditText(this, mBinding.logDialogDateintervalDateFrom, mDateTimeFrom);
        ViewUtils.setupTimeEditText(this, mBinding.logDialogDateintervalTimeFrom, mDateTimeFrom);
        ViewUtils.setupDateEditText(this, mBinding.logDialogDateintervalDateTo, mDateTimeTo);
        ViewUtils.setupTimeEditText(this, mBinding.logDialogDateintervalTimeTo, mDateTimeTo);

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

        var arguments = getArguments();
        if (arguments != null) {
            var logRequest = (LogRequest) arguments.getParcelable(ARGUMENTS_LOG_REQUEST);
            if (logRequest != null) updateView(logRequest);
        }

        return mBinding.getRoot();
    }

    private void updateView(@NonNull LogRequest request) {
        // unpack file log request
        if (request instanceof FileLogRequest) {
            var fileRequest = (FileLogRequest) request;
            if (fileRequest.getRoot() != null) request = fileRequest.getRoot();
        }

        var mode = request.getMode();
        mBinding.setMode(mode);
        mBinding.logDialogModeSpinner.setSelection(mode.ordinal());
        if (request instanceof LogRequest.OnlineLogRequest) {
            var onlineRequest = (LogRequest.OnlineLogRequest) request;
            mBinding.logDialogChannel.setText(onlineRequest.getChannel());
            if (request instanceof PostRecentLogRequest) {
                var postRecentRequest = (PostRecentLogRequest) request;
                mBinding.logDialogPostrecent.setText(String.valueOf(postRecentRequest.getLast()));
            } else if (request instanceof PostIntervalLogRequest) {
                var postIntervalRequest = (PostIntervalLogRequest) request;
                mBinding.logDialogPostintervalFrom.setText(String.valueOf(postIntervalRequest.getFrom()));
                mBinding.logDialogPostintervalTo.setText(String.valueOf(postIntervalRequest.getTo()));
            } else if (request instanceof DateRecentLogRequest) {
                var dateRecentRequest = (DateRecentLogRequest) request;
                mBinding.logDialogDaterecent.setText(String.valueOf(TimeUnit.SECONDS.toHours(dateRecentRequest.getSeconds())));
            } else if (request instanceof DateIntervalLogRequest) {
                var dateIntervalRequest = (DateIntervalLogRequest) request;
                mDateTimeFrom.setValue(TimeUtils.localFromInstant(dateIntervalRequest.getFrom()));
                mDateTimeTo.setValue(TimeUtils.localFromInstant(dateIntervalRequest.getTo()));
            } else if (request instanceof SinceOwnLogRequest) {
                var sinceOwnRequest = (SinceOwnLogRequest) request;
                mBinding.logDialogSinceown.setText(String.valueOf(sinceOwnRequest.getSkip()));
            }
        } else if (request instanceof FileLogRequest) {
            var fileRequest = (FileLogRequest) request;
            mFile.setValue(fileRequest.getFile());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        Mode item = (Mode) spinner.getItemAtPosition(position);
        mBinding.setMode(item);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void onPositiveClicked(View v) {
        var request = loadRequest();
        if (request != null) {
            sendResult(request);
            dismiss();
        }
    }

    private void sendResult(LogRequest request) {
        if (request != null) {
            try {
                NavHostFragment.findNavController(this)
                               .getBackStackEntry(R.id.nav_chat_log)
                               .getSavedStateHandle()
                               .set(LogFragment.LOG_REQUEST_KEY, request);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private @Nullable LogRequest loadRequest() {
        var channel = mBinding.logDialogChannel.getText().toString();
        var mode = (Mode) mBinding.logDialogModeSpinner.getSelectedItem();
        if (mode == null) return null;

        try {
            switch (mode) {
                case POST_RECENT: {
                    long last = parseLong(mBinding.logDialogPostrecent, null);
                    return new PostRecentLogRequest(channel, last);
                }
                case DATE_RECENT: {
                    long last = parseLong(mBinding.logDialogDaterecent, null);
                    return new DateRecentLogRequest(channel, last, TimeUnit.HOURS);
                }
                case POST_INTERVAL: {
                    long from = parseLong(mBinding.logDialogPostintervalFrom, null);
                    long to = parseLong(mBinding.logDialogPostintervalTo, null);
                    return new PostIntervalLogRequest(channel, from, to);
                }
                case DATE_INTERVAL: {
                    var instantFrom = validateNonNull(
                            TimeUtils.instantFromLocal(mDateTimeFrom.getValue()),
                            mBinding.logDialogDateintervalTimeFrom
                    );
                    var instantTo = validateNonNull(
                            TimeUtils.instantFromLocal(mDateTimeTo.getValue()),
                            mBinding.logDialogDateintervalTimeTo
                    );
                    return new DateIntervalLogRequest(channel, instantFrom, instantTo);
                }
                case FILE: {
                    var uri = validateNonNull(mFile.getValue(), mBinding.logDialogFile);
                    return new FileLogRequest(uri);
                }
                case SINCE_OWN: {
                    long skip = parseLong(mBinding.logDialogSinceown, 0L);
                    return new SinceOwnLogRequest(channel, skip);
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }

        return null;
    }

    private static long parseLong(@NonNull EditText editText, @Nullable Long fallback) {
        long out;
        try {
            out = Long.parseLong(editText.getText().toString());
        } catch (NumberFormatException e) {
            if (fallback != null) {
                out = fallback;
            } else {
                ViewUtils.setError(editText, true);
                throw new IllegalArgumentException(e);
            }
        }

        ViewUtils.setError(editText, false);
        return out;
    }

    @NonNull
    private static <T> T validateNonNull(T value, @NonNull EditText input) {
        if (value == null) {
            ViewUtils.setError(input, true);
            throw new IllegalArgumentException(new NullPointerException());
        } else {
            ViewUtils.setError(input, false);
            return value;
        }
    }
}
