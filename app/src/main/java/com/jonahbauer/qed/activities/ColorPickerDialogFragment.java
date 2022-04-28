package com.jonahbauer.qed.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.lifecycle.ViewModelProvider;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogColorBinding;
import com.jonahbauer.qed.layoutStuff.views.ChatColorPicker;
import com.jonahbauer.qed.model.viewmodel.ColorPickerViewModel;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ColorPickerDialogFragment extends AppCompatDialogFragment implements ChatColorPicker.OnColorChangedListener, SeekBar.OnSeekBarChangeListener {
    private static final String SAVED_MODE = "mode";

    public static final int MODE_HSV = 0;
    public static final int MODE_RGB = 1;

    private AlertDialogColorBinding mBinding;
    private ColorPickerViewModel mViewModel;

    private @Nullable Button mNeutralButton;

    private OnDismissListener mDismissListener;

    private int mMode = MODE_HSV;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMode = savedInstanceState.getInt(SAVED_MODE);
        }

        var builder = new AlertDialog.Builder(requireContext());

        var inflater = LayoutInflater.from(requireContext());
        mBinding = AlertDialogColorBinding.inflate(inflater);

        mBinding.colorPicker.setOnColorChangedListener(this);
        mBinding.seekBarRed.setOnSeekBarChangeListener(this);
        mBinding.seekBarGreen.setOnSeekBarChangeListener(this);
        mBinding.seekBarBlue.setOnSeekBarChangeListener(this);
        mBinding.info.setOnClickListener(v -> showInfoDialog());
        mBinding.deltaE.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        mBinding.toggleMode.setOnClickListener(v -> {
            mMode = 1 - mMode;
            mBinding.setMode(mMode);
        });
        mBinding.setMode(mMode);

        builder.setView(mBinding.getRoot());

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            var result = mViewModel.getResult().getValue();
            if (result != null) {
                Preferences.chat().edit().setName(result.first).apply();
            }
            this.dismiss();
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> this.dismiss());
        builder.setNeutralButton(R.string.color_picker_calculate, (dialog, which) -> {}); // listener is set in onStart
        mViewModel = new ViewModelProvider(this).get(ColorPickerViewModel.class);

        mViewModel.getCalculating().observe(this, calculating -> {
            if (mNeutralButton != null) mNeutralButton.setEnabled(!calculating);
        });
        mViewModel.getResult().observe(this, result -> {
            if (result == null) return;
            mBinding.setCalculatedColor(result.second);
        });
        mViewModel.getColor().observe(this, color -> {
            if (color == null) return;
            mBinding.setColor(color.first);
            mBinding.seekBarRed.setProgress(Color.red(color.first) - 100);
            mBinding.seekBarGreen.setProgress(Color.green(color.first) - 100);
            mBinding.seekBarBlue.setProgress(Color.blue(color.first) - 100);
            mBinding.colorPicker.setColor(color.second);
        });
        mViewModel.getDeltaE().observe(this, mBinding::setDeltaE);

        var name = Preferences.chat().getName();
        mViewModel.init(name);
        mBinding.setName(MessageUtils.formatName(requireContext(), name));

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        var dialog = (AlertDialog) requireDialog();
        mNeutralButton = Objects.requireNonNull(dialog.getButton(Dialog.BUTTON_NEUTRAL));
        mNeutralButton.setOnClickListener(v -> mViewModel.calculate());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDismissListener != null) mDismissListener.onDismiss();
    }

    @Override
    public void onColorChanged(float hue, float saturation, float value, boolean fromUser) {
        if (!fromUser) return;
        mViewModel.setColor(hue, saturation, value);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;
        int offset = -1;
        if (seekBar == mBinding.seekBarRed) {
            offset = 16;
        } else if (seekBar == mBinding.seekBarGreen) {
            offset = 8;
        } else if (seekBar == mBinding.seekBarBlue) {
            offset = 0;
        }
        if (offset != -1) {
            var color = mViewModel.getColor().getValue();
            var rgb = color == null ? Color.WHITE : color.first;
            mViewModel.setColor(rgb & ~(0xFF << offset) | (((progress + 100) & 0xFF) << offset));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void showInfoDialog() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.color_picker_help)
                .setPositiveButton(R.string.ok, (d, which) -> d.dismiss())
                .show();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        mDismissListener = listener;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_MODE, mMode);
    }

    @FunctionalInterface
    public interface OnDismissListener {
        void onDismiss();
    }
}
