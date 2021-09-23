package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.imageActivity.ImageActivity;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.util.Actions;

public class DeepLinkingActivity extends FragmentActivity {
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.deeplink.bottom_sheet";

    private boolean mUsedIntent = false;
    private boolean mShowsBottomSheet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;

        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data == null) return;

            String scheme = data.getScheme();
            String host = data.getHost();

            if (!"https".equals(scheme) && !"http".equals(scheme)) return;
            if (host == null) return;

            if ("qedgallery.qed-verein.de".equals(host)) {
                if (handleGallery(intent)) {
                    mUsedIntent = true;
                    return;
                }
            }

            if ("qeddb.qed-verein.de".equals(host)) {
                if (handleDatabase(intent)) {
                    mUsedIntent = true;
                    return;
                }
            }

            if (MainActivity.handleIntent(intent, null)) {
                Intent intent1 = new Intent(QEDIntent.ACTION_SHOW, data, this, MainActivity.class);
                startActivity(intent1);
                mUsedIntent = true;
            }
        }
    }

    private boolean handleDatabase(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) return false;

        String path = uri.getPath();
        if (path == null) return false;

        if (path.startsWith("/people")) {
            if (path.matches("/people/\\d{1,5}")) {
                int id = Integer.parseInt(path.substring(8));
                Actions.showInfoSheet(this, new Person(id), FRAGMENT_TAG);
                mShowsBottomSheet = true;
                return true;
            } else {
                // handled by MainActivity
                return false;
            }
        } else if (path.startsWith("/events")) {
            if (path.matches("/events/\\d{1,5}")) {
                int id = Integer.parseInt(path.substring(8));
                Actions.showInfoSheet(this, new Event(id), FRAGMENT_TAG);
                mShowsBottomSheet = true;
                return true;
            } else {
                // handled by MainActivity
                return false;
            }
        }

        return false;
    }

    private boolean handleGallery(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) return false;

        String path = uri.getPath();
        if (path == null) return false;

        if (path.startsWith("/image_view.php")) {
            boolean wantsIntent = ImageActivity.handleIntent(intent, null);

            if (wantsIntent) {
                Intent intent1 = new Intent(QEDIntent.ACTION_SHOW_IMAGE, uri, this, ImageActivity.class);
                startActivity(intent1);
                return true;
            }
        } else if (path.startsWith("/album_view.php")) {
            boolean wantsIntent = GalleryAlbumActivity.handleIntent(intent, null);

            if (wantsIntent) {
                Intent intent1 = new Intent(QEDIntent.ACTION_SHOW_ALBUM, uri, this, GalleryAlbumActivity.class);
                startActivity(intent1);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mUsedIntent)
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();

        if (!mShowsBottomSheet) {
            finish();
        } else {
            // register callback in order to close DeepLinkingActivity after bottom sheet dismissal
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    super.onFragmentDestroyed(fm, f);

                    if (FRAGMENT_TAG.equals(f.getTag())) {
                        finish();
                        fragmentManager.unregisterFragmentLifecycleCallbacks(this);
                    }
                }
            }, true);
        }
    }

    public static class QEDIntent {
        public static final String ACTION_SHOW = "com.jonahbauer.qed.intent.action.SHOW";
        public static final String ACTION_SHOW_ALBUM = "com.jonahbauer.qed.intent.action.SHOW_ALBUM";
        public static final String ACTION_SHOW_IMAGE = "com.jonahbauer.qed.intent.action.SHOW_IMAGE";
    }
}
