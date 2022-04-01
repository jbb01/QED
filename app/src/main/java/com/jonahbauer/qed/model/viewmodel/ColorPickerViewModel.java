package com.jonahbauer.qed.model.viewmodel;

import android.graphics.Color;
import android.os.Build;

import com.jonahbauer.qed.util.Colors;

import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ColorPickerViewModel extends ViewModel {
    private final MutableLiveData<Pair<String, Integer>> mResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mCalculating = new MutableLiveData<>(false);
    private final MutableLiveData<Pair<Integer, float[]>> mColor = new MutableLiveData<>();
    private final MediatorLiveData<Double> mDeltaE = new MediatorLiveData<>();

    {
        mDeltaE.addSource(mColor, color -> mDeltaE.setValue(deltaE(mResult.getValue(), color)));
        mDeltaE.addSource(mResult, result -> mDeltaE.setValue(deltaE(result, mColor.getValue())));
    }

    private String mName;

    private Disposable mCalculationTask;
    private int mCalculationProgress;

    public void init(String name) {
        if (!Objects.equals(mName, name.trim())) {
            mName = name.trim();
            var color = Colors.getColorForName(name);
            mResult.setValue(Pair.create(name, color));
            setColor(color);
        }
    }

    public void calculate() {
        if (isCalculating()) return;
        if (mCalculationTask != null) mCalculationTask.dispose();
        var color = mColor.getValue();
        if (color == null) return;

        mCalculating.setValue(true);

        int start = mCalculationProgress == 0 ? 0 : (1 << (mCalculationProgress + 10));
        int count = (1 << (mCalculationProgress + 11)) - start;
        var single = Single.fromCallable(() -> {
            var out = Colors.findColor(mName, color.first, mCalculationProgress, count);
            return out != null ? out : mName;
        });
        mCalculationTask = single.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::cancel)
                .subscribe(name -> {
                    mCalculationProgress++;

                    var c = mColor.getValue();
                    var newValue = Pair.create(name, Colors.getColorForName(name));
                    var oldValue = mResult.getValue();
                    if (oldValue == null || deltaE(newValue, c) < deltaE(oldValue, c)) {
                        mResult.setValue(Pair.create(name, Colors.getColorForName(name)));
                    }
                });
    }

    public void setColor(@ColorInt int color) {
        var old = mColor.getValue();
        if (old == null || old.first != color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            mColor.setValue(Pair.create(color, hsv));
            reset();
        }
    }

    public void setColor(float hue, float saturation, float value) {
        var old = mColor.getValue();
        if (old == null || old.second[0] != hue || old.second[1] != saturation || old.second[2] != value) {
            float[] hsv = new float[]{hue, saturation, value};
            mColor.setValue(Pair.create(Color.HSVToColor(hsv), hsv));
            reset();
        }
    }

    public void reset() {
        cancel();
        mCalculationProgress = 0;
    }

    public void cancel() {
        if (mCalculationTask != null && !mCalculationTask.isDisposed()) {
            mCalculationTask.dispose();
        }

        mCalculationTask = null;
        mCalculating.setValue(false);
    }

    public LiveData<Pair<String, Integer>> getResult() {
        return mResult;
    }

    public LiveData<Boolean> getCalculating() {
        return mCalculating;
    }

    public LiveData<Pair<Integer, float[]>> getColor() {
        return mColor;
    }

    public LiveData<Double> getDeltaE() {
        return mDeltaE;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mCalculationTask != null) mCalculationTask.dispose();
    }

    private boolean isCalculating() {
        return Objects.requireNonNull(mCalculating.getValue());
    }

    private static double deltaE(Pair<String, Integer> result, Pair<Integer, float[]> color) {
        if (result == null || color == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Double.NaN;
        } else {
            return Colors.deltaE(result.second, color.first);
        }
    }
}
