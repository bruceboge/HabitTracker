package com.habittracker.app.domain.algorithm;

import com.habittracker.app.domain.model.DailyLog;
import com.habittracker.app.domain.model.HabitState;

import java.time.LocalDate;

/**
 * The adaptive difficulty engine — the core of the habit tracker's psychology-driven scaling.
 * 
 * Based on:
 * - Flow theory (Csikszentmihalyi): keep challenge matched to skill
 * - Fogg Behavior Model (B=MAP): when someone fails, shrink the task (Ability), don't guilt-trip
 * - Progressive overload: small, not sudden, difficulty increases
 * 
 * Uses an Exponential Moving Average (EMA) for completion rate so recent behavior
 * counts more than behavior from weeks ago. Volatility dampens adjustment magnitude
 * for inconsistent users who need gentler steps.
 * 
 * This class has NO Android dependencies — it's pure Java, testable in isolation.
 */
public class DifficultyEngine {

    // EMA smoothing factor: α = 2/(N+1) where N = effective window size
    // α = 0.25 → ~7-day effective window
    private static final double EMA_ALPHA = 0.25;

    // Volatility smoothing factor (same window as EMA)
    private static final double VOLATILITY_ALPHA = 0.25;

    // Difficulty adjustment parameters
    private static final double BASE_STEP = 0.05;           // 5% base adjustment per day
    private static final double MAX_STEP = 0.15;            // Never jump more than 15%
    private static final double SHRINK_MULTIPLIER = 1.5;    // Shrink faster than grow

    // Flow zone thresholds
    private static final double THRESHOLD_CRUISING = 0.80;  // Above → increase difficulty
    private static final double THRESHOLD_STRUGGLING = 0.50; // Below → decrease difficulty
    // Between 0.50 and 0.80 → Goldilocks zone, hold steady

    // Difficulty bounds
    private static final double MIN_DIFFICULTY = 0.05;      // "Two-minute version"
    private static final double MAX_DIFFICULTY = 1.0;

    // Miss streak parameters
    private static final int MISS_STREAK_THRESHOLD = 3;     // 3+ consecutive misses → auto-shrink
    private static final double RESTART_DIFFICULTY = 0.05;   // Reset to easiest version

    // Effort-based modifiers
    private static final double EFFORT_EASY_BOOST = 1.3;    // Speed up increase if user reports "easy"
    private static final double EFFORT_HARD_DAMPEN = 0.3;   // Slow down increase if user reports "hard"

    /**
     * Core algorithm: recalculate a habit's state after a new daily log.
     * Returns a NEW HabitState — does not mutate the input.
     *
     * @param current   The habit's current algorithm state
     * @param todayLog  Today's log entry (completed + effort)
     * @return          Updated state with new EMA, volatility, difficulty, streaks
     */
    public static HabitState recalculate(HabitState current, DailyLog todayLog) {
        HabitState updated = current.copy();
        double completedValue = todayLog.isCompleted() ? 1.0 : 0.0;

        // 1. Update Exponential Moving Average of completion rate
        double newEma = updateEma(current.getCompletionRateEma(), completedValue);
        updated.setCompletionRateEma(newEma);

        // 2. Update volatility (how much completion swings day-to-day)
        double newVolatility = updateVolatility(
                current.getVolatility(), completedValue, current.getCompletionRateEma());
        updated.setVolatility(newVolatility);

        // 3. Update streak tracking
        updateStreaks(updated, todayLog);

        // 4. Check for miss-streak override FIRST (trumps normal adjustment)
        if (updated.getConsecutiveMisses() >= MISS_STREAK_THRESHOLD) {
            // Auto-shrink to easiest version and flag restart win
            updated.setDifficultyLevel(RESTART_DIFFICULTY);
            updated.setRestartWinPending(true);
        } else {
            // 5. Normal difficulty adjustment
            double adjustment = calculateAdjustment(newEma, newVolatility, todayLog.getEffortLevel());
            double newDifficulty = clampDifficulty(current.getDifficultyLevel() + adjustment);
            updated.setDifficultyLevel(newDifficulty);
        }

        // 6. Handle restart win (completing after 3+ misses)
        if (todayLog.isCompleted() && current.isRestartWinPending()) {
            // The restart win is consumed — UI should show celebration
            updated.setRestartWinPending(false);
            // Don't increase difficulty on a restart win — let them rebuild
        }

        updated.setLastAdjustedAt(LocalDate.now());

        return updated;
    }

    /**
     * Exponential Moving Average update.
     * EMA_today = α × value_today + (1 - α) × EMA_yesterday
     * 
     * With α = 0.25, a single day's value has 25% weight, decaying exponentially.
     * After 7 days, old values contribute ~13% — effectively a rolling 7-day window
     * without needing to store the full history.
     */
    public static double updateEma(double currentEma, double newValue) {
        return EMA_ALPHA * newValue + (1.0 - EMA_ALPHA) * currentEma;
    }

