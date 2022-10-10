package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentLoginBinding;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LoginFragment extends Fragment implements QEDPageReceiver<Boolean> {

    private FragmentLoginBinding mBinding;

    private Feature mFeature = Feature.CHAT;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            LoginFragmentArgs args = LoginFragmentArgs.fromBundle(arguments);
            mFeature = args.getFeature();
        }

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentLoginBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.rememberMeCheckbox.setChecked(Preferences.getGeneral().isRememberMe());
        mBinding.rememberMeCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> Preferences.getGeneral().setRememberMe(isChecked)
        );

        mBinding.password.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        mBinding.usernameLayout.setErrorEnabled(true);
        mBinding.passwordLayout.setErrorEnabled(true);

        mBinding.signInButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        mBinding.usernameLayout.setError(null);
        mBinding.passwordLayout.setError(null);
        ViewUtils.setError(mBinding.username, false);
        ViewUtils.setError(mBinding.password, false);

        String username = mBinding.username.getText().toString();
        char[] password = new char[mBinding.password.getText().length()];
        mBinding.password.getText().getChars(0, password.length, password, 0);

        mBinding.setLoading(true);
        mDisposable.add(
                QEDLogin.loginAsync(username, password, mFeature, this)
        );
    }

    @Override
    public void onResult(@NonNull Boolean out) {
        // login failure is handled in onError
        assert out;

        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getPreviousBackStackEntry() != null) {
            navController.popBackStack();
        } else {
            navController.navigate(R.id.nav_chat);
        }
    }

    @Override
    public void onError(Boolean out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);

        mBinding.setLoading(false);

        if (cause instanceof InvalidCredentialsException) {
            setError(getString(R.string.login_error_incorrect_data));
        } else {
            setError(getString(reason.getStringRes()));
        }
    }

    private void setError(String string) {
        mBinding.passwordLayout.setError(string);
        ViewUtils.setError(mBinding.password, string != null);
        if (string != null) mBinding.password.requestFocus();
    }
}

