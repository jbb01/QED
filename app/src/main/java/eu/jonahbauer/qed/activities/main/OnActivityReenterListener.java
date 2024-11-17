package eu.jonahbauer.qed.activities.main;

import android.content.Intent;

public interface OnActivityReenterListener {
    void onActivityReenter(int resultCode, Intent data);
}
