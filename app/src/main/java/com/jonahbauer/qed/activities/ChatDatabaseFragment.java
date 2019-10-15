package com.jonahbauer.qed.activities;

import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.ChatDatabaseReceiver;
import com.jonahbauer.qed.layoutStuff.TileDrawable;
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

@SuppressWarnings("ConstantConditions")
public class ChatDatabaseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, ChatDatabaseReceiver {
    private ChatDatabase database;
    private MessageAdapter messageAdapter;

    private ListView messageListView;
    private View expand;
    private Button searchButton;
    private ProgressBar searchProgress;
    private CheckBox channelCheckBox;
    private CheckBox messageCheckBox;
    private CheckBox nameCheckBox;
    private CheckBox dateFromCheckBox;
    private CheckBox dateToCheckBox;
    private CheckBox idCheckBox;
    private EditText channelEditText;
    private EditText messageEditText;
    private EditText nameEditText;
    private EditText dateFromEditText;
    private EditText dateToEditText;
    private EditText idEditText;
    private TextView hitsView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_chat_database, container, false);

        messageListView = view.findViewById(R.id.message_list_view);
        ImageView background = view.findViewById(R.id.background);
        expand = view.findViewById(R.id.expand);
        searchButton = view.findViewById(R.id.search_button);
        searchProgress = view.findViewById(R.id.search_progress);
        CheckBox expandCheckBox = view.findViewById(R.id.expand_checkBox);
        channelCheckBox = view.findViewById(R.id.database_channel_checkbox);
        messageCheckBox = view.findViewById(R.id.database_message_checkbox);
        nameCheckBox = view.findViewById(R.id.database_name_checkbox);
        dateFromCheckBox = view.findViewById(R.id.database_dateFrom_checkbox);
        dateToCheckBox = view.findViewById(R.id.database_dateTo_checkbox);
        idCheckBox = view.findViewById(R.id.database_id_checkbox);
        channelEditText = view.findViewById(R.id.database_channel_editText);
        messageEditText = view.findViewById(R.id.database_message_editText);
        nameEditText = view.findViewById(R.id.database_name_editText);
        dateFromEditText = view.findViewById(R.id.database_dateFrom_editText);
        dateToEditText = view.findViewById(R.id.database_dateTo_editText);
        idEditText = view.findViewById(R.id.database_id_editText);
        hitsView = view.findViewById(R.id.database_hits);
        
        database = new ChatDatabase();
        database.init(getContext(), this);

        TileDrawable tileDrawable  = new TileDrawable(getContext().getDrawable(R.drawable.background_part), Shader.TileMode.REPEAT);
        background.setImageDrawable(tileDrawable);

        messageAdapter = new MessageAdapter(getContext(),new ArrayList<>(), true);
        messageListView.setAdapter(messageAdapter);
        searchButton.setOnClickListener(a -> search());

        expandCheckBox.setOnCheckedChangeListener(this);

        channelCheckBox.setOnCheckedChangeListener(this);
        messageCheckBox.setOnCheckedChangeListener(this);
        nameCheckBox.setOnCheckedChangeListener(this);
        dateFromCheckBox.setOnCheckedChangeListener(this);
        dateToCheckBox.setOnCheckedChangeListener(this);
        idCheckBox.setOnCheckedChangeListener(this);

        onCheckedChanged(channelCheckBox, channelCheckBox.isChecked());
        onCheckedChanged(messageCheckBox, messageCheckBox.isChecked());
        onCheckedChanged(nameCheckBox, nameCheckBox.isChecked());
        onCheckedChanged(dateFromCheckBox, dateFromCheckBox.isChecked());
        onCheckedChanged(dateToCheckBox, dateToCheckBox.isChecked());
        onCheckedChanged(idCheckBox, idCheckBox.isChecked());

        dateFromEditText.addTextChangedListener(new PatternedTextWatcher("####-##-## ##:##:##"));
        dateToEditText.addTextChangedListener(new PatternedTextWatcher("####-##-## ##:##:##"));
        
        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.expand_checkBox:
                if (isChecked) {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    expand(expand, getResources().getInteger(R.integer.expand_time));
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(expand, getResources().getInteger(R.integer.expand_time));
                }
                break;
            case R.id.database_channel_checkbox:
                channelEditText.setEnabled(isChecked);
                if (isChecked) channelEditText.requestFocus();
                break;
            case R.id.database_message_checkbox:
                messageEditText.setEnabled(isChecked);
                if (isChecked) messageEditText.requestFocus();
                break;
            case R.id.database_name_checkbox:
                nameEditText.setEnabled(isChecked);
                if (isChecked) nameEditText.requestFocus();
                break;
            case R.id.database_dateFrom_checkbox:
                dateFromEditText.setEnabled(isChecked);
                if (isChecked) dateFromEditText.requestFocus();
                break;
            case R.id.database_dateTo_checkbox:
                dateToEditText.setEnabled(isChecked);
                if (isChecked) dateToEditText.requestFocus();
                break;
            case R.id.database_id_checkbox:
                idEditText.setEnabled(isChecked);
                if (isChecked) idEditText.requestFocus();
                break;
        }
    }

    private static void expand(final View v, int duration) {
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);

        v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final int targetHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(duration);
        v.startAnimation(a);
    }

    private static void collapse(final View v, int duration) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(duration);
        v.startAnimation(a);
    }

    public void search() {
        if (!checkFilters()) return;

        ArrayList<String> filters = new ArrayList<>();

        StringBuilder dateFromEditTextBuilder = new StringBuilder(dateFromEditText.getText().toString());
        while (dateFromEditTextBuilder.length() < 4) {
            dateFromEditTextBuilder.append("0");
        }
        if (dateFromEditTextBuilder.length() == 4) dateFromEditTextBuilder.append("-");

        StringBuilder dateToEditTextBuilder = new StringBuilder(dateToEditText.getText().toString());
        while (dateToEditTextBuilder.length() < 4) {
            dateToEditTextBuilder.append("0");
        }
        if (dateToEditTextBuilder.length() == 4) dateToEditTextBuilder.append("-");


        if (channelCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_CHANNEL + " = '" + channelEditText.getText().toString() + "')");
        if (messageCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_MESSAGE + " LIKE '%" + messageEditText.getText().toString() + "%')");
        if (nameCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_NAME + " LIKE '%" + nameEditText.getText().toString() + "%')");
        if (dateFromCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_DATE + " >= '" + dateFromEditTextBuilder.toString() + "')");
        if (dateToCheckBox.isChecked()) filters.add("(" + COLUMN_NAME_DATE + " <= '" + dateToEditTextBuilder.toString() + "')");
        if (idCheckBox.isChecked()) {
            String idText = idEditText.getText().toString();
            if (!idText.equals("")) {
                try {
                    int id = Integer.valueOf(idText);
                    if (id >= 0) idText += "-";
                } catch (NumberFormatException ignored) {
                }
                String[] id = idText.split("-");
                int idFrom = -1, idTo = -1;
                if (id.length > 0 && !id[0].isEmpty()) idFrom = Integer.valueOf(id[0]);
                if (id.length > 1 && !id[1].isEmpty()) idTo = Integer.valueOf(id[1]);

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
        sql.append(" ORDER BY " + COLUMN_NAME_ID + " ASC");

        messageAdapter.clear();

        searchProgress.setVisibility(View.VISIBLE);
        messageListView.setVisibility(View.GONE);
        searchButton.setEnabled(false);

        database.query(sql.toString(), null);
    }
    
    private boolean checkFilters() {
        boolean error = false;
        boolean localError;

        if (channelCheckBox.isChecked()) {
            channelCheckBox.post(() -> channelEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        } else {
            channelCheckBox.post(() -> channelEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (messageCheckBox.isChecked()) {
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        } else {
            messageEditText.post(() -> messageEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (nameCheckBox.isChecked()) {
            nameEditText.post(() -> nameEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        } else {
            nameEditText.post(() -> nameEditText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0));
        }
        if (dateFromCheckBox.isChecked()) {
            localError = false;
            try {
                String[] dateFrom = dateFromEditText.getText().toString().split("[^\\d]");
                if (dateFrom.length > 1) {
                    for (int i = 0; i < 2 - dateFrom[1].length(); i++) {
                        dateFrom[1] += "0";
                    }
                    if (Integer.valueOf(dateFrom[1]) > 12 || Integer.valueOf(dateFrom[1]) == 0) localError = true;
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
                        boolean leap = Integer.valueOf(dateFrom[0]) % 4 == 0 && (!(Integer.valueOf(dateFrom[0]) % 100 == 0) || Integer.valueOf(dateFrom[0]) % 400 == 0);
                        monthLength = leap ? 29 : 28;
                    }
                    if (Integer.valueOf(dateFrom[2]) > monthLength || Integer.valueOf(dateFrom[2]) == 0) localError = true;
                }
                if (dateFrom.length > 3) {
                    for (int i = 0; i < 2 - dateFrom[3].length(); i++) {
                        dateFrom[3] += "0";
                    }
                    if (Integer.valueOf(dateFrom[3]) > 23) localError = true;
                }
                if (dateFrom.length > 4) {
                    for (int i = 0; i < 2 - dateFrom[4].length(); i++) {
                        dateFrom[4] += "0";
                    }
                    if (Integer.valueOf(dateFrom[4]) > 59) localError = true;
                }
                if (dateFrom.length > 5) {
                    for (int i = 0; i < 2 - dateFrom[5].length(); i++) {
                        dateFrom[5] += "0";
                    }
                    if (Integer.valueOf(dateFrom[5]) > 59) localError = true;
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                dateFromEditText.post(() -> dateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                dateFromEditText.post(() -> dateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            dateFromEditText.post(() -> dateFromEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        if (dateToCheckBox.isChecked()) {
            localError = false;
            try {
                String[] dateTo = dateToEditText.getText().toString().split("[^\\d]");
                if (dateTo.length > 1) {
                    for (int i = 0; i < 2 - dateTo[1].length(); i++) {
                        dateTo[1] += "0";
                    }
                    if (Integer.valueOf(dateTo[1]) > 12) localError = true;
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
                        boolean leap = Integer.valueOf(dateTo[0]) % 4 == 0 && (!(Integer.valueOf(dateTo[0]) % 100 == 0) || Integer.valueOf(dateTo[0]) % 400 == 0);
                        monthLength = leap ? 29 : 28;
                    }
                    if (Integer.valueOf(dateTo[2]) > monthLength) localError = true;
                }
                if (dateTo.length > 3) {
                    for (int i = 0; i < 2 - dateTo[3].length(); i++) {
                        dateTo[3] += "0";
                    }
                    if (Integer.valueOf(dateTo[3]) > 23) localError = true;
                }
                if (dateTo.length > 4) {
                    for (int i = 0; i < 2 - dateTo[4].length(); i++) {
                        dateTo[4] += "0";
                    }
                    if (Integer.valueOf(dateTo[4]) > 59) localError = true;
                }
                if (dateTo.length > 5) {
                    for (int i = 0; i < 2 - dateTo[5].length(); i++) {
                        dateTo[5] += "0";
                    }
                    if (Integer.valueOf(dateTo[5]) > 59) localError = true;
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                dateToEditText.post(() -> dateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                dateToEditText.post(() -> dateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            dateToEditText.post(() -> dateToEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        if (idCheckBox.isChecked()) {
            localError = false;
            boolean isNumber = true;
            try {
                //noinspection ResultOfMethodCallIgnored
                Integer.valueOf(idEditText.getText().toString());
            } catch (NumberFormatException e) {
                isNumber = false;
            }
            try {
                String idEditTextText = idEditText.getText().toString();
                if (!idEditTextText.equals("")) {
                    if (!(idEditTextText.matches("\\d*-\\d*") || isNumber)) localError = true;
                    else {
                        String[] ids = idEditTextText.split("-");
                        if (ids.length > 0 && !ids[0].isEmpty()) //noinspection ResultOfMethodCallIgnored
                            Integer.valueOf(ids[0]);
                        if (ids.length > 1 && !ids[1].isEmpty()) //noinspection ResultOfMethodCallIgnored
                            Integer.valueOf(ids[1]);
                    }
                }
            } catch (NumberFormatException e) {
                localError = true;
            }
            if (localError) {
                error = true;
                idEditText.post(() -> idEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0));
            } else {
                idEditText.post(() -> idEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
            }
        } else {
            idEditText.post(() -> idEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0));
        }
        return !error;
    }

    @Override
    public void onReceiveResult(List<Message> messages) {
        searchProgress.post(() -> searchProgress.setVisibility(View.GONE));
        messageListView.post(() -> messageListView.setVisibility(View.VISIBLE));
        searchButton.post(() -> searchButton.setEnabled(true));

        String hits = messages.size() + " " + getString(R.string.database_hits);
        hitsView.post(() -> hitsView.setText(hits));

        messageListView.post(() -> {
            messages.forEach(messageAdapter::add);
            messageAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDatabaseError() {
        searchProgress.post(() -> searchProgress.setVisibility(View.GONE));
        messageListView.post(() -> messageListView.setVisibility(View.VISIBLE));
        searchButton.post(() -> searchButton.setEnabled(true));
    }

    @Override
    public void onInsertAllUpdate(int done, int total) {}

    @Override
    public void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
