package com.habittracker.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for Supabase signup: POST /auth/v1/signup
 */
public class SignUpRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("options")
    private Options options;

    public SignUpRequest(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.options = new Options(new UserMetadata(displayName));
    }

    public static class Options {
        @SerializedName("data")
        private UserMetadata data;

        public Options(UserMetadata data) {
            this.data = data;
        }
    }

    public static class UserMetadata {
        @SerializedName("display_name")
        private String displayName;

        public UserMetadata(String displayName) {
            this.displayName = displayName;
        }
    }
}
