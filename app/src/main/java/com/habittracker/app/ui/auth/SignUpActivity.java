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
 * Sign up screen with name, email, password.
 * On success, auto-signs in and navigates to Home.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton signUpButton;
    private ProgressBar loading;
    private TextView errorText;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TokenManager tokenManager = new TokenManager(this);
        SupabaseClient client = SupabaseClient.getInstance(tokenManager);
        authRepository = new AuthRepository(tokenManager, client);

        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        signUpButton = findViewById(R.id.sign_up_button);
        loading = findViewById(R.id.loading);
        errorText = findViewById(R.id.error_text);
        TextView signInLink = findViewById(R.id.sign_in_link);

        signUpButton.setOnClickListener(v -> attemptSignUp());

        signInLink.setOnClickListener(v -> {
            finish(); // Return to login
        });
    }

    private void attemptSignUp() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        authRepository.signUp(email, password, name, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    setLoading(false);
                    AppDatabase db = AppDatabase.getInstance(SignUpActivity.this);
                    new com.habittracker.app.data.repository.HabitRepository(db)
                            .ensureProfileExists(userId, name);
                    navigateToMain();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Sign up failed. This email may already be in use.");
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        signUpButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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
