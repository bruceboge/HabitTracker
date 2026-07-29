package com.habittracker.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for Supabase signin: POST /auth/v1/token?grant_type=password
 */
public class SignInRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public SignInRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
