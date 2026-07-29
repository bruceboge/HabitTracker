package com.habittracker.app.ui.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.domain.algorithm.DifficultyEngine;
import com.habittracker.app.domain.model.HabitState;
import com.habittracker.app.ui.home.HeatmapView;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Habit detail screen showing full stats, large heatmap, algorithm transparency,
 * and completion trend chart.
 */
public class HabitDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private HabitRepository repository;
    private String habitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_detail);

        habitId = getIntent().getStringExtra(Constants.EXTRA_HABIT_ID);
        if (habitId == null) { finish(); return; }

        db = AppDatabase.getInstance(this);
        repository = new HabitRepository(db);

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Menu button (archive/delete)
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(this::showMenu);

        // Load habit data
        loadHabitData();
    }

    private void loadHabitData() {
        // Observe habit
        repository.getHabitById(habitId).observe(this, habit -> {
            if (habit == null) return;

            ((TextView) findViewById(R.id.detail_icon)).setText(habit.icon);
            ((TextView) findViewById(R.id.detail_name)).setText(habit.name);

            // Load heatmap
            HeatmapView heatmap = findViewById(R.id.detail_heatmap);
            repository.getHeatmapData(habitId).observe(this, logs -> {
                LocalDate createdAt = DateUtils.parseIso(habit.createdAt);
                heatmap.setData(logs, habit.color, createdAt);

                // Setup completion trend chart
                setupCompletionChart(logs, habit.color);
            });
        });

        // Observe state
        repository.getHabitState(habitId).observe(this, state -> {
            if (state == null) return;

            // Stats row
            ((TextView) findViewById(R.id.stat_streak)).setText(String.valueOf(state.streak));
            ((TextView) findViewById(R.id.stat_best_streak)).setText(String.valueOf(state.bestStreak));
            ((TextView) findViewById(R.id.stat_total)).setText(String.valueOf(state.totalCompletions));

            // Difficulty label
            com.habittracker.app.domain.model.Habit domainHabit = new com.habittracker.app.domain.model.Habit();
            ((TextView) findViewById(R.id.stat_difficulty))
                    .setText(domainHabit.getDifficultyLabel(state.difficultyLevel));

            // Algorithm transparency
            HabitState domainState = new HabitState(
                    state.habitId, state.difficultyLevel, state.completionRateEma,
                    state.volatility, state.streak, state.bestStreak, state.totalCompletions,
                    state.consecutiveMisses, state.restartWinPending, null, null
            );
            String explanation = DifficultyEngine.explainState(domainState);
            ((TextView) findViewById(R.id.algorithm_explanation)).setText(explanation);
        });
    }

    private void setupCompletionChart(List<DailyLogEntity> logs, String habitColor) {
        LineChart chart = findViewById(R.id.completion_chart);

        if (logs == null || logs.isEmpty()) {
            chart.setNoDataText("No data yet");
            chart.setNoDataTextColor(Color.parseColor("#B0B3B8"));
            return;
        }

        // Build entries: running average of completions over time
        List<Entry> entries = new ArrayList<>();
        float runningSum = 0;
        for (int i = 0; i < logs.size(); i++) {
            runningSum += logs.get(i).completed ? 1 : 0;
            float average = runningSum / (i + 1);
            entries.add(new Entry(i, average * 100)); // As percentage
        }

        int color;
        try {
            color = Color.parseColor(habitColor);
        } catch (Exception e) {
            color = Color.parseColor("#4CAF50");
        }

        LineDataSet dataSet = new LineDataSet(entries, "Completion %");
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(40);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Styling
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.getAxisLeft().setTextColor(Color.parseColor("#B0B3B8"));
        chart.getAxisLeft().setGridColor(Color.parseColor("#1F2937"));
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.archive_habit));
        popup.getMenu().add(0, 2, 1, getString(R.string.delete_habit));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                repository.archiveHabit(habitId);
                finish();
                return true;
            } else if (item.getItemId() == 2) {
                confirmDelete();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_habit))
                .setMessage(getString(R.string.confirm_delete))
                .setPositiveButton("Delete", (d, w) -> {
                    repository.deleteHabit(habitId);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
