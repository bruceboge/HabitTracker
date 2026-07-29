package com.habittracker.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.local.dao.DailyLogDao;
import com.habittracker.app.data.local.dao.HabitDao;
import com.habittracker.app.data.local.dao.HabitStateDao;
import com.habittracker.app.data.local.dao.ProfileDao;
import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.local.entity.ProfileEntity;
import com.habittracker.app.domain.algorithm.DifficultyEngine;
import com.habittracker.app.domain.model.DailyLog;
import com.habittracker.app.domain.model.Habit;
import com.habittracker.app.domain.model.HabitState;
import com.habittracker.app.domain.model.HabitWithState;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Repository for habit operations — the main data layer the UI talks to.
 * 
 * Offline-first principle:
 * - ALL reads come from Room (local DB)
 * - ALL writes go to Room first, then marked for sync
 * - SyncManager handles pushing to Supabase asynchronously
 */
public class HabitRepository {

    private static final String TAG = "HabitRepository";

    private final HabitDao habitDao;
    private final HabitStateDao habitStateDao;
    private final DailyLogDao dailyLogDao;
    private final ProfileDao profileDao;
    private final Executor executor;

    public HabitRepository(AppDatabase db) {
        this.habitDao = db.habitDao();
        this.habitStateDao = db.habitStateDao();
        this.dailyLogDao = db.dailyLogDao();
        this.profileDao = db.profileDao();
        this.executor = Executors.newFixedThreadPool(4);
    }

    // ===================== HABIT CRUD =====================

    /** Get all active habits (LiveData for reactive UI) */
    public LiveData<List<HabitEntity>> getActiveHabits() {
        return habitDao.getActiveHabits();
    }

    /** Get a single habit by ID */
    public LiveData<HabitEntity> getHabitById(String habitId) {
        return habitDao.getById(habitId);
    }

    /** Get habit count */
    public LiveData<Integer> getActiveHabitCount() {
        return habitDao.getActiveHabitCount();
    }

