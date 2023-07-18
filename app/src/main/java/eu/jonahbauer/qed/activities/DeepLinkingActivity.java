package eu.jonahbauer.qed.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.util.Actions;

public class DeepLinkingActivity extends AppCompatActivity {
    private static final String FRAGMENT_TAG = DeepLinkingActivity.class.getName();

    private BottomSheetDialogFragment mBottomSheet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;

        var fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            mBottomSheet = (BottomSheetDialogFragment) fragment;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data == null) return;

            String scheme = data.getScheme();
            String host = data.getHost();
            String path = data.getPath();

            if (!"http".equals(scheme) && !"https".equals(scheme)) return;
            if (!"qeddb.qed-verein.de".equals(host)) return;
            if (path == null) return;

            if (path.matches("^/people/\\d{1,5}")) {
                int id = Integer.parseInt(path.substring(8));
                mBottomSheet = Actions.showInfoSheet(this, new Person(id), FRAGMENT_TAG);
            } else if (path.matches("^/events/\\d{1,5}")) {
                int id = Integer.parseInt(path.substring(8));
                mBottomSheet = Actions.showInfoSheet(this, new Event(id), FRAGMENT_TAG);
            } else if (path.matches("^/registrations/\\d{1,5}")) {
                int id = Integer.parseInt(path.substring(15));
                mBottomSheet = Actions.showInfoSheet(this, new Registration(id), FRAGMENT_TAG);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mBottomSheet == null) {
            Toast.makeText(this, R.string.error_bad_link, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mBottomSheet.requireDialog().setOnDismissListener(dialog -> {
                finish();
            });
        }
    }
}
