-- Habit Tracker — Initial Schema
-- Run via Supabase SQL editor or apply as a migration

-- Enable UUID extension (not needed for gen_random_uuid(), but keeping comment placeholder)

-- ===================== TABLES =====================

-- Profiles (extends Supabase auth.users)
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    xp INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    points INTEGER DEFAULT 0,
    avatar_config JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Habits
CREATE TABLE habits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    icon TEXT DEFAULT '⭐',
    color TEXT DEFAULT '#4CAF50',
    cadence TEXT DEFAULT 'daily',
    cadence_days TEXT DEFAULT '1,2,3,4,5,6,7',
    difficulty_tiny TEXT,
    difficulty_normal TEXT,
    difficulty_stretch TEXT,
    is_archived BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Habit State (algorithm working data — one per habit)
CREATE TABLE habit_state (
    habit_id UUID PRIMARY KEY REFERENCES habits(id) ON DELETE CASCADE,
    difficulty_level DOUBLE PRECISION DEFAULT 0.3,
    completion_rate_ema DOUBLE PRECISION DEFAULT 0.5,
    volatility DOUBLE PRECISION DEFAULT 0.0,
    streak INTEGER DEFAULT 0,
    best_streak INTEGER DEFAULT 0,
    total_completions INTEGER DEFAULT 0,
    consecutive_misses INTEGER DEFAULT 0,
    restart_win_pending BOOLEAN DEFAULT false,
    last_completed_at DATE,
    last_adjusted_at DATE
);

-- Daily Logs
CREATE TABLE daily_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID REFERENCES habits(id) ON DELETE CASCADE NOT NULL,
    log_date DATE NOT NULL,
    completed BOOLEAN DEFAULT false,
    effort_level INTEGER CHECK (effort_level BETWEEN 0 AND 3),
    difficulty_at_time DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(habit_id, log_date)
);

-- Milestones / Badges
CREATE TABLE milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    habit_id UUID REFERENCES habits(id) ON DELETE CASCADE,
    milestone_type TEXT NOT NULL,
    earned_at TIMESTAMPTZ DEFAULT now()
);

-- ===================== INDEXES =====================

CREATE INDEX idx_daily_logs_habit_date ON daily_logs(habit_id, log_date DESC);
CREATE INDEX idx_habits_user ON habits(user_id);
CREATE INDEX idx_habits_user_active ON habits(user_id) WHERE is_archived = false;
CREATE INDEX idx_milestones_user ON milestones(user_id);

-- ===================== ROW LEVEL SECURITY =====================

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE habit_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE milestones ENABLE ROW LEVEL SECURITY;

-- Profiles: users can only access their own
CREATE POLICY "Users can view own profile"
    ON profiles FOR SELECT USING (id = auth.uid());
CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE USING (id = auth.uid());
CREATE POLICY "Users can insert own profile"
    ON profiles FOR INSERT WITH CHECK (id = auth.uid());

-- Habits: users can only CRUD their own
CREATE POLICY "Users can select own habits"
    ON habits FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "Users can insert own habits"
    ON habits FOR INSERT WITH CHECK (user_id = auth.uid());
CREATE POLICY "Users can update own habits"
    ON habits FOR UPDATE USING (user_id = auth.uid());
CREATE POLICY "Users can delete own habits"
    ON habits FOR DELETE USING (user_id = auth.uid());

-- Habit State: access through habit ownership
CREATE POLICY "Users can select own habit state"
    ON habit_state FOR SELECT
    USING (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));
CREATE POLICY "Users can insert own habit state"
    ON habit_state FOR INSERT
    WITH CHECK (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));
CREATE POLICY "Users can update own habit state"
    ON habit_state FOR UPDATE
    USING (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));

-- Daily Logs: access through habit ownership
CREATE POLICY "Users can select own logs"
    ON daily_logs FOR SELECT
    USING (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));
CREATE POLICY "Users can insert own logs"
    ON daily_logs FOR INSERT
    WITH CHECK (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));
CREATE POLICY "Users can update own logs"
    ON daily_logs FOR UPDATE
    USING (habit_id IN (SELECT id FROM habits WHERE user_id = auth.uid()));

-- Milestones: users can view their own
CREATE POLICY "Users can view own milestones"
    ON milestones FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "Users can insert own milestones"
    ON milestones FOR INSERT WITH CHECK (user_id = auth.uid());

-- ===================== TRIGGERS =====================

-- Auto-create profile on signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO profiles (id, display_name)
    VALUES (NEW.id, NEW.raw_user_meta_data->>'display_name');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();
