package com.jonahbauer.qed.activities.mainFragments;

import static com.jonahbauer.qed.model.viewmodel.LogViewModel.DateIntervalLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.DateRecentLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.FILE;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.SINCE_OWN;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.OnlineLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.PostIntervalLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.PostRecentLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.SinceOwnLogRequest;

import android.app.AlertDialog;
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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogLogModeBinding;
import com.jonahbauer.qed.databinding.FragmentLogBinding;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.viewmodel.LogViewModel;
import com.jonahbauer.qed.model.viewmodel.LogViewModel.LogRequest;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogFragment extends QEDFragment {
    private static final String ARGUMENT_LOG_REQUEST = "logRequest";

    private MessageAdapter mMessageAdapter;
    private FragmentLogBinding mBinding;

    private LogViewModel mLogViewModel;

    private LogRequest mLogRequest;

    private final MutableLiveData<Uri> mFile = new MutableLiveData<>();
    private ActivityResultLauncher<String[]> mFileChooser;


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

        mFileChooser = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                mFile::setValue
        );
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

        mBinding.subtitle.setOnClickListener(v -> showDialog());

        setHasOptionsMenu(true);

        mLogViewModel.getDownloadStatus().observe(getViewLifecycleOwner(), pair -> {
            if (pair == null) {
                mBinding.setDownloadStatus(0);
                mBinding.setDownloadText(getString(R.string.log_status_download_pending));
            } else {
                double done = pair.leftLong() / 1048576d;
                mBinding.setDownloadStatus(1);
                mBinding.setDownloadText(getString(R.string.log_status_downloading, done));
            }
        });

        mLogViewModel.getParseStatus().observe(getViewLifecycleOwner(), pair -> {
            mBinding.setDownloadStatus(2);
            if (pair == null) {
                mBinding.setParseStatus(0);
                mBinding.setParseText(getString(R.string.log_status_parse_pending));
            } else {
                long done = pair.leftLong();
                long total = pair.rightLong();
                if (done == total) {
                    mBinding.setParseStatus(2);
                } else {
                    mBinding.setParseStatus(1);
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
            Database.getInstance(requireContext()).messageDao()
                    .insert(mMessageAdapter.getData())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Snackbar.make(requireView(), R.string.saved, Snackbar.LENGTH_SHORT).show();
                                mBinding.setSaving(false);
                                item.setEnabled(true);
                            },
                            (e) -> {
                                Snackbar.make(requireView(), R.string.save_error, Snackbar.LENGTH_SHORT).show();
                                mBinding.setSaving(false);
                                item.setEnabled(true);
                            }
                    );
            return true;
        }
        return false;
    }

    //<editor-fold desc="Lifecycle" defaultstate="collapsed">
    @Override
    public Boolean onDrop(boolean force) {
        return true;
//        if (getActivity() == null) return true;
//
//        @QEDChatPages.Status
//        int asyncStatus = QEDChatPages.getStatus(getClass().toString());
//
//        boolean downloadRunning = (asyncStatus & (QEDChatPages.STATUS_DOWNLOAD_RUNNING | QEDChatPages.STATUS_DOWNLOAD_PENDING)) != 0;
//        boolean parseRunning = (asyncStatus & (QEDChatPages.STATUS_PARSE_THREAD_ALIVE)) != 0;
//
//        final MainActivity mainActivity = (MainActivity) getActivity();
//
//        if (force && downloadRunning) { // force closing
//            Toast.makeText(mainActivity, R.string.log_download_continue_in_background_toast, Toast.LENGTH_LONG).show();
//            downloadContinueInBackground();
//            return true;
//        } else if (downloadRunning) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
//            builder.setMessage(R.string.log_confirm_exit);
//
//            // do nothing
//            builder.setPositiveButton(R.string.log_confirm_exit_stay, (dialog, which) -> {
//                mainActivity.onDropResult(false);
//                dialog.dismiss();
//            });
//
//            // interrupt download manger, ui async task and parse thread
//            builder.setNegativeButton(R.string.log_confirm_exit_cancel, (dialog, which) -> {
//                mainActivity.onDropResult(true);
//                downloadGoAndCancel();
//                dialog.dismiss();
//            });
//
//            // interrupt ui async task
//            builder.setNeutralButton(R.string.log_confirm_exit_continue_in_background, (dialog, which) -> {
//                mainActivity.onDropResult(true);
//                downloadContinueInBackground();
//                dialog.dismiss();
//            });
//
//            // stay on cancel
//            builder.setOnCancelListener(dialog -> {
//                mainActivity.onDropResult(false);
//                dialog.dismiss();
//            });
//
//            mDropAlertDialog = builder.show();
//            return null;
//        } else if (parseRunning) {
//            QEDChatPages.interrupt(getClass().toString());
//            return true;
//        } else {
//            return true;
//        }
    }

