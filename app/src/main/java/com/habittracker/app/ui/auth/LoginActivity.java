package com.habittracker.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.remote.SupabaseClient;
import com.habittracker.app.data.repository.AuthRepository;
import com.habittracker.app.ui.MainActivity;
import com.habittracker.app.util.TokenManager;

/**
 * Login screen with email/password, create account link, and skip option.
 * Skip option enables delayed signup — user can use the app locally without an account.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;
    private ProgressBar loading;
    private TextView errorText;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init dependencies
        TokenManager tokenManager = new TokenManager(this);
        SupabaseClient client = SupabaseClient.getInstance(tokenManager);
        authRepository = new AuthRepository(tokenManager, client);

        // Bind views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        signInButton = findViewById(R.id.sign_in_button);
        loading = findViewById(R.id.loading);
        errorText = findViewById(R.id.error_text);
        TextView createAccountLink = findViewById(R.id.create_account_link);
        TextView skipLink = findViewById(R.id.skip_link);

        // Sign in
        signInButton.setOnClickListener(v -> attemptSignIn());

        // Navigate to sign up
        createAccountLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        // Skip (delayed signup)
        skipLink.setOnClickListener(v -> {
            authRepository.useLocalOnly();
            navigateToMain();
        });
    }

    private void attemptSignIn() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        setLoading(true);

        authRepository.signIn(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    setLoading(false);
                    // Ensure profile exists
                    AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
                    new com.habittracker.app.data.repository.HabitRepository(db)
                            .ensureProfileExists(userId, email);
                    navigateToMain();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Invalid email or password. Please try again.");
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        signInButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
