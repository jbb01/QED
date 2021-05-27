package com.jonahbauer.qed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.eventSheet.EventBottomSheet;
import com.jonahbauer.qed.activities.imageActivity.ImageActivity;
import com.jonahbauer.qed.activities.personSheet.PersonBottomSheet;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Person;

public class DeepLinkingActivity extends FragmentActivity {
    private boolean mUsedIntent = false;
    private boolean mShowsBottomSheet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data == null) {
                return;
            }

            String scheme = data.getScheme();
            String host = data.getHost();
            String path = data.getPath();

            if (!"https".equals(scheme) && !"http".equals(scheme)) return;
            if (host == null) return;

            if ("qedgallery.qed-verein.de".equals(host)) {
                if (path != null) {
                    if (path.startsWith("/image_view.php")) {
                        boolean wantsIntent = ImageActivity.handleIntent(intent, null);

                        if (wantsIntent) {
                            Intent intent1 = new Intent(QEDIntent.ACTION_SHOW_IMAGE, data, this, ImageActivity.class);
                            startActivity(intent1);
                            mUsedIntent = true;
                        }

                    } else if (path.startsWith("/album_view.php")) {
                        boolean wantsIntent = GalleryAlbumActivity.handleIntent(intent, null);

                        if (wantsIntent) {
                            Intent intent1 = new Intent(QEDIntent.ACTION_SHOW_ALBUM, data, this, GalleryAlbumActivity.class);
                            startActivity(intent1);
                            mUsedIntent = true;
                        }
                    }
                }
                if (mUsedIntent) return;
            }

            if ("qeddb.qed-verein.de".equals(host)) {
                if (path != null) {
                    if (path.startsWith("/person.php") || path.startsWith("/personen.php")) {
                        String person = data.getQueryParameter("person");
                        if (person != null && person.matches("\\d{1,5}")) {
                            showBottomSheet(PersonBottomSheet.newInstance(new Person(Long.parseLong(person))));
                            mUsedIntent = true;
                            return;
                        }
                    } else if (path.startsWith("/veranstaltung.php") || path.startsWith("/veranstaltungen.php")) {
                        String event = data.getQueryParameter("veranstaltung");
                        if (event != null && event.matches("\\d{1,5}")) {
                            showBottomSheet(EventBottomSheet.newInstance(new Event(Long.parseLong(event))));
                            mUsedIntent = true;
                            return;
                        }
                    }
                }
            }

            if (MainActivity.handleIntent(intent, null)) {
                Intent intent1 = new Intent(QEDIntent.ACTION_SHOW, data, this, MainActivity.class);
                startActivity(intent1);
                mUsedIntent = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mUsedIntent)
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();

        if (!mShowsBottomSheet)
            finish();
    }

    private void showBottomSheet(BottomSheetDialogFragment bottomSheetDialogFragment) {
        if (mShowsBottomSheet) throw new IllegalStateException("Can only show one bottom sheet at a time!");
        mShowsBottomSheet = true;

        String TAG = "TAG";
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentDestroyed(fm, f);

                if (TAG.equals(f.getTag())) {
                    finish();
                    fragmentManager.unregisterFragmentLifecycleCallbacks(this);
                }
            }
        }, true);
        bottomSheetDialogFragment.show(fragmentManager, TAG);
    }

    public static class QEDIntent {
        public static final String ACTION_SHOW = "com.jonahbauer.qed.intent.action.SHOW";
        public static final String ACTION_SHOW_ALBUM = "com.jonahbauer.qed.intent.action.SHOW_ALBUM";
        public static final String ACTION_SHOW_IMAGE = "com.jonahbauer.qed.intent.action.SHOW_IMAGE";
    }
}