    /**
     * Volatility = EMA of |actual - expected|.
     * Measures how "jittery" the user's completion pattern is.
     * 
     * High volatility (>0.4) → user is inconsistent, needs smaller adjustments
     * Low volatility (<0.2) → user is stable, can handle normal-sized adjustments
     */
    public static double updateVolatility(double currentVolatility, double completedValue, double currentEma) {
        double deviation = Math.abs(completedValue - currentEma);
        return VOLATILITY_ALPHA * deviation + (1.0 - VOLATILITY_ALPHA) * currentVolatility;
    }

    /**
     * Calculate the difficulty adjustment for this day.
     * 
     * Logic:
     * - EMA > 0.80 (cruising): increase difficulty
     * - EMA < 0.50 (struggling): decrease difficulty (faster than increases)
     * - 0.50 ≤ EMA ≤ 0.80 (flow zone): no adjustment — this IS the sweet spot
     * 
     * Modifiers:
     * - High volatility → smaller steps (dampen adjustment magnitude)
     * - Effort "easy" → increase faster
     * - Effort "hard" → hold back on increases, even if EMA is high
     *
     * @return Signed adjustment: positive = harder, negative = easier, 0 = hold
     */
    public static double calculateAdjustment(double ema, double volatility, int effortLevel) {
        // Volatility dampening: high volatility → smaller steps
        // volatilityDamper ranges from 0.3 (very volatile) to 1.0 (perfectly stable)
        double volatilityDamper = 1.0 - (volatility * 0.7);
        volatilityDamper = Math.max(0.3, Math.min(1.0, volatilityDamper));

        double step = BASE_STEP * volatilityDamper;

        if (ema > THRESHOLD_CRUISING) {
            // User is crushing it — nudge difficulty up
            double adjustment = step;

            // Effort modifier for increases
            if (effortLevel == DailyLog.EFFORT_EASY) {
                // User says it's easy AND they're completing consistently → increase faster
                adjustment *= EFFORT_EASY_BOOST;
            } else if (effortLevel == DailyLog.EFFORT_HARD) {
                // User says it's hard even though they're completing → hold back
                // They're at their limit, don't push further yet
                adjustment *= EFFORT_HARD_DAMPEN;
            }

            return Math.min(adjustment, MAX_STEP);

        } else if (ema < THRESHOLD_STRUGGLING) {
            // User is struggling — shrink difficulty
            // Shrink FASTER than grow (asymmetric by design — Fogg's principle)
            double adjustment = -step * SHRINK_MULTIPLIER;

            // If user reports "hard" while struggling, shrink even more aggressively
            if (effortLevel == DailyLog.EFFORT_HARD) {
                adjustment *= 1.3;
            }

            return Math.max(adjustment, -MAX_STEP);

        } else {
            // Goldilocks zone (0.50–0.80) — hold steady
            // This IS the flow state — don't disturb it
            return 0.0;
        }
    }

    /**
     * Update streak counters based on today's log.
     */
    private static void updateStreaks(HabitState state, DailyLog todayLog) {
        if (todayLog.isCompleted()) {
            state.setStreak(state.getStreak() + 1);
            state.setConsecutiveMisses(0);
            state.setTotalCompletions(state.getTotalCompletions() + 1);
            state.setLastCompletedAt(todayLog.getLogDate());

            // Update best streak if current exceeds it
            if (state.getStreak() > state.getBestStreak()) {
                state.setBestStreak(state.getStreak());
            }
        } else {
            // Single miss: no streak reset (streak-anxiety prevention)
            // But track consecutive misses for the 3+ override
            state.setConsecutiveMisses(state.getConsecutiveMisses() + 1);

            // Only reset streak after 2+ misses (1 miss is a "quiet day")
            if (state.getConsecutiveMisses() >= 2) {
                state.setStreak(0);
            }
        }
    }

    /**
     * Clamp difficulty to valid bounds [MIN_DIFFICULTY, MAX_DIFFICULTY].
     */
    private static double clampDifficulty(double difficulty) {
        return Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, difficulty));
    }

    /**
     * Generate an explanation string for the algorithm transparency panel.
     * Tells the user WHY their difficulty is what it is.
     */
    public static String explainState(HabitState state) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Completion rate: %.0f%% (last ~7 days)\n",
                state.getCompletionRateEma() * 100));

        if (state.getCompletionRateEma() > THRESHOLD_CRUISING) {
            sb.append("→ You're consistently completing this habit. ");
            sb.append("We'll gradually increase the challenge.\n");
        } else if (state.getCompletionRateEma() < THRESHOLD_STRUGGLING) {
            sb.append("→ This seems tough right now. ");
            sb.append("We've scaled it back to help you rebuild momentum.\n");
        } else {
            sb.append("→ You're in a good rhythm. ");
            sb.append("We're keeping the difficulty steady.\n");
        }

        if (state.getVolatility() > 0.4) {
            sb.append("Your pattern has been variable lately, ");
            sb.append("so we're making smaller adjustments.\n");
        }

        if (state.isRestartWinPending()) {
            sb.append("🎯 We've reset to the smallest version — ");
            sb.append("just do a tiny bit and you're back on track!\n");
        }

        sb.append(String.format("\nCurrent level: %.0f%%", state.getDifficultyLevel() * 100));

        return sb.toString();
    }
}
