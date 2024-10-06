package eu.jonahbauer.qed.util.preferences;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.provider.ContactsContract;
import eu.jonahbauer.qed.Application;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.Preferences;

public class FavoritesSharedPreferenceListener implements OnSharedPreferenceChangeListener  {
    public static final FavoritesSharedPreferenceListener INSTANCE = new FavoritesSharedPreferenceListener();

    private FavoritesSharedPreferenceListener() {}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.getDatabase().getKeys().getFavorites().equals(key)) {
            var application = Application.getApplicationReference().get();
            if (application == null) return;

            String accountName = application.getString(R.string.app_name);
            String accountType = application.getString(R.string.account_type);
            Account account = new Account(accountName, accountType);

            ContentResolver.requestSync(account, ContactsContract.AUTHORITY, Bundle.EMPTY);
        }
    }
}
