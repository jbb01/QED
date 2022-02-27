package com.jonahbauer.qed.activities.mainFragments;

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentChatDatabaseBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.room.MessageDao;
import com.jonahbauer.qed.model.viewmodel.MessageListViewModel;
import com.jonahbauer.qed.util.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.time.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ChatDatabaseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String LOG_TAG = ChatDatabaseFragment.class.getName();

    private MessageAdapter mMessageAdapter;
    private MessageListViewModel mMessageListViewModel;

    private MutableLiveData<LocalDate> mDateFrom;
    private MutableLiveData<LocalTime> mTimeFrom;
    private MutableLiveData<LocalDate> mDateTo;
    private MutableLiveData<LocalTime> mTimeTo;

    private FragmentChatDatabaseBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Search the database for possible misassigned dates due to daylight savings time
        // and try to fix them
        Function<Message, Message> dateFixer = MessageUtils.dateFixer();
        MessageDao messageDao = Database.getInstance(getContext()).messageDao();
        //noinspection ResultOfMethodCallIgnored
        messageDao.possibleDateErrors()
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.computation())
                  .map(list -> list.stream()
                                   .map(msg -> {
                                       Message out = dateFixer.apply(msg);
                                       if (out == msg) return null;
                                       else return out;
                                   })
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList())
                  )
                  .flatMapCompletable(messageDao::insert)
                  .subscribe(
                          () -> {},
                          err -> Log.e(LOG_TAG, "Error fixing dates.", err)
                  );

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentChatDatabaseBinding.inflate(inflater, container, false);
        mMessageListViewModel = ViewUtils.getViewModelProvider(this).get(MessageListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindows(view);

        mDateFrom = new MutableLiveData<>(LocalDate.now());
        mTimeFrom = new MutableLiveData<>(LocalTime.now());
        mDateTo = new MutableLiveData<>(LocalDate.now());
        mTimeTo = new MutableLiveData<>(LocalTime.now());

        // setup list view
        mMessageAdapter = new MessageAdapter(requireContext(), new ArrayList<>(), null, false, null, true);
        mBinding.messageListView.setOnItemClickListener((parent, v, position, id) -> setChecked(position, false));
        mBinding.messageListView.setOnItemLongClickListener((parent, v, position, id) -> {
            if (!mBinding.messageListView.isItemChecked(position)) {
                int checked = mBinding.messageListView.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(position, true);
                return true;
            } else return false;
        });
        mBinding.messageListView.setAdapter(mMessageAdapter);

        mBinding.expandCheckBox.setOnCheckedChangeListener(this);
        mBinding.searchButton.setOnClickListener(v -> search());

        mBinding.databaseChannelCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseMessageCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseNameCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseDateFromCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseDateToCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseIdCheckbox.setOnCheckedChangeListener(this);

        onCheckedChanged(mBinding.databaseChannelCheckbox, mBinding.databaseChannelCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseMessageCheckbox, mBinding.databaseMessageCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseNameCheckbox, mBinding.databaseNameCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseDateFromCheckbox, mBinding.databaseDateFromCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseDateToCheckbox, mBinding.databaseDateToCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseIdCheckbox, mBinding.databaseIdCheckbox.isChecked());

        ViewUtils.setupDateEditText(mBinding.databaseDateFromEditText, mDateFrom);
        ViewUtils.setupDateEditText(mBinding.databaseDateToEditText, mDateTo);
        ViewUtils.setupTimeEditText(mBinding.databaseTimeFromEditText, mTimeFrom);
        ViewUtils.setupTimeEditText(mBinding.databaseTimeToEditText, mTimeTo);

        mMessageListViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            mBinding.setStatus(messages.getCode());

            mMessageAdapter.clear();
            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                mMessageAdapter.addAll(messages.getValue());

                mBinding.setHits(getString(R.string.hits, messages.getValue().size()));
            }
            mMessageAdapter.notifyDataSetChanged();
        });
    }

    /**
     * Sets the checked item in the list view and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     * @param value if the item is checked or not
     */
    private void setChecked(int position, boolean value) {
        MessageUtils.setChecked(
                this,
                mBinding.messageListView,
                mMessageAdapter,
                (mode, msg) -> NavHostFragment.findNavController(this)
                                              .navigate(ChatDatabaseFragmentDirections.showMessage(msg)),
                null,
                position,
                value
        );
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.expand_checkBox) {
            if (isChecked) {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.expand(mBinding.expandable);
            } else {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.collapse(mBinding.expandable);
            }
        } else if (id == R.id.database_channel_checkbox) {
            mBinding.databaseChannelEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseChannelEditText.requestFocus();
        } else if (id == R.id.database_message_checkbox) {
            mBinding.databaseMessageEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseMessageEditText.requestFocus();
        } else if (id == R.id.database_name_checkbox) {
            mBinding.databaseNameEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseNameEditText.requestFocus();
        } else if (id == R.id.database_dateFrom_checkbox) {
            mBinding.databaseDateFromEditText.setEnabled(isChecked);
            mBinding.databaseTimeFromEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseDateFromEditText.requestFocus();
        } else if (id == R.id.database_dateTo_checkbox) {
            mBinding.databaseDateToEditText.setEnabled(isChecked);
            mBinding.databaseTimeToEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseDateToEditText.requestFocus();
        } else if (id == R.id.database_id_checkbox) {
            mBinding.databaseIdEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseIdEditText.requestFocus();
        }
    }

    private void search() {
        if (!checkFilters()) return;

        String channel = null;
        String message = null;
        String name = null;
        Instant fromDate = null;
        Instant toDate = null;
        Long fromId = null;
        Long toId = null;
        long limit = Preferences.chat().getDbMaxResults();

        if (mBinding.databaseChannelCheckbox.isChecked()) {
            channel = mBinding.databaseChannelEditText.getText().toString();
        }

        if (mBinding.databaseMessageCheckbox.isChecked()) {
            message = mBinding.databaseMessageEditText.getText().toString();
        }

        if (mBinding.databaseNameCheckbox.isChecked()) {
            name = mBinding.databaseNameEditText.getText().toString();
        }
        
        if (mBinding.databaseDateFromCheckbox.isChecked()) {
            LocalDate date = mDateFrom.getValue();
            LocalTime time = mTimeFrom.getValue();
            fromDate = ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant();
        }

        if (mBinding.databaseDateToCheckbox.isChecked()) {
            LocalDate date = mDateTo.getValue();
            LocalTime time = mTimeTo.getValue();
            toDate = ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant();
        }

        if (mBinding.databaseIdCheckbox.isChecked()) {
            String str = mBinding.databaseIdEditText.getText().toString();
            Pattern pattern = Pattern.compile("(\\d+)?(-)?(\\d+)?");
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                String from = matcher.group(1);
                boolean single = matcher.group(2) == null;
                String to = matcher.group(3);

                if (from != null) {
                    try {
                        fromId = Long.parseLong(from);
                    } catch (NumberFormatException ignored) {}
                }

                if (to != null) {
                    try {
                        toId = Long.parseLong(to);
                    } catch (NumberFormatException ignored) {}
                }

                if (single) {
                    toId = fromId;
                }
            }
        }

        mMessageListViewModel.load(channel, message, name, fromDate, toDate, fromId, toId, limit);
    }
    
    private boolean checkFilters() {
        boolean valid = true;

        // id
        if (mBinding.databaseIdCheckbox.isChecked()) {
            boolean idValid = checkId(mBinding.databaseIdEditText.getText().toString());
            valid = idValid;
            ViewUtils.setError(mBinding.databaseIdEditText, !idValid);
        } else {
            ViewUtils.setError(mBinding.databaseIdEditText, false);
        }

        return valid;
    }

    private boolean checkId(String str) {
        Pattern pattern = Pattern.compile("(\\d+)?-?(\\d+)?");
        Matcher matcher = pattern.matcher(str);

        if (matcher.matches()) {
            String from = matcher.group(1);
            String to = matcher.group(2);

            if (from == null && to == null) {
                return false;
            }

            try {
                if (from != null) Long.parseLong(from);
                if (to != null) Long.parseLong(to);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}
