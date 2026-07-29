package com.habittracker.app.data.remote.api;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for Supabase PostgREST daily_logs endpoints.
 */
public interface DailyLogsApi {

    @GET("rest/v1/daily_logs")
    Call<List<JsonObject>> getLogs(
            @Query("habit_id") String habitIdFilter,
            @Query("log_date") String dateFilter,    // e.g. "gte.2026-01-01"
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/daily_logs")
    Call<List<JsonObject>> insertLog(@Body JsonObject log);

    /**
     * Upsert a daily log (uses PostgREST's on_conflict).
     * Requires the Prefer: resolution=merge-duplicates header (added by SupabaseClient).
     */
    @POST("rest/v1/daily_logs")
    Call<List<JsonObject>> upsertLog(
            @Body JsonObject log,
            @Query("on_conflict") String onConflict // "habit_id,log_date"
    );
}
