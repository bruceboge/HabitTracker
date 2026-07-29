package com.habittracker.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for Supabase token refresh: POST /auth/v1/token?grant_type=refresh_token
 */
public class RefreshRequest {

    @SerializedName("refresh_token")
    private String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
