package eu.jonahbauer.qed.contact;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class ContactSyncAdapterService extends Service {
    private static ContactSyncAdapter sContactSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        // Create the sync adapter as a singleton.
        synchronized (sSyncAdapterLock) {
            if (sContactSyncAdapter == null) {
                sContactSyncAdapter = new ContactSyncAdapter(getApplicationContext());
            }
        }
    }

    @Override
    public @Nullable IBinder onBind(Intent intent) {
        return sContactSyncAdapter.getSyncAdapterBinder();
    }
}
