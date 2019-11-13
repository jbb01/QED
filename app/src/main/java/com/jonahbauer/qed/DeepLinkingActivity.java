package com.jonahbauer.qed;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.jonahbauer.qed.activities.GalleryAlbumActivity;
import com.jonahbauer.qed.activities.ImageActivity;
import com.jonahbauer.qed.activities.MainActivity;

public class DeepLinkingActivity extends Activity {
    private boolean usedIntent = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data == null) {
                return;
            }

            String scheme = data.getScheme();
            String host = data.getHost();
            String path = data.getPath();

            if (!"https".equals(scheme)) return;
            if (host == null) return;

            if ("qedgallery.qed-verein.de".equals(host)) {
                if (path != null) {
                    if (path.startsWith("/image_view.php")) {
                        boolean wantsIntent = ImageActivity.handleIntent(intent, null);

                        if (wantsIntent) {
                            Intent intent1 = new Intent("com.jonahbauer.qed.action.SHOW_IMAGE", data, this, ImageActivity.class);
                            startActivity(intent1);
                            usedIntent = true;
                        }

                    } else if (path.startsWith("/album_view.php")) {
                        boolean wantsIntent = GalleryAlbumActivity.handleIntent(intent, null);

                        if (wantsIntent) {
                            Intent intent1 = new Intent("com.jonahbauer.qed.action.SHOW_ALBUM", data, this, GalleryAlbumActivity.class);
                            startActivity(intent1);
                            usedIntent = true;
                        }
                    }
                }
                if (usedIntent) return;
            }

            if (MainActivity.handleIntent(intent, null)) {
                Intent intent1 = new Intent("com.jonahbauer.qed.action.SHOW", data, this, MainActivity.class);
                startActivity(intent1);
                usedIntent = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!usedIntent)
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        finish();
    }
}
