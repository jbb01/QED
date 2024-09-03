package eu.jonahbauer.qed.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class AccountAuthenticatorService extends Service {
    private AccountAuthenticator mAccountAuthenticator;

    @Override
    public void onCreate() {
        mAccountAuthenticator = new AccountAuthenticator(getApplicationContext());
    }

    @Override
    public @Nullable IBinder onBind(Intent intent) {
        return mAccountAuthenticator.getIBinder();
    }
}
