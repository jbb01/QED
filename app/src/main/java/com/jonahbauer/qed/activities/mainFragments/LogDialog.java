package com.jonahbauer.qed.activities.mainFragments;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogLogModeBinding;
import com.jonahbauer.qed.layoutStuff.CustomArrayAdapter;
import com.jonahbauer.qed.model.LogRequest;
import com.jonahbauer.qed.model.LogRequest.DateIntervalLogRequest;
import com.jonahbauer.qed.model.LogRequest.DateRecentLogRequest;
import com.jonahbauer.qed.model.LogRequest.FileLogRequest;
import com.jonahbauer.qed.model.LogRequest.Mode;
import com.jonahbauer.qed.model.LogRequest.PostIntervalLogRequest;
import com.jonahbauer.qed.model.LogRequest.PostRecentLogRequest;
import com.jonahbauer.qed.model.LogRequest.SinceOwnLogRequest;
import com.jonahbauer.qed.util.TimeUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LogDialog extends DialogFragment implements AdapterView.OnItemSelectedListener,
                                                         DialogInterface.OnClickListener,
                                                         DialogInterface.OnCancelListener {

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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.title_activity_log_dialog);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(createView(requireContext()));
        return builder.create();
    }

    @NonNull
    public View createView(@NonNull Context context) {
        mBinding = AlertDialogLogModeBinding.inflate(getLayoutInflater());

        // setup mode spinner
        var adapter = new CustomArrayAdapter<>(context, android.R.layout.simple_spinner_item, Mode.values());
        adapter.setToString((mode) -> context.getString(mode.toStringRes()));
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        mBinding.logDialogModeSpinner.setAdapter(adapter);
        mBinding.logDialogModeSpinner.setSelection(0);
        mBinding.logDialogModeSpinner.setOnItemSelectedListener(this);

        // setup file
        mBinding.logDialogFile.setOnClickListener(v -> {
            mFileChooser.launch(new String[]{"*/*"});
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
        Mode item = (Mode) spinner.getItemAtPosition(position);
        mBinding.setMode(item);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            String channel = mBinding.logDialogChannel.getText().toString();
            Mode mode = (Mode) mBinding.logDialogModeSpinner.getSelectedItem();

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
                        logRequest = new FileLogRequest(uri);
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
    public void onCancel(@NonNull DialogInterface dialog) {
        sendResult(null);
        dialog.dismiss();
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
}
