package com.habittracker.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Supabase GoTrue auth endpoints (signup, signin, refresh).
 */
public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("user")
    private UserInfo user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public int getExpiresIn() { return expiresIn; }
    public UserInfo getUser() { return user; }

    /**
     * Nested user object returned by Supabase auth.
     */
    public static class UserInfo {
        @SerializedName("id")
        private String id;

        @SerializedName("email")
        private String email;

        @SerializedName("created_at")
        private String createdAt;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getCreatedAt() { return createdAt; }
    }
}
