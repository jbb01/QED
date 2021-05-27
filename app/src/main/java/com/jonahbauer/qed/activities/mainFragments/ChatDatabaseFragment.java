package com.jonahbauer.qed.activities.mainFragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.messageInfoSheet.MessageInfoBottomSheet;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.util.Preferences;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_CHANNEL;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_DATE;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_ID;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_MESSAGE;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_NAME;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.TABLE_NAME;


public class ChatDatabaseFragment extends QEDFragment implements CompoundButton.OnCheckedChangeListener, ChatDatabaseReceiver {
    private ChatDatabase mDatabase;
    private MessageAdapter mMessageAdapter;

    private ListView mMessageListView;
    private View mExpandable;
    private Button mSearchButton;
    private ProgressBar mSearchProgress;
    private CheckBox mChannelCheckBox;
    private CheckBox mMessageCheckBox;
    private CheckBox mNameCheckBox;
    private CheckBox mDateFromCheckBox;
    private CheckBox mDateToCheckBox;
    private CheckBox mIdCheckBox;
    private EditText mChannelEditText;
    private EditText mMessageEditText;
    private EditText mNameEditText;
    private EditText mDateFromEditText;
    private EditText mDateToEditText;
    private EditText mIdEditText;
    private TextView mHitsView;

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mMessageListView = view.findViewById(R.id.message_list_view);
        mExpandable = view.findViewById(R.id.expandable);
        mSearchButton = view.findViewById(R.id.search_button);
        mSearchProgress = view.findViewById(R.id.search_progress);
        CheckBox expandCheckBox = view.findViewById(R.id.expand_checkBox);
        mChannelCheckBox = view.findViewById(R.id.database_channel_checkbox);
        mMessageCheckBox = view.findViewById(R.id.database_message_checkbox);
        mNameCheckBox = view.findViewById(R.id.database_name_checkbox);
        mDateFromCheckBox = view.findViewById(R.id.database_dateFrom_checkbox);
        mDateToCheckBox = view.findViewById(R.id.database_dateTo_checkbox);
        mIdCheckBox = view.findViewById(R.id.database_id_checkbox);
        mChannelEditText = view.findViewById(R.id.database_channel_editText);
        mMessageEditText = view.findViewById(R.id.database_message_editText);
        mNameEditText = view.findViewById(R.id.database_name_editText);
        mDateFromEditText = view.findViewById(R.id.database_dateFrom_editText);
        mDateToEditText = view.findViewById(R.id.database_dateTo_editText);
        mIdEditText = view.findViewById(R.id.database_id_editText);
        mHitsView = view.findViewById(R.id.database_hits);
        
        mDatabase = new ChatDatabase();
        mDatabase.init(requireContext(), this);

        mMessageAdapter = new MessageAdapter(requireContext(), new ArrayList<>(), null, false, null, true);
        mMessageListView.setOnItemClickListener((adapterView, view1, i, l) -> setChecked(i, false));
        mMessageListView.setOnItemLongClickListener((adapterView, view1, i, l) -> {
            if (!mMessageListView.isItemChecked(i)) {
                int checked = mMessageListView.getCheckedItemPosition();
                if (checked != -1) setChecked(checked, false);

                setChecked(i, true);
                return true;
            } else return false;
        });

        mMessageListView.setAdapter(mMessageAdapter);
        mSearchButton.setOnClickListener(a -> search());

        expandCheckBox.setOnCheckedChangeListener(this);

        mChannelCheckBox.setOnCheckedChangeListener(this);
        mMessageCheckBox.setOnCheckedChangeListener(this);
        mNameCheckBox.setOnCheckedChangeListener(this);
        mDateFromCheckBox.setOnCheckedChangeListener(this);
        mDateToCheckBox.setOnCheckedChangeListener(this);
        mIdCheckBox.setOnCheckedChangeListener(this);

        onCheckedChanged(mChannelCheckBox, mChannelCheckBox.isChecked());
        onCheckedChanged(mMessageCheckBox, mMessageCheckBox.isChecked());
        onCheckedChanged(mNameCheckBox, mNameCheckBox.isChecked());
        onCheckedChanged(mDateFromCheckBox, mDateFromCheckBox.isChecked());
        onCheckedChanged(mDateToCheckBox, mDateToCheckBox.isChecked());
        onCheckedChanged(mIdCheckBox, mIdCheckBox.isChecked());

