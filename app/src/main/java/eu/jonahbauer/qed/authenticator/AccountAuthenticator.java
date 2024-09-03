package eu.jonahbauer.qed.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

    public AccountAuthenticator(@NonNull Context context) {
        super(context);
    }

    @Override
    public @Nullable Bundle addAccount(
            @NonNull AccountAuthenticatorResponse response, @NonNull String accountType,
            @Nullable String authTokenType, @Nullable String[] requiredFeatures, @NonNull Bundle options
    ) {
        return null;
    }

    // <editor-fold desc="Unsupported API">

    @Override
    public @Nullable Bundle editProperties(@NonNull AccountAuthenticatorResponse response, @NonNull String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Bundle confirmCredentials(@NonNull AccountAuthenticatorResponse response, @NonNull Account account, @Nullable Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Bundle getAuthToken(@NonNull AccountAuthenticatorResponse response, @NonNull Account account, @NonNull String authTokenType, @NonNull Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String getAuthTokenLabel(@NonNull String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Bundle updateCredentials(@NonNull AccountAuthenticatorResponse response, @NonNull Account account, @Nullable String authTokenType, @Nullable Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Bundle hasFeatures(@NonNull AccountAuthenticatorResponse response, @NonNull Account account, @NonNull String[] features) {
        throw new UnsupportedOperationException();
    }

    // </editor-fold>
}
