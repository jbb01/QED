package com.jonahbauer.qed.networking.downloadManager;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.DeepLinkingActivity;
import com.jonahbauer.qed.R;

import java.util.Random;

import static com.jonahbauer.qed.Application.NOTIFICATION_CHANNEL_ID;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
    private static final LongSparseArray<Integer> downloadNotifications = new LongSparseArray<>();
    private static final Random random = new Random();


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) Log.d(Application.LOG_TAG_DEBUG, "intent: " + intent);
        if (intent != null && DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            final long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            Download download = Download.getDownload(id);
            if (download == null) return;

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            assert downloadManager != null;

            final Cursor cursor = downloadManager.query(query);

            if (!cursor.moveToFirst()) return;


            DownloadListener listener = download.getListener();

            //noinspection StatementWithEmptyBody
            if (Application.isForeground() && listener != null) {
                // when app is in foreground and a receiver is present -> call receiver
//                handler.post(() -> listener.onDownloadCompleted(id, cursor));
            } else {
                // when app is in background or no receiver is present (e.g. when fragment no longer visible) -> show notification
                Application.createNotificationChannel(context);
                showNotification(context, id, cursor);
            }
        } else if (intent != null && DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            final long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

            Download download = null;

            if (ids != null) for (long id : ids) {
                download = Download.getDownload(id);
                if (download != null) break;
            }
            if (download == null) return;

            final long id = download.getDownloadId();

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            assert downloadManager != null;

            final Cursor cursor = downloadManager.query(query);

            if (!cursor.moveToFirst()) return;

            Intent intent2 = new Intent(
                    DeepLinkingActivity.QEDIntent.ACTION_SHOW,
                    // TODO generify
                    Uri.parse("https://chat.qed-verein.de/rubychat/history"),
                    context,
                    DeepLinkingActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);

            Intent intent3 = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(intent3);
        }
    }

    private static void showNotification(Context context, long downloadId, Cursor download) {
        Intent intent = new Intent(
                DeepLinkingActivity.QEDIntent.ACTION_SHOW,
                // TODO generify
                Uri.parse("https://chat.qed-verein.de/rubychat/history"),
                context,
                DeepLinkingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_qed_logo_notification)
                .setContentTitle(context.getString(R.string.download_notification_title))
                .setContentText("Ein Download ist fertig.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        int notificationId = random.nextInt();
        downloadNotifications.put(downloadId, notificationId);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    public static void cancelNotification(Context context, long downloadId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Integer notificationId = downloadNotifications.get(downloadId, -1);
        if (notificationId != null && notificationId != -1)
            notificationManager.cancel(notificationId);
    }
}
