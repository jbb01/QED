package com.jonahbauer.qed.activities.mainFragments;

import android.content.Intent;

public interface OnActivityReenterListener {
    void onActivityReenter(int resultCode, Intent data);
}
