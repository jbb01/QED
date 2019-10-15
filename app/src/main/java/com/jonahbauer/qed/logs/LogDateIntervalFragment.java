package com.jonahbauer.qed.logs;

import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.jonahbauer.qed.R;

public class LogDateIntervalFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_log_dialog_dateinterval, container, false);

        assert getActivity() != null;

        ((EditText)view.findViewById(R.id.log_dialog_dateinterval_editText_from)).setText(android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext()).format(Calendar.getInstance(TimeZone.getDefault()).getTime()));
        ((EditText)view.findViewById(R.id.log_dialog_dateinterval_editText_to)).setText(android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext()).format(Calendar.getInstance(TimeZone.getDefault()).getTime()));
        return view;
    }
}
