package com.habittracker.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.habittracker.app.data.remote.SupabaseClient;
import com.habittracker.app.data.remote.api.AuthApi;
import com.habittracker.app.data.remote.dto.AuthResponse;
import com.habittracker.app.data.remote.dto.SignInRequest;
import com.habittracker.app.data.remote.dto.SignUpRequest;
import com.habittracker.app.util.TokenManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages authentication state and Supabase auth operations.
 * Exposes auth state as LiveData for reactive UI observation.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final TokenManager tokenManager;
    private final SupabaseClient supabaseClient;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    public enum AuthState {
        AUTHENTICATED,      // Has valid token
        UNAUTHENTICATED,    // No token, needs login
        LOCAL_ONLY,         // Using app without account (delayed signup)
        LOADING,            // Auth operation in progress
        ERROR               // Auth operation failed
    }

    /** Callback for auth operations */
    public interface AuthCallback {
        void onSuccess(String userId);
        void onError(String message);
    }

    public AuthRepository(TokenManager tokenManager, SupabaseClient supabaseClient) {
        this.tokenManager = tokenManager;
        this.supabaseClient = supabaseClient;

        // Determine initial auth state
        if (tokenManager.hasTokens()) {
            authState.setValue(AuthState.AUTHENTICATED);
        } else {
            authState.setValue(AuthState.UNAUTHENTICATED);
        }
    }

    public LiveData<AuthState> getAuthState() { return authState; }

    public boolean isLoggedIn() {
        return tokenManager.hasTokens();
    }

    public String getCurrentUserId() {
        return tokenManager.getUserId();
    }

    public String getUserName() {
        return tokenManager.getUserName();
    }

    public String getUserEmail() {
        return tokenManager.getUserEmail();
    }

    public boolean shouldPromptSignup() {
        return tokenManager.shouldPromptSignup();
    }

    /** Record first use of the app (for delayed signup timer) */
    public void recordFirstUse() {
        tokenManager.recordFirstUse();
    }

    /**
     * Sign up with email/password.
     * On success, stores tokens and user info.
     */
    public void signUp(String email, String password, String displayName, AuthCallback callback) {
        authState.postValue(AuthState.LOADING);

        AuthApi api = supabaseClient.getAuthApi();
        SignUpRequest request = new SignUpRequest(email, password, displayName);

        Log.d(TAG, "Attempting signup for: " + email);

        api.signUp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Sign up successful for: " + email);
                    AuthResponse body = response.body();
                    handleAuthSuccess(body, displayName);
                    callback.onSuccess(body.getUser().getId());
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    String error = "Sign up failed: " + response.code() + " | Body: " + errorBody;
                    Log.e(TAG, error);
                    authState.postValue(AuthState.ERROR);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                authState.postValue(AuthState.ERROR);
                callback.onError(error);
            }
        });
    }

    /**
     * Sign in with email/password.
     */
    public void signIn(String email, String password, AuthCallback callback) {
        authState.postValue(AuthState.LOADING);

        AuthApi api = supabaseClient.getAuthApi();
        SignInRequest request = new SignInRequest(email, password);

        Log.d(TAG, "Attempting sign in for: " + email);

        api.signIn("password", request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Sign in successful for: " + email);
                    AuthResponse body = response.body();
                    handleAuthSuccess(body, null);
                    callback.onSuccess(body.getUser().getId());
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    String error = "Sign in failed: " + response.code() + " | Body: " + errorBody;
                    Log.e(TAG, error);
                    authState.postValue(AuthState.ERROR);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                authState.postValue(AuthState.ERROR);
                callback.onError(error);
            }
        });
    }

    /**
     * Sign out — clear tokens and user info.
     */
    public void signOut() {
        tokenManager.logout();
        authState.postValue(AuthState.UNAUTHENTICATED);
    }

    /**
     * Switch to local-only mode (delayed signup).
     * App works without an account, data stored in Room only.
     */
    public void useLocalOnly() {
        authState.postValue(AuthState.LOCAL_ONLY);
        tokenManager.recordFirstUse();
    }

    /** Handle successful auth response — store tokens and user info */
    private void handleAuthSuccess(AuthResponse response, String displayName) {
        tokenManager.saveTokens(response.getAccessToken(), response.getRefreshToken());

        if (response.getUser() != null) {
            String name = displayName != null ? displayName : response.getUser().getEmail();
            tokenManager.saveUserInfo(
                    response.getUser().getId(),
                    response.getUser().getEmail(),
                    name
            );
        }

        authState.postValue(AuthState.AUTHENTICATED);
    }
}
