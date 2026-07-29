package com.habittracker.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.data.sync.SyncWorker;
import com.habittracker.app.ui.checkin.CheckInBottomSheet;
import com.habittracker.app.ui.create.CreateHabitActivity;
import com.habittracker.app.ui.detail.HabitDetailActivity;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.DateUtils;
import com.habittracker.app.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Home screen fragment — the main dashboard showing:
 * - Greeting with time-of-day awareness
 * - Today's progress bar
 * - Scrollable list of habit cards with heatmap grids
 * - FAB to add new habits
 * - Delayed signup prompt (after 3 days without an account)
 */
public class HomeFragment extends Fragment implements HabitCardAdapter.HabitCardListener {

    private TextView greetingText;
    private TextView progressText;
    private ProgressBar dailyProgressBar;
    private RecyclerView habitsRecycler;
    private LinearLayout emptyState;
    private MaterialCardView signupPromptCard;
    private SwipeRefreshLayout swipeRefresh;

    private HabitRepository repository;
    private HabitCardAdapter adapter;
    private List<HabitStateEntity> currentStates = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init
        AppDatabase db = AppDatabase.getInstance(requireContext());
        repository = new HabitRepository(db);
        TokenManager tokenManager = new TokenManager(requireContext());

        // Bind views
        greetingText = view.findViewById(R.id.greeting_text);
        progressText = view.findViewById(R.id.progress_text);
        dailyProgressBar = view.findViewById(R.id.daily_progress_bar);
        habitsRecycler = view.findViewById(R.id.habits_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        signupPromptCard = view.findViewById(R.id.signup_prompt_card);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_habit);

        // Setup RecyclerView
        adapter = new HabitCardAdapter(repository, getViewLifecycleOwner());
        adapter.setListener(this);
        habitsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        habitsRecycler.setAdapter(adapter);

        // Greeting
        String name = tokenManager.getUserName();
        String greeting = DateUtils.getGreeting();
        if (name != null && !name.isEmpty()) {
            greetingText.setText(String.format("%s, %s", greeting, name));
        } else {
            greetingText.setText(greeting);
        }

        // Observe habits
        repository.getActiveHabits().observe(getViewLifecycleOwner(), habits -> {
            if (habits == null || habits.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                habitsRecycler.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
                dailyProgressBar.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                habitsRecycler.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                dailyProgressBar.setVisibility(View.VISIBLE);

                // Load states for all habits
                loadStatesAndUpdate(habits);
            }
        });

        // Observe today's completion count
        repository.getCompletedTodayCount().observe(getViewLifecycleOwner(), completedCount -> {
            updateProgressBar(completedCount != null ? completedCount : 0);
        });

        // FAB
        fab.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreateHabitActivity.class));
        });

        // Pull to refresh
        swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.accent_primary));
        swipeRefresh.setOnRefreshListener(() -> {
            SyncWorker.syncNow(requireContext());
            swipeRefresh.setRefreshing(false);
        });

        // Delayed signup prompt
        if (tokenManager.shouldPromptSignup()) {
            signupPromptCard.setVisibility(View.VISIBLE);
            view.findViewById(R.id.signup_prompt_button).setOnClickListener(v -> {
                startActivity(new Intent(requireContext(),
                        com.habittracker.app.ui.auth.SignUpActivity.class));
            });
        }
    }

    private void loadStatesAndUpdate(List<HabitEntity> habits) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            currentStates = db.habitStateDao().getAllSync();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setData(habits, currentStates);
                });
            }
        }).start();
    }

    private void updateProgressBar(int completedToday) {
        repository.getActiveHabitCount().observe(getViewLifecycleOwner(), totalCount -> {
            int total = totalCount != null ? totalCount : 0;
            if (total > 0) {
                int percent = (completedToday * 100) / total;
                dailyProgressBar.setProgress(percent);
                progressText.setText(getString(R.string.today_progress, completedToday, total));
            } else {
                dailyProgressBar.setProgress(0);
                progressText.setText("");
            }
        });
    }

    // --- HabitCardListener callbacks ---

    @Override
    public void onCardClicked(HabitEntity habit) {
        Intent intent = new Intent(requireContext(), HabitDetailActivity.class);
        intent.putExtra(Constants.EXTRA_HABIT_ID, habit.id);
        intent.putExtra(Constants.EXTRA_HABIT_NAME, habit.name);
        startActivity(intent);
    }

    @Override
    public void onActionClicked(HabitEntity habit, HabitStateEntity state) {
        CheckInBottomSheet sheet = CheckInBottomSheet.newInstance(
                habit.id, habit.name, habit.icon, habit.color,
                state != null ? state.difficultyLevel : 0.3,
                habit.difficultyTiny, habit.difficultyNormal, habit.difficultyStretch
        );
        sheet.show(getChildFragmentManager(), "checkin");
    }
}