        mDateFromEditText.addTextChangedListener(new PatternedTextWatcher("####-##-## ##:##:##"));
        mDateToEditText.addTextChangedListener(new PatternedTextWatcher("####-##-## ##:##:##"));
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
                expand(mExpandable);
            } else {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                collapse(mExpandable);
            }
        } else if (id == R.id.database_channel_checkbox) {
            mChannelEditText.setEnabled(isChecked);
            if (isChecked) mChannelEditText.requestFocus();
        } else if (id == R.id.database_message_checkbox) {
            mMessageEditText.setEnabled(isChecked);
            if (isChecked) mMessageEditText.requestFocus();
        } else if (id == R.id.database_name_checkbox) {
            mNameEditText.setEnabled(isChecked);
            if (isChecked) mNameEditText.requestFocus();
        } else if (id == R.id.database_dateFrom_checkbox) {
            mDateFromEditText.setEnabled(isChecked);
            if (isChecked) mDateFromEditText.requestFocus();
        } else if (id == R.id.database_dateTo_checkbox) {
            mDateToEditText.setEnabled(isChecked);
            if (isChecked) mDateToEditText.requestFocus();
        } else if (id == R.id.database_id_checkbox) {
            mIdEditText.setEnabled(isChecked);
            if (isChecked) mIdEditText.requestFocus();
        }
    }

    private static void expand(@NonNull final View v) {
        v.setVisibility(View.VISIBLE);
    }

    private static void collapse(@NonNull final View v) {
        v.setVisibility(View.GONE);
    }

    private void search() {
        // TODO not SQL injection safe !!!
        if (!checkFilters()) return;

        ArrayList<String> filters = new ArrayList<>();

        StringBuilder dateFromEditTextBuilder = new StringBuilder(mDateFromEditText.getText().toString());
        while (dateFromEditTextBuilder.length() < 4) {
            dateFromEditTextBuilder.append("0");
        }
        if (dateFromEditTextBuilder.length() == 4) dateFromEditTextBuilder.append("-");

        StringBuilder dateToEditTextBuilder = new StringBuilder(mDateToEditText.getText().toString());
        while (dateToEditTextBuilder.length() < 4) {
            dateToEditTextBuilder.append("0");
        }
        if (dateToEditTextBuilder.length() == 4) dateToEditTextBuilder.append("-");


        if (mChannelCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_CHANNEL + " = '" + mChannelEditText.getText().toString() + "')");
        if (mMessageCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_MESSAGE + " LIKE '%" + mMessageEditText.getText().toString() + "%')");
        if (mNameCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_NAME + " LIKE '%" + mNameEditText.getText().toString() + "%')");
        if (mDateFromCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_DATE + " >= '" + dateFromEditTextBuilder.toString() + "')");
        if (mDateToCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_DATE + " <= '" + dateToEditTextBuilder.toString() + "')");
        if (mIdCheckBox.isChecked()) {
            String idText = mIdEditText.getText().toString();
            if (!idText.equals("")) {
                try {
                    int id = Integer.parseInt(idText);
                    if (id >= 0) idText += "-";
                } catch (NumberFormatException ignored) {
                }
                String[] id = idText.split("-");
                int idFrom = -1, idTo = -1;
                if (id.length > 0 && !id[0].isEmpty()) idFrom = Integer.parseInt(id[0]);
                if (id.length > 1 && !id[1].isEmpty()) idTo = Integer.parseInt(id[1]);

                if (idFrom != -1)
                    filters.add("(" + COLUMN_NAME_ID + " >= '" + idFrom + "')");
                if (idTo != -1)
                    filters.add("(" + COLUMN_NAME_ID + " <= '" + idTo + "')");
            }
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME);
        for (int i = 0; i < filters.size(); i++) {
            if (i == 0) sql.append(" WHERE ").append(filters.get(i));
            else sql.append(" AND ").append(filters.get(i));
        }
        sql.append(" ORDER BY ").append(COLUMN_NAME_ID).append(" ASC");

        int limit = Preferences.chat().getDbMaxResults();
        sql.append(" LIMIT ").append(limit).append(";");

        mMessageAdapter.clear();

        mSearchProgress.setVisibility(View.VISIBLE);
        mMessageListView.setVisibility(View.GONE);
        mSearchButton.setEnabled(false);

        mDatabase.query(sql.toString(), null);
    }
    
    private boolean checkFilters() {
        boolean error = false;
        boolean localError;

        mChannelCheckBox.post(() -> mChannelEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        mMessageEditText.post(() -> mMessageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        mNameEditText.post(() -> mNameEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));

        if (mDateFromCheckBox.isChecked()) {
            localError = false;
            try {
                String[] dateFrom = mDateFromEditText.getText().toString().split("[^\\d]");
                if (dateFrom.length > 1) {
                    for (int i = 0; i < 2 - dateFrom[1].length(); i++) {
                        dateFrom[1] += "0";
                    }
                    if (Integer.parseInt(dateFrom[1]) > 12 || Integer.parseInt(dateFrom[1]) == 0) localError = true;
                }
                if (dateFrom.length > 2) {
                    for (int i = 0; i < 2 - dateFrom[2].length(); i++) {
                        dateFrom[2] += "0";
                    }
                    int monthLength = 0;
                    if ("01,03,05,07,08,10,12".contains(dateFrom[1]))
                        monthLength = 31;
                    else if ("04,06,09,11".contains(dateFrom[1]))
                        monthLength = 30;
                    else if ("02".equals(dateFrom[1])) {
                        boolean leap = Integer.parseInt(dateFrom[0]) % 4 == 0 && (!(Integer.parseInt(dateFrom[0]) % 100 == 0) || Integer.parseInt(dateFrom[0]) % 400 == 0);
                        monthLength = leap ? 29 : 28;
                    }
                    if (Integer.parseInt(dateFrom[2]) > monthLength || Integer.parseInt(dateFrom[2]) == 0) localError = true;
                }
                if (dateFrom.length > 3) {
                    for (int i = 0; i < 2 - dateFrom[3].length(); i++) {
                        dateFrom[3] += "0";
                    }
                    if (Integer.parseInt(dateFrom[3]) > 23) localError = true;
                }
                if (dateFrom.length > 4) {
                    for (int i = 0; i < 2 - dateFrom[4].length(); i++) {
                        dateFrom[4] += "0";
                    }
                    if (Integer.parseInt(dateFrom[4]) > 59) localError = true;
                }
                if (dateFrom.length > 5) {
                    for (int i = 0; i < 2 - dateFrom[5].length(); i++) {
                        dateFrom[5] += "0";
                    }
                    if (Integer.parseInt(dateFrom[5]) > 59) localError = true;
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                mDateFromEditText.post(() -> mDateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                mDateFromEditText.post(() -> mDateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            mDateFromEditText.post(() -> mDateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        if (mDateToCheckBox.isChecked()) {
            localError = false;
            try {
                String[] dateTo = mDateToEditText.getText().toString().split("[^\\d]");
                if (dateTo.length > 1) {
                    for (int i = 0; i < 2 - dateTo[1].length(); i++) {
                        dateTo[1] += "0";
                    }
                    if (Integer.parseInt(dateTo[1]) > 12) localError = true;
                }
                if (dateTo.length > 2) {
                    for (int i = 0; i < 2 - dateTo[2].length(); i++) {
                        dateTo[2] += "0";
                    }
                    int monthLength = 0;
                    if ("01,03,05,07,08,10,12".contains(dateTo[1]))
                        monthLength = 31;
                    else if ("04,06,09,11".contains(dateTo[1]))
                        monthLength = 30;
                    else if ("02".equals(dateTo[1])) {
                        boolean leap = Integer.parseInt(dateTo[0]) % 4 == 0 && (!(Integer.parseInt(dateTo[0]) % 100 == 0) || Integer.parseInt(dateTo[0]) % 400 == 0);
                        monthLength = leap ? 29 : 28;
                    }
                    if (Integer.parseInt(dateTo[2]) > monthLength) localError = true;
                }
                if (dateTo.length > 3) {
                    for (int i = 0; i < 2 - dateTo[3].length(); i++) {
                        dateTo[3] += "0";
                    }
                    if (Integer.parseInt(dateTo[3]) > 23) localError = true;
                }
                if (dateTo.length > 4) {
                    for (int i = 0; i < 2 - dateTo[4].length(); i++) {
                        dateTo[4] += "0";
                    }
                    if (Integer.parseInt(dateTo[4]) > 59) localError = true;
                }
                if (dateTo.length > 5) {
                    for (int i = 0; i < 2 - dateTo[5].length(); i++) {
                        dateTo[5] += "0";
                    }
                    if (Integer.parseInt(dateTo[5]) > 59) localError = true;
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                mDateToEditText.post(() -> mDateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                mDateToEditText.post(() -> mDateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            mDateToEditText.post(() -> mDateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        if (mIdCheckBox.isChecked()) {
            localError = false;
            boolean isNumber = true;
            try {
                Integer.valueOf(mIdEditText.getText().toString());
            } catch (NumberFormatException e) {
                isNumber = false;
            }
            try {
                String idEditTextText = mIdEditText.getText().toString();
                if (!idEditTextText.equals("")) {
                    if (!(idEditTextText.matches("\\d*-\\d*") || isNumber)) localError = true;
                    else {
                        String[] ids = idEditTextText.split("-");
                        if (ids.length > 0 && !ids[0].isEmpty())
                            Integer.valueOf(ids[0]);
                        if (ids.length > 1 && !ids[1].isEmpty())
                            Integer.valueOf(ids[1]);
                    }
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                mIdEditText.post(() -> mIdEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                mIdEditText.post(() -> mIdEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            mIdEditText.post(() -> mIdEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        return !error;
    }

    @Override
    public void onReceiveResult(@NonNull List<Message> messages) {
        String hits = messages.size() + " " + getString(R.string.database_hits);

        mHandler.post(() -> {
            mSearchProgress.setVisibility(View.GONE);
            mMessageListView.setVisibility(View.VISIBLE);
            mSearchButton.setEnabled(true);
            mHitsView.setText(hits);
        });

        mMessageListView.post(() -> {
            messages.forEach(mMessageAdapter::add);
            mMessageAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDatabaseError() {
        mHandler.post(() -> {
            mSearchProgress.setVisibility(View.GONE);
            mMessageListView.setVisibility(View.VISIBLE);
            mSearchButton.setEnabled(true);
        });
    }

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    @Override
    public void onDestroy() {
        if (mDatabase != null) mDatabase.close();
        super.onDestroy();
    }
}