//    private void downloadContinueInBackground() {
//        DownloadListener listener = mLogDownload.getListener();
//        if (listener != null)
//            mLogDownload.detachListener(listener);
//    }
//
//    private void downloadGoAndCancel() {
//        QEDChatPages.interrupt(getClass().toString());
//    }
    //</editor-fold>

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.title_activity_log_dialog);

        AlertDialogLogModeBinding binding = AlertDialogLogModeBinding.inflate(LayoutInflater.from(requireContext()));
        binding.setMode(POST_RECENT);
        builder.setView(binding.getRoot());

        binding.logDialogModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getItemAtPosition(position);

                if (LogFragment.this.getString(R.string.log_request_mode_postrecent).equals(item)) {
                    binding.setMode(POST_RECENT);
                } else if (LogFragment.this.getString(R.string.log_request_mode_daterecent).equals(item)) {
                    binding.setMode(DATE_RECENT);
                } else if (LogFragment.this.getString(R.string.log_request_mode_dateinterval).equals(item)) {
                    binding.setMode(DATE_INTERVAL);
                } else if (LogFragment.this.getString(R.string.log_request_mode_postinterval).equals(item)) {
                    binding.setMode(POST_INTERVAL);
                } else if (getString(R.string.log_request_mode_sinceown).equals(item)) {
                    binding.setMode(SINCE_OWN);
                } else if (LogFragment.this.getString(R.string.log_request_mode_file).equals(item)) {
                    binding.setMode(FILE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Calendar dateFrom = Calendar.getInstance();
        Calendar dateTo = Calendar.getInstance();
        ViewUtils.setupDateEditText(binding.logDialogDateintervalFrom, dateFrom);
        ViewUtils.setupDateEditText(binding.logDialogDateintervalTo, dateTo);

        binding.logDialogFile.setOnClickListener(v -> mFileChooser.launch(new String[] {"*/*"}));
        Observer<Uri> fileObserver = uri -> binding.logDialogFile.setText(uri != null ? uri.getPath() : "");
        mFile.observe(getViewLifecycleOwner(), fileObserver);

        AlertDialog dialog = builder.create();

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), (DialogInterface.OnClickListener) null);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (d, which) -> {});
        dialog.setOnDismissListener(d -> {
            mFile.removeObserver(fileObserver);
        });

        // positive button click listener is set directly on the button after dialog is shown
        // to prevent dialog from dismissing when input could not be validated
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                LogRequest logRequest = null;
                String channel = Preferences.chat().getChannel();
                switch (binding.getMode()) {
                    case POST_RECENT:
                        try {
                            long last = Long.parseLong(binding.logDialogPostrecent.getText().toString());
                            logRequest = new PostRecentLogRequest(channel, last);
                            binding.logDialogPostrecent.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } catch (NumberFormatException e) {
                            binding.logDialogPostrecent.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                        }
                        break;
                    case DATE_RECENT:
                        try {
                            long last = Long.parseLong(binding.logDialogDaterecent.getText().toString());
                            logRequest = new DateRecentLogRequest(channel, last, TimeUnit.HOURS);
                            binding.logDialogDaterecent.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } catch (NumberFormatException e) {
                            binding.logDialogDaterecent.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                        }
                        break;
                    case POST_INTERVAL:
                        long from = 0, to = 0;
                        boolean error = false;
                        try {
                            from = Long.parseLong(binding.logDialogPostintervalFrom.getText().toString());
                            binding.logDialogPostintervalFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } catch (NumberFormatException e) {
                            binding.logDialogPostintervalFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                            error = true;
                        }
                        try {
                            to = Long.parseLong(binding.logDialogPostintervalTo.getText().toString());
                            binding.logDialogPostintervalTo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } catch (NumberFormatException e) {
                            binding.logDialogPostintervalTo.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                            error = true;
                        }

                        if (error) break;

                        logRequest = new PostIntervalLogRequest(channel, from, to);
                        break;
                    case DATE_INTERVAL:
                        logRequest = new DateIntervalLogRequest(channel, dateFrom.getTime(), dateTo.getTime());
                        break;
                    case FILE:
                        Uri uri = mFile.getValue();
                        if (uri == null) {
                            binding.logDialogFile.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                        } else {
                            logRequest = new LogViewModel.FileLogRequest(uri);
                            binding.logDialogFile.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        }
                        break;
                    case SINCE_OWN:
                        try {
                            long skip = Long.parseLong(binding.logDialogSinceown.getText().toString());
                            logRequest = new SinceOwnLogRequest(channel, skip);
                            binding.logDialogSinceown.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } catch (NumberFormatException e) {
                            binding.logDialogSinceown.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error, 0);
                        }
                        break;
                }
                if (logRequest != null) {
                    reload(logRequest);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

}
