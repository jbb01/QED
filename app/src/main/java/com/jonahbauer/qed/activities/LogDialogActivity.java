package com.jonahbauer.qed.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.LogFragment.Mode;
import com.jonahbauer.qed.logs.LogDateIntervalFragment;
import com.jonahbauer.qed.logs.LogDateRecentFragment;
import com.jonahbauer.qed.logs.LogPostRecentFragment;

import java.text.DateFormat;
import java.util.Date;

import static com.jonahbauer.qed.activities.LogFragment.LOG_FROM_KEY;
import static com.jonahbauer.qed.activities.LogFragment.LOG_LAST_KEY;
import static com.jonahbauer.qed.activities.LogFragment.LOG_MODE_KEY;
import static com.jonahbauer.qed.activities.LogFragment.LOG_TO_KEY;
import static com.jonahbauer.qed.activities.LogFragment.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.activities.LogFragment.Mode.DATE_RECENT;
import static com.jonahbauer.qed.activities.LogFragment.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.activities.LogFragment.Mode.POST_RECENT;
import static com.jonahbauer.qed.activities.LogFragment.Mode.SINCE_OWN;

public class LogDialogActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener {
    private Mode mode;
    private Date dateStart;
    private Date dateEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_dialog);
        setFinishOnTouchOutside(true);
        Spinner modeSpinner = findViewById(R.id.log_dialog_mode_spinner);
        modeSpinner.setOnItemSelectedListener(this);

        dateStart = Calendar.getInstance(TimeZone.getDefault()).getTime();
        dateEnd = Calendar.getInstance(TimeZone.getDefault()).getTime();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        String item = (String) spinner.getItemAtPosition(position);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();

        for (Fragment fragment : manager.getFragments()) {
            fragmentTransaction.remove(fragment);
        }

        fragmentTransaction.commit();

        fragmentTransaction = manager.beginTransaction();

        if (POST_RECENT.modeStr.equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogPostRecentFragment(), "");
            mode = POST_RECENT;
        } else if (DATE_RECENT.modeStr.equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateRecentFragment(), "");
            mode = DATE_RECENT;
        } else if (DATE_INTERVAL.modeStr.equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
            mode = DATE_INTERVAL;
        } else if (POST_INTERVAL.modeStr.equals(item)) {
            // TODO fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
            mode = DATE_INTERVAL;
        } else if (SINCE_OWN.modeStr.equals(item)) {
            // TODO fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
            mode = SINCE_OWN;
        }

        fragmentTransaction.commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public void cancel(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    public void ok(View view) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(LOG_MODE_KEY, mode.toString());
        switch (mode) {
            case POST_RECENT: {
                Long last = Long.valueOf(((EditText) findViewById(R.id.log_dialog_postrecent_editText)).getText().toString().trim());
                resultIntent.putExtra(LOG_LAST_KEY, last);
                break;
            }
            case DATE_RECENT: {
                Long last = Long.valueOf(((EditText) findViewById(R.id.log_dialog_daterecent_editText)).getText().toString().trim());
                resultIntent.putExtra(LOG_LAST_KEY, last * 3600);
                break;
            }
            case DATE_INTERVAL: {
                resultIntent.putExtra(LOG_FROM_KEY, (dateStart != null) ? dateStart.getTime() : 0);
                resultIntent.putExtra(LOG_TO_KEY, (dateEnd != null) ? dateEnd.getTime() : 0);
                break;
            }
            case POST_INTERVAL:
            case SINCE_OWN: {
                // TODO
                break;
            }
        }

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR,year);
        cal.set(Calendar.MONTH,month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        Date date = cal.getTime();
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        if (view.getId() == R.id.log_dialog_datepicker_start) {
            dateStart = date;
            ((EditText) findViewById(R.id.log_dialog_dateinterval_editText_from)).setText(dateFormat.format(date));
        } else if (view.getId() == R.id.log_dialog_datepicker_end) {
            dateEnd = date;
            ((EditText) findViewById(R.id.log_dialog_dateinterval_editText_to)).setText(dateFormat.format(date));
        }
    }


    public void pickDateIntervalStart(View view) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

        DatePickerDialog dialog = new DatePickerDialog(this, R.style.AppTheme_Dialog, this,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.ok), (dialog1, which) -> {
            DatePicker datePicker = dialog.getDatePicker();
            onDateSet(datePicker, datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        });
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (Message) null);

        dialog.show();

        dialog.getDatePicker().setId(R.id.log_dialog_datepicker_start);
    }

    public void pickDateIntervalEnd(View view) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

        DatePickerDialog dialog = new DatePickerDialog(this, R.style.AppTheme_Dialog, this,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.ok), (dialog1, which) -> {
            DatePicker datePicker = dialog.getDatePicker();
            onDateSet(datePicker, datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        });
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (Message) null);

        dialog.show();

        dialog.getDatePicker().setId(R.id.log_dialog_datepicker_end);
    }
}
