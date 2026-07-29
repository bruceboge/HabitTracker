package com.habittracker.app.data.remote;

import android.util.Log;

import com.habittracker.app.BuildConfig;
import com.habittracker.app.data.remote.api.AuthApi;
import com.habittracker.app.data.remote.api.DailyLogsApi;
import com.habittracker.app.data.remote.api.HabitsApi;
import com.habittracker.app.util.TokenManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit client for Supabase REST API.
 * 
 * Handles:
 * - Adding apikey header to every request (Supabase requires this)
 * - Adding Authorization Bearer token for authenticated requests
 * - Auto-refreshing expired tokens on 401 responses
 * - Request/response logging in debug builds
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static volatile SupabaseClient INSTANCE;

    private final Retrofit retrofit;
    private final Retrofit authRetrofit; // Separate instance for auth (no Bearer token)

    private SupabaseClient(TokenManager tokenManager) {
        // Logging interceptor for debug builds
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Auth interceptor: adds apikey + Bearer token to every request
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json");

            // Add Bearer token if available (not needed for signup/signin)
            String token = tokenManager.getAccessToken();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            // PostgREST requires Prefer header for upserts
            if ("POST".equals(original.method()) || "PATCH".equals(original.method())) {
                builder.header("Prefer", "return=representation");
            }

            return chain.proceed(builder.build());
        };

        // Token refresh interceptor: retries on 401
        Interceptor refreshInterceptor = chain -> {
            Response response = chain.proceed(chain.request());

            if (response.code() == 401 && tokenManager.getRefreshToken() != null) {
                Log.d(TAG, "Got 401 — attempting token refresh");
                response.close();

                // Try to refresh the token
                boolean refreshed = refreshToken(tokenManager);
                if (refreshed) {
                    // Retry original request with new token
                    Request newRequest = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                            .build();
                    return chain.proceed(newRequest);
                }
            }

            return response;
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(refreshInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Auth-only client (no Bearer token, just apikey + Authorization: Bearer <ANON_KEY>)
        OkHttpClient authClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                            .header("Content-Type", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        authRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static SupabaseClient getInstance(TokenManager tokenManager) {
        if (INSTANCE == null) {
            synchronized (SupabaseClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SupabaseClient(tokenManager);
                }
            }
        }
        return INSTANCE;
    }

    /** Get the Auth API (signup, signin, refresh — uses auth-only client without Bearer) */
    public AuthApi getAuthApi() {
        return authRetrofit.create(AuthApi.class);
    }

    /** Get the Habits API (CRUD via PostgREST — uses authenticated client) */
    public HabitsApi getHabitsApi() {
        return retrofit.create(HabitsApi.class);
    }

    /** Get the Daily Logs API (uses authenticated client) */
    public DailyLogsApi getDailyLogsApi() {
        return retrofit.create(DailyLogsApi.class);
    }

    /**
     * Attempt to refresh the access token using the stored refresh token.
     * Called automatically by the refresh interceptor on 401.
     */
    private boolean refreshToken(TokenManager tokenManager) {
        try {
            AuthApi authApi = getAuthApi();
            com.habittracker.app.data.remote.dto.RefreshRequest request =
                    new com.habittracker.app.data.remote.dto.RefreshRequest(tokenManager.getRefreshToken());

            retrofit2.Response<com.habittracker.app.data.remote.dto.AuthResponse> response =
                    authApi.refreshToken("refresh_token", request).execute();

            if (response.isSuccessful() && response.body() != null) {
                com.habittracker.app.data.remote.dto.AuthResponse body = response.body();
                tokenManager.saveTokens(body.getAccessToken(), body.getRefreshToken());
                Log.d(TAG, "Token refreshed successfully");
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Token refresh failed", e);
        }
        return false;
    }
}
