/*package com.jonahbauer.qed.networking;

import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.chat.Message;

import java.util.ArrayList;
import java.util.List;


public class NetworkTest {
    public static void testNetwork() {

        List<Message> messageList = new ArrayList<>();

        QEDPageStreamReceiver receiver = new QEDPageStreamReceiver() {
            @Override
            public void onStreamError(String tag) {
                Log.d(Application.LOG_TAG_DEBUG, tag + ": streamError");
            }

            @Override
            public void onProgressUpdate(String tag, long done, long total) {
                if (total == Integer.MIN_VALUE && done == Integer.MIN_VALUE) {
                    Log.d(Application.LOG_TAG_DEBUG, tag + ": download complete");
                } else if (total == Integer.MIN_VALUE) {
                    Log.d(Application.LOG_TAG_DEBUG, tag + ": downloading: " + done / 1_048_576 + "MiB");
                } else {
                    Log.d(Application.LOG_TAG_DEBUG, tag + ": converting " + done + "/" + total);
                }
            }

            @Override
            public void onPageReceived(String tag) {
                Log.d(Application.LOG_TAG_DEBUG, tag + ": conversion complete");
                Log.d(Application.LOG_TAG_DEBUG, tag + ": " + messageList.size());
            }

            @Override
            public void onNetworkError(String tag) {
                Log.d(Application.LOG_TAG_DEBUG, tag + ": networkError");
            }
        };

        QEDChatPages.getChatLog("TAG", "mode=postrecent&last=50000&channel=", messageList, receiver);
    }

}
*/