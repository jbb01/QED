package com.jonahbauer.qed.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.logs.LogDateIntervalFragment;
import com.jonahbauer.qed.logs.LogDateRecentFragment;
import com.jonahbauer.qed.logs.LogPostRecentFragment;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import static com.jonahbauer.qed.activities.LogFragment.LOG_FROM;
import static com.jonahbauer.qed.activities.LogFragment.LOG_LAST;
import static com.jonahbauer.qed.activities.LogFragment.LOG_MODE_INTERVAL_DATE;
import static com.jonahbauer.qed.activities.LogFragment.LOG_MODE_KEY;
import static com.jonahbauer.qed.activities.LogFragment.LOG_MODE_RECENT_DATE;
import static com.jonahbauer.qed.activities.LogFragment.LOG_MODE_RECENT_POSTS;
import static com.jonahbauer.qed.activities.LogFragment.LOG_TO;

public class LogDialogActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener {
    private String mode;
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

        if (getString(R.string.log_postrecent).equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogPostRecentFragment(), "");
            mode = LOG_MODE_RECENT_POSTS;
        } else if (getString(R.string.log_daterecent).equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateRecentFragment(), "");
            mode = LOG_MODE_RECENT_DATE;
        } else if (getString(R.string.log_dateinterval).equals(item)) {
            fragmentTransaction.add(R.id.log_dialog_fragment_holder, new LogDateIntervalFragment(), "");
            mode = LOG_MODE_INTERVAL_DATE;
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
        resultIntent.putExtra(LOG_MODE_KEY, mode);
        switch (mode) {
            case LOG_MODE_RECENT_POSTS: {
                String last = ((EditText) findViewById(R.id.log_dialog_postrecent_editText)).getText().toString();
                resultIntent.putExtra(LOG_LAST, last);
                break;
            }
            case LOG_MODE_RECENT_DATE: {
                String last = ((EditText) findViewById(R.id.log_dialog_daterecent_editText)).getText().toString();
                if (last.trim().equals("")) last = "1";
                resultIntent.putExtra(LOG_LAST, String.valueOf(Integer.valueOf(last) * 3600));
                break;
            }
            case LOG_MODE_INTERVAL_DATE: {
                resultIntent.putExtra(LOG_FROM, (dateStart != null)? MessageFormat.format("{0,date,dd.MM.yyyy}", dateStart): "");
                resultIntent.putExtra(LOG_TO, (dateEnd != null)? MessageFormat.format("{0,date,dd.MM.yyyy}", dateEnd): "");
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
