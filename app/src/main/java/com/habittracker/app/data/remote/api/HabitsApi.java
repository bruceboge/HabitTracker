package com.habittracker.app.data.remote.api;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for Supabase PostgREST habits + habit_state endpoints.
 * 
 * PostgREST uses query parameters for filtering:
 * - ?user_id=eq.{uuid} → WHERE user_id = uuid
 * - ?id=eq.{uuid} → WHERE id = uuid
 * - select=* → SELECT *
 */
public interface HabitsApi {

    // --- Habits CRUD ---

    @GET("rest/v1/habits")
    Call<List<JsonObject>> getHabits(
            @Query("user_id") String userIdFilter,  // e.g. "eq.uuid-here"
            @Query("is_archived") String archivedFilter, // e.g. "eq.false"
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/habits")
    Call<List<JsonObject>> insertHabit(@Body JsonObject habit);

    @PATCH("rest/v1/habits")
    Call<List<JsonObject>> updateHabit(
            @Query("id") String idFilter, // e.g. "eq.uuid-here"
            @Body JsonObject updates
    );

    @DELETE("rest/v1/habits")
    Call<Void> deleteHabit(@Query("id") String idFilter);

    // --- Habit State CRUD ---

    @GET("rest/v1/habit_state")
    Call<List<JsonObject>> getHabitStates(
            @Query("habit_id") String habitIdFilter,
            @Query("select") String select
    );

    @POST("rest/v1/habit_state")
    Call<List<JsonObject>> insertHabitState(@Body JsonObject state);

    @PATCH("rest/v1/habit_state")
    Call<List<JsonObject>> updateHabitState(
            @Query("habit_id") String habitIdFilter,
            @Body JsonObject updates
    );
}
