package com.jonahbauer.qed.qedgallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class QEDGalleryThumbnail extends AsyncTask<Object, Void, Bitmap> {
    private QEDGalleryThumbnailReceiver receiver;
    private Image image;

    @Override
    protected Bitmap doInBackground(Object...objects) {
        char[] sessionId;
        char[] pwhash;
        char[] userid;

        if (objects.length < 5) return null;
        if (objects[0] instanceof QEDGalleryThumbnailReceiver) receiver = (QEDGalleryThumbnailReceiver) objects[0];
        else return null;

        if (objects[1] instanceof char[]) sessionId = (char[]) objects[1];
        else return null;

        if (objects[2] instanceof char[]) pwhash = (char[]) objects[2];
        else return null;

        if (objects[3] instanceof char[]) userid = (char[]) objects[3];
        else return null;

        if (objects[4] instanceof Image) image = (Image) objects[4];
        else return null;

        InputStream in;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_thumbnail) + image.id).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + String.valueOf(sessionId) + "; pwhash=" + String.valueOf(pwhash) + "; userid=" + String.valueOf(userid));
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.connect();

            in = httpsURLConnection.getInputStream();

            Bitmap bm = BitmapFactory.decodeStream(in);

            in.close();

            httpsURLConnection.disconnect();

            return bm;
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (!isCancelled())
            receiver.onGalleryThumbnailReceived(image, bitmap);
    }

    @Override
    protected void onCancelled() {

    }
}
