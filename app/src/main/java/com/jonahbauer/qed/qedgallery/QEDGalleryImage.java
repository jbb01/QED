package com.jonahbauer.qed.qedgallery;

import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

@Deprecated
public class QEDGalleryImage extends AsyncTask<Object, Long, Boolean> {
    private QEDGalleryImageReceiver receiver;
    private Image image;

    @Override
    protected Boolean doInBackground(Object...objects) {
        Mode mode = Mode.THUMBNAIL;

        if (objects.length < 4) return false;
        if (objects[0] instanceof QEDGalleryImageReceiver) receiver = (QEDGalleryImageReceiver) objects[0];
        else return false;

        if (objects[1] instanceof Image) image = (Image) objects[1];
        else return false;

        if (objects[2] instanceof Mode) mode = (Mode) objects[2];

        OutputStream os;
        if (objects[3] instanceof OutputStream) os = (OutputStream) objects[3];
        else return false;

        Application application = Application.getContext();

        String userid = application.loadData(Application.KEY_USERID, false);
        String pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
        String sessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);

        if (userid == null || pwhash == null || sessionId == null)
            return false;

        InputStream in;

        String modeStr = "";
        if (mode == Mode.THUMBNAIL) modeStr = "thumbnail";
        else if (mode == Mode.NORMAL) modeStr = "normal";
        else if (mode == Mode.ORIGINAL) modeStr = "original";

        //Log.d("test", "loading file " + image.id + " in mode " + modeStr);
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(String.format(Application.getContext().getString(R.string.gallery_server_image), modeStr, String.valueOf(image.id))).openConnection();
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId + "; pwhash=" + pwhash + "; userid=" + userid);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");


            long contentLength = httpsURLConnection.getContentLength();
            in = httpsURLConnection.getInputStream();

            byte[] buffer = new byte[4 * 1024];
            long count = 0;
            int n;
            int i = 0;
            while (-1 != (n = in.read(buffer)) && !isCancelled()) {
                os.write(buffer, 0, n);
                count += n;
                i++;
                if (i == 256) {
                    i = 0;
                    publishProgress(count, contentLength);
                }
            }

            in.close();
            os.close();

            httpsURLConnection.disconnect();

            return true;
        } catch (IOException e) {
            Log.e("test", e.getMessage(), e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!isCancelled()) {
            receiver.onGalleryImageReceived(image, success);
            receiver.onGalleryImageProgressUpdate(0, 0);
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        receiver.onGalleryImageProgressUpdate(values[0], values[1]);
    }

    public enum Mode {
        THUMBNAIL, NORMAL, ORIGINAL
    }
}
