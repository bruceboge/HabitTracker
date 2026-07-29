import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

/**
 * Supabase Edge Function: Nightly Difficulty Recalculation
 * 
 * Runs the adaptive difficulty algorithm for ALL active habits.
 * Triggered by pg_cron at 3 AM UTC daily.
 * 
 * This is the authoritative calculation — the on-device calculation
 * is a preview. This function ensures consistency across devices.
 */

// Algorithm constants (mirrors DifficultyEngine.java)
const EMA_ALPHA = 0.25
const VOLATILITY_ALPHA = 0.25
const BASE_STEP = 0.05
const MAX_STEP = 0.15
const SHRINK_MULTIPLIER = 1.5
const THRESHOLD_CRUISING = 0.80
const THRESHOLD_STRUGGLING = 0.50
const MIN_DIFFICULTY = 0.05
const MAX_DIFFICULTY = 1.0
const MISS_STREAK_THRESHOLD = 3
const RESTART_DIFFICULTY = 0.05

function updateEma(currentEma: number, newValue: number): number {
  return EMA_ALPHA * newValue + (1.0 - EMA_ALPHA) * currentEma
}

function updateVolatility(currentVol: number, completedValue: number, currentEma: number): number {
  const deviation = Math.abs(completedValue - currentEma)
  return VOLATILITY_ALPHA * deviation + (1.0 - VOLATILITY_ALPHA) * currentVol
}

function calculateAdjustment(ema: number, volatility: number, effortLevel: number): number {
  const volatilityDamper = Math.max(0.3, Math.min(1.0, 1.0 - (volatility * 0.7)))
  let step = BASE_STEP * volatilityDamper

  if (ema > THRESHOLD_CRUISING) {
    let adjustment = step
    if (effortLevel === 3) adjustment *= 1.3      // Easy → increase faster
    else if (effortLevel === 1) adjustment *= 0.3  // Hard → hold back
    return Math.min(adjustment, MAX_STEP)
  } else if (ema < THRESHOLD_STRUGGLING) {
    let adjustment = -step * SHRINK_MULTIPLIER
    if (effortLevel === 1) adjustment *= 1.3       // Hard + struggling → shrink more
    return Math.max(adjustment, -MAX_STEP)
  }
  return 0.0 // Goldilocks zone
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value))
}

Deno.serve(async (req) => {
  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const serviceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    
    const supabase = createClient(supabaseUrl, serviceKey)

    // Get today's date
    const today = new Date().toISOString().split('T')[0]

    // Fetch all active habit states with their habits
    const { data: states, error: statesError } = await supabase
      .from('habit_state')
      .select('*, habits!inner(id, is_archived)')

    if (statesError) throw statesError

    let processed = 0
    let adjusted = 0

    for (const state of (states || [])) {
      if (state.habits?.is_archived) continue

      // Get today's log for this habit
      const { data: logs } = await supabase
        .from('daily_logs')
        .select('*')
        .eq('habit_id', state.habit_id)
        .eq('log_date', today)
        .limit(1)

      const todayLog = logs?.[0]
      const completed = todayLog?.completed ?? false
      const effortLevel = todayLog?.effort_level ?? 0
      const completedValue = completed ? 1.0 : 0.0

      // Update EMA
      const newEma = updateEma(state.completion_rate_ema, completedValue)
      const newVolatility = updateVolatility(state.volatility, completedValue, state.completion_rate_ema)

      // Update streaks
      let streak = state.streak
      let bestStreak = state.best_streak
      let consecutiveMisses = state.consecutive_misses
      let totalCompletions = state.total_completions
      let restartWinPending = state.restart_win_pending

      if (completed) {
        streak += 1
        consecutiveMisses = 0
        totalCompletions += 1
        if (streak > bestStreak) bestStreak = streak
        if (restartWinPending) restartWinPending = false
      } else {
        consecutiveMisses += 1
        if (consecutiveMisses >= 2) streak = 0
      }

      // Calculate difficulty
      let newDifficulty = state.difficulty_level
      if (consecutiveMisses >= MISS_STREAK_THRESHOLD) {
        newDifficulty = RESTART_DIFFICULTY
        restartWinPending = true
      } else {
        const adjustment = calculateAdjustment(newEma, newVolatility, effortLevel)
        if (adjustment !== 0) adjusted++
        newDifficulty = clamp(state.difficulty_level + adjustment, MIN_DIFFICULTY, MAX_DIFFICULTY)
      }

      // Update state in database
      const { error: updateError } = await supabase
        .from('habit_state')
        .update({
          completion_rate_ema: newEma,
          volatility: newVolatility,
          difficulty_level: newDifficulty,
          streak,
          best_streak: bestStreak,
          total_completions: totalCompletions,
          consecutive_misses: consecutiveMisses,
          restart_win_pending: restartWinPending,
          last_adjusted_at: today,
          ...(completed ? { last_completed_at: today } : {})
        })
        .eq('habit_id', state.habit_id)

      if (updateError) {
        console.error(`Error updating habit ${state.habit_id}:`, updateError)
      }

      processed++
    }

    return new Response(
      JSON.stringify({
        success: true,
        processed,
        adjusted,
        date: today
      }),
      { headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Recalculation failed:', error)
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
