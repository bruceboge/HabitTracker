package com.habittracker.app.data.remote.api;

import com.habittracker.app.data.remote.dto.AuthResponse;
import com.habittracker.app.data.remote.dto.RefreshRequest;
import com.habittracker.app.data.remote.dto.SignInRequest;
import com.habittracker.app.data.remote.dto.SignUpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for Supabase GoTrue Auth endpoints.
 * Base URL: {SUPABASE_URL}/auth/v1/
 */
public interface AuthApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest request);

    @POST("auth/v1/token")
    Call<AuthResponse> signIn(
            @Query("grant_type") String grantType,
            @Body SignInRequest request
    );

    @POST("auth/v1/token")
    Call<AuthResponse> refreshToken(
            @Query("grant_type") String grantType,
            @Body RefreshRequest request
    );
}
