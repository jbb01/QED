package com.jonahbauer.qed.activities.mainFragments;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.messageInfoSheet.MessageInfoBottomSheet;
import com.jonahbauer.qed.databinding.FragmentChatDatabaseBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.viewmodel.MessageListViewModel;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatDatabaseFragment extends QEDFragment implements CompoundButton.OnCheckedChangeListener {
    private MessageAdapter mMessageAdapter;
    private MessageListViewModel mMessageListViewModel;

    private Calendar mDateFrom;
    private Calendar mDateTo;

    private FragmentChatDatabaseBinding mBinding;

    @NonNull
    public static ChatDatabaseFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_chat_database);

        ChatDatabaseFragment fragment = new ChatDatabaseFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentChatDatabaseBinding.bind(view);
        mMessageListViewModel = new ViewModelProvider(this).get(MessageListViewModel.class);

        mDateFrom = Calendar.getInstance();
        mDateTo = Calendar.getInstance();

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
        ViewUtils.setupTimeEditText(mBinding.databaseTimeFromEditText, mDateFrom);
        ViewUtils.setupTimeEditText(mBinding.databaseTimeToEditText, mDateTo);

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
        mBinding.messageListView.setItemChecked(position, value);

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
                        MessageInfoBottomSheet sheet = MessageInfoBottomSheet.newInstance(msg);
                        sheet.show(getChildFragmentManager(), sheet.getTag());
                    }

                    return false;
                });

                if (msg != null) toolbar.setTitle(msg.getName());
            } else {
                mainActivity.returnAltToolbar();
            }
        }
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
        Date fromDate = null;
        Date toDate = null;
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
            fromDate = mDateFrom.getTime();
        }

        if (mBinding.databaseDateToCheckbox.isChecked()) {
            toDate = mDateTo.getTime();
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
            setErrorDrawable(mBinding.databaseIdEditText, !idValid);
        } else {
            setErrorDrawable(mBinding.databaseIdEditText, false);
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
    
    private void setErrorDrawable(EditText editText, boolean error) {
        editText.post(() -> {
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, error ? R.drawable.ic_error : 0, 0);
        });
    }
}
