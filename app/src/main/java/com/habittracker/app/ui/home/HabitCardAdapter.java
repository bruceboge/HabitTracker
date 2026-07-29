package com.habittracker.app.ui.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.habittracker.app.R;
import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.domain.model.Habit;
import com.habittracker.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for habit cards on the home screen.
 * Each card shows: icon, name, subtitle, action button, heatmap grid, difficulty label, streak.
 */
public class HabitCardAdapter extends RecyclerView.Adapter<HabitCardAdapter.ViewHolder> {

    public interface HabitCardListener {
        void onCardClicked(HabitEntity habit);
        void onActionClicked(HabitEntity habit, HabitStateEntity state);
    }

    private List<HabitEntity> habits = new ArrayList<>();
    private List<HabitStateEntity> states = new ArrayList<>();
    private final HabitRepository repository;
    private final LifecycleOwner lifecycleOwner;
    private HabitCardListener listener;

    public HabitCardAdapter(HabitRepository repository, LifecycleOwner lifecycleOwner) {
        this.repository = repository;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setListener(HabitCardListener listener) {
        this.listener = listener;
    }

    public void setData(List<HabitEntity> habits, List<HabitStateEntity> states) {
        this.habits = habits != null ? habits : new ArrayList<>();
        this.states = states != null ? states : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HabitEntity habit = habits.get(position);

        // Find matching state
        HabitStateEntity state = null;
        for (HabitStateEntity s : states) {
            if (s.habitId.equals(habit.id)) {
                state = s;
                break;
            }
        }

        holder.bind(habit, state);
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView habitIcon;
        private final TextView habitName;
        private final TextView habitSubtitle;
        private final MaterialButton actionButton;
        private final HeatmapView heatmapView;
        private final TextView difficultyLabel;
        private final TextView streakText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            habitIcon = itemView.findViewById(R.id.habit_icon);
            habitName = itemView.findViewById(R.id.habit_name);
            habitSubtitle = itemView.findViewById(R.id.habit_subtitle);
            actionButton = itemView.findViewById(R.id.action_button);
            heatmapView = itemView.findViewById(R.id.heatmap_view);
            difficultyLabel = itemView.findViewById(R.id.difficulty_label);
            streakText = itemView.findViewById(R.id.streak_text);
        }

        void bind(HabitEntity habit, HabitStateEntity state) {
            // Icon with color tint
            habitIcon.setText(habit.icon);
            try {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadius(12 * itemView.getResources().getDisplayMetrics().density);
                int color = Color.parseColor(habit.color);
                bg.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
                habitIcon.setBackground(bg);
            } catch (Exception ignored) {}

            // Name
            habitName.setText(habit.name);

            // Subtitle (task description from difficulty level)
            Habit domainHabit = HabitRepository.toDomain(habit);
            double diffLevel = state != null ? state.difficultyLevel : 0.3;
            habitSubtitle.setText(domainHabit.getTaskDescription(diffLevel));

            // Difficulty label
            difficultyLabel.setText(String.format("Level: %s", domainHabit.getDifficultyLabel(diffLevel)));

            // Streak
            int streak = state != null ? state.streak : 0;
            if (streak > 0) {
                streakText.setText(String.format("🔥 %d days", streak));
                streakText.setVisibility(View.VISIBLE);
            } else {
                streakText.setVisibility(View.GONE);
            }

            // Action button state
            String today = DateUtils.formatIso(LocalDate.now());
            // Check if already completed today via heatmap data load below
            actionButton.setText("+ Do");

            // Load heatmap data
            LocalDate createdAt = DateUtils.parseIso(habit.createdAt);
            repository.getHeatmapData(habit.id).observe(lifecycleOwner, logs -> {
                heatmapView.setData(logs, habit.color, createdAt);

                // Update action button based on today's completion
                boolean completedToday = false;
                if (logs != null) {
                    for (DailyLogEntity log : logs) {
                        if (today.equals(log.logDate) && log.completed) {
                            completedToday = true;
                            break;
                        }
                    }
                }

                if (completedToday) {
                    actionButton.setText("✓ Done");
                    actionButton.setAlpha(0.6f);
                } else {
                    actionButton.setText("+ Do");
                    actionButton.setAlpha(1.0f);
                }
            });

            // Click listeners
            final HabitStateEntity finalState = state;
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCardClicked(habit);
            });

            actionButton.setOnClickListener(v -> {
                if (listener != null) listener.onActionClicked(habit, finalState);
            });
        }
    }
}