    /**
     * Create a new habit with its initial algorithm state.
     * Runs on background thread.
     */
    public void createHabit(Habit habit, Runnable onComplete) {
        executor.execute(() -> {
            // Convert domain model to entity
            HabitEntity entity = toEntity(habit);
            entity.synced = false;
            habitDao.insert(entity);

            // Create initial algorithm state
            HabitStateEntity state = new HabitStateEntity();
            state.habitId = habit.getId();
            state.synced = false;
            habitStateDao.upsert(state);

            Log.d(TAG, "Created habit: " + habit.getName() + " (id=" + habit.getId() + ")");

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /** Update an existing habit */
    public void updateHabit(Habit habit, Runnable onComplete) {
        executor.execute(() -> {
            HabitEntity entity = toEntity(habit);
            entity.synced = false;
            entity.updatedAt = System.currentTimeMillis();
            habitDao.update(entity);

            if (onComplete != null) onComplete.run();
        });
    }

    /** Archive a habit (soft delete) */
    public void archiveHabit(String habitId) {
        executor.execute(() -> habitDao.archive(habitId));
    }

    /** Hard delete a habit and all its data */
    public void deleteHabit(String habitId) {
        executor.execute(() -> habitDao.delete(habitId));
    }

    // ===================== DAILY LOG / CHECK-IN =====================

    /**
     * Log a habit completion (the core check-in action).
     * This is the most critical write path — it:
     * 1. Writes the daily log to Room
     * 2. Runs DifficultyEngine locally for immediate feedback
     * 3. Updates habit state in Room
     * 4. Awards points
     * 5. Marks everything for sync
     */
    public void logCompletion(String habitId, boolean completed, int effortLevel,
                              CompletionCallback callback) {
        executor.execute(() -> {
            try {
                // Get current state
                HabitStateEntity stateEntity = habitStateDao.getByHabitIdSync(habitId);
                if (stateEntity == null) {
                    stateEntity = new HabitStateEntity();
                    stateEntity.habitId = habitId;
                }

                // Create daily log
                String today = DateUtils.formatIso(LocalDate.now());
                DailyLogEntity logEntity = new DailyLogEntity();
                logEntity.id = UUID.randomUUID().toString();
                logEntity.habitId = habitId;
                logEntity.logDate = today;
                logEntity.completed = completed;
                logEntity.effortLevel = effortLevel;
                logEntity.difficultyAtTime = stateEntity.difficultyLevel;
                logEntity.synced = false;

                dailyLogDao.insertOrUpdate(logEntity);

                // Run difficulty algorithm locally
                HabitState domainState = toDomainState(stateEntity);
                DailyLog domainLog = toDomainLog(logEntity);
                HabitState updatedState = DifficultyEngine.recalculate(domainState, domainLog);

                // Determine if this is a restart win
                boolean isRestartWin = completed && stateEntity.restartWinPending;
                boolean difficultyIncreased = updatedState.getDifficultyLevel() > stateEntity.difficultyLevel;

                // Save updated state
                updateStateEntity(stateEntity, updatedState);
                stateEntity.synced = false;
                habitStateDao.upsert(stateEntity);

                // Award points
                int pointsEarned = 0;
                if (completed) {
                    pointsEarned = calculatePoints(updatedState, isRestartWin, difficultyIncreased);
                    awardPoints(pointsEarned);
                }

                Log.d(TAG, "Logged completion for habit " + habitId +
                        " | completed=" + completed +
                        " | difficulty=" + String.format("%.2f", updatedState.getDifficultyLevel()) +
                        " | EMA=" + String.format("%.2f", updatedState.getCompletionRateEma()) +
                        " | streak=" + updatedState.getStreak());

                if (callback != null) {
                    callback.onComplete(pointsEarned, isRestartWin, difficultyIncreased,
                            updatedState.getStreak());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error logging completion", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // ===================== HEATMAP DATA =====================

    /** Get heatmap data for a habit (last 91 days) */
    public LiveData<List<DailyLogEntity>> getHeatmapData(String habitId) {
        String startDate = DateUtils.formatIso(DateUtils.getHeatmapStartDate());
        String endDate = DateUtils.formatIso(LocalDate.now());
        return dailyLogDao.getLogsForHabit(habitId, startDate, endDate);
    }

    /** Get today's completion count for dashboard progress bar */
    public LiveData<Integer> getCompletedTodayCount() {
        return dailyLogDao.getCompletedTodayCount(DateUtils.formatIso(LocalDate.now()));
    }

    // ===================== HABIT STATE =====================

    /** Get algorithm state for a habit */
    public LiveData<HabitStateEntity> getHabitState(String habitId) {
        return habitStateDao.getByHabitId(habitId);
    }

    // ===================== PROFILE =====================

    /** Get or create user profile */
    public LiveData<ProfileEntity> getProfile() {
        return profileDao.getProfile();
    }

    public void ensureProfileExists(String userId, String displayName) {
        executor.execute(() -> {
            ProfileEntity existing = profileDao.getByUserId(userId);
            if (existing == null) {
                ProfileEntity profile = new ProfileEntity();
                profile.userId = userId;
                profile.displayName = displayName;
                profileDao.upsert(profile);
            }
        });
    }

    // ===================== POINTS & REWARDS =====================

    private int calculatePoints(HabitState state, boolean isRestartWin, boolean difficultyIncreased) {
        int points = Constants.POINTS_BASE_COMPLETION;

        // Streak bonus (starts at day 3)
        if (state.getStreak() >= Constants.STREAK_BONUS_START_DAY) {
            int bonus = state.getStreak() * Constants.POINTS_STREAK_BONUS_PER_DAY;
            points += Math.min(bonus, Constants.POINTS_STREAK_BONUS_CAP);
        }

        // Restart win bonus
        if (isRestartWin) {
            points += Constants.POINTS_RESTART_WIN;
        }

        // Difficulty upgrade bonus
        if (difficultyIncreased) {
            points += Constants.POINTS_DIFFICULTY_UPGRADE;
        }

        return points;
    }

    private void awardPoints(int points) {
        ProfileEntity profile = profileDao.getProfileSync();
        if (profile != null) {
            profileDao.addPoints(profile.userId, points);
            profileDao.addXp(profile.userId, points);

            // Check for level up
            ProfileEntity updated = profileDao.getProfileSync();
            if (updated != null) {
                int expectedLevel = (updated.xp / Constants.XP_PER_LEVEL) + 1;
                if (expectedLevel > updated.level) {
                    profileDao.updateLevel(updated.userId, expectedLevel);
                }
            }
        }
    }

    // ===================== ENTITY ↔ DOMAIN CONVERTERS =====================

    private HabitEntity toEntity(Habit habit) {
        HabitEntity e = new HabitEntity();
        e.id = habit.getId();
        e.userId = habit.getUserId();
        e.name = habit.getName();
        e.icon = habit.getIcon();
        e.color = habit.getColor();
        e.cadence = habit.getCadence();
        e.cadenceDays = habit.getCadenceDays() != null ?
                habit.getCadenceDays().stream().map(String::valueOf).collect(Collectors.joining(",")) :
                "1,2,3,4,5,6,7";
        e.difficultyTiny = habit.getDifficultyTiny();
        e.difficultyNormal = habit.getDifficultyNormal();
        e.difficultyStretch = habit.getDifficultyStretch();
        e.isArchived = habit.isArchived();
        e.createdAt = DateUtils.formatIso(habit.getCreatedAt());
        return e;
    }

    public static Habit toDomain(HabitEntity e) {
        Habit h = new Habit();
        h.setId(e.id);
        h.setUserId(e.userId);
        h.setName(e.name);
        h.setIcon(e.icon);
        h.setColor(e.color);
        h.setCadence(e.cadence);
        if (e.cadenceDays != null && !e.cadenceDays.isEmpty()) {
            h.setCadenceDays(Arrays.stream(e.cadenceDays.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        h.setDifficultyTiny(e.difficultyTiny);
        h.setDifficultyNormal(e.difficultyNormal);
        h.setDifficultyStretch(e.difficultyStretch);
        h.setArchived(e.isArchived);
        h.setCreatedAt(DateUtils.parseIso(e.createdAt));
        return h;
    }

    private HabitState toDomainState(HabitStateEntity e) {
        return new HabitState(
                e.habitId, e.difficultyLevel, e.completionRateEma, e.volatility,
                e.streak, e.bestStreak, e.totalCompletions, e.consecutiveMisses,
                e.restartWinPending,
                e.lastCompletedAt != null ? DateUtils.parseIso(e.lastCompletedAt) : null,
                e.lastAdjustedAt != null ? DateUtils.parseIso(e.lastAdjustedAt) : null
        );
    }

    private DailyLog toDomainLog(DailyLogEntity e) {
        return new DailyLog(e.id, e.habitId, DateUtils.parseIso(e.logDate),
                e.completed, e.effortLevel, e.difficultyAtTime, e.synced);
    }

    private void updateStateEntity(HabitStateEntity entity, HabitState domain) {
        entity.difficultyLevel = domain.getDifficultyLevel();
        entity.completionRateEma = domain.getCompletionRateEma();
        entity.volatility = domain.getVolatility();
        entity.streak = domain.getStreak();
        entity.bestStreak = domain.getBestStreak();
        entity.totalCompletions = domain.getTotalCompletions();
        entity.consecutiveMisses = domain.getConsecutiveMisses();
        entity.restartWinPending = domain.isRestartWinPending();
        entity.lastCompletedAt = domain.getLastCompletedAt() != null ?
                DateUtils.formatIso(domain.getLastCompletedAt()) : null;
        entity.lastAdjustedAt = domain.getLastAdjustedAt() != null ?
                DateUtils.formatIso(domain.getLastAdjustedAt()) : null;
    }

    // ===================== CALLBACKS =====================

    public interface CompletionCallback {
        void onComplete(int pointsEarned, boolean isRestartWin, boolean difficultyIncreased, int streak);
        void onError(String message);
    }
}
