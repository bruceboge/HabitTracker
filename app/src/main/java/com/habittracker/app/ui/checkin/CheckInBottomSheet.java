package com.habittracker.app.ui.checkin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.data.sync.SyncWorker;
import com.habittracker.app.domain.model.DailyLog;
import com.habittracker.app.domain.model.Habit;

/**
 * Bottom sheet for quick habit check-in.
 * Shows habit info, effort tags (Hard/OK/Easy), and complete/skip buttons.
 * After completion, shows reward feedback before dismissing.
 */
public class CheckInBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HABIT_ID = "habit_id";
    private static final String ARG_HABIT_NAME = "habit_name";
    private static final String ARG_HABIT_ICON = "habit_icon";
    private static final String ARG_HABIT_COLOR = "habit_color";
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String ARG_DIFF_TINY = "diff_tiny";
    private static final String ARG_DIFF_NORMAL = "diff_normal";
    private static final String ARG_DIFF_STRETCH = "diff_stretch";

    private int selectedEffort = DailyLog.EFFORT_OK; // Default to OK

    public static CheckInBottomSheet newInstance(String habitId, String name, String icon,
                                                  String color, double difficulty,
                                                  String diffTiny, String diffNormal,
                                                  String diffStretch) {
        CheckInBottomSheet sheet = new CheckInBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_HABIT_ID, habitId);
        args.putString(ARG_HABIT_NAME, name);
        args.putString(ARG_HABIT_ICON, icon);
        args.putString(ARG_HABIT_COLOR, color);
        args.putDouble(ARG_DIFFICULTY, difficulty);
        args.putString(ARG_DIFF_TINY, diffTiny);
        args.putString(ARG_DIFF_NORMAL, diffNormal);
        args.putString(ARG_DIFF_STRETCH, diffStretch);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }

        String habitId = args.getString(ARG_HABIT_ID);
        String name = args.getString(ARG_HABIT_NAME);
        String icon = args.getString(ARG_HABIT_ICON);
        double difficulty = args.getDouble(ARG_DIFFICULTY, 0.3);

        // Set header info
        ((TextView) view.findViewById(R.id.checkin_icon)).setText(icon);
        ((TextView) view.findViewById(R.id.checkin_habit_name)).setText(name);

        // Set today's target
        Habit tempHabit = new Habit();
        tempHabit.setDifficultyTiny(args.getString(ARG_DIFF_TINY));
        tempHabit.setDifficultyNormal(args.getString(ARG_DIFF_NORMAL));
        tempHabit.setDifficultyStretch(args.getString(ARG_DIFF_STRETCH));

        String target = tempHabit.getTaskDescription(difficulty);
        ((TextView) view.findViewById(R.id.checkin_target))
                .setText(getString(R.string.todays_target, target));
        ((TextView) view.findViewById(R.id.checkin_difficulty_label))
                .setText(tempHabit.getDifficultyLabel(difficulty));

        // Effort tags
        TextView effortHard = view.findViewById(R.id.effort_hard);
        TextView effortOk = view.findViewById(R.id.effort_ok);
        TextView effortEasy = view.findViewById(R.id.effort_easy);

        // Default selection: OK
        effortOk.setSelected(true);

        View.OnClickListener effortClickListener = v -> {
            effortHard.setSelected(false);
            effortOk.setSelected(false);
            effortEasy.setSelected(false);
            v.setSelected(true);

            if (v.getId() == R.id.effort_hard) selectedEffort = DailyLog.EFFORT_HARD;
            else if (v.getId() == R.id.effort_ok) selectedEffort = DailyLog.EFFORT_OK;
            else if (v.getId() == R.id.effort_easy) selectedEffort = DailyLog.EFFORT_EASY;
        };

        effortHard.setOnClickListener(effortClickListener);
        effortOk.setOnClickListener(effortClickListener);
        effortEasy.setOnClickListener(effortClickListener);

        // Complete button
        MaterialButton btnComplete = view.findViewById(R.id.btn_complete);
        MaterialButton btnSkip = view.findViewById(R.id.btn_skip);
        LinearLayout feedbackContainer = view.findViewById(R.id.feedback_container);
        TextView feedbackText = view.findViewById(R.id.feedback_text);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        HabitRepository repo = new HabitRepository(db);

        btnComplete.setOnClickListener(v -> {
            btnComplete.setEnabled(false);
            btnSkip.setEnabled(false);

            repo.logCompletion(habitId, true, selectedEffort,
                    new HabitRepository.CompletionCallback() {
                        @Override
                        public void onComplete(int pointsEarned, boolean isRestartWin,
                                             boolean difficultyIncreased, int streak) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                // Show feedback
                                StringBuilder feedback = new StringBuilder();
                                if (isRestartWin) {
                                    feedback.append(getString(R.string.restart_win, pointsEarned));
                                } else {
                                    feedback.append(getString(R.string.great_job, pointsEarned));
                                }
                                if (difficultyIncreased) {
                                    feedback.append("\n").append(getString(R.string.difficulty_up));
                                }

                                feedbackText.setText(feedback.toString());
                                feedbackContainer.setVisibility(View.VISIBLE);
                                btnComplete.setVisibility(View.GONE);
                                btnSkip.setVisibility(View.GONE);

                                // Trigger sync
                                SyncWorker.syncNow(requireContext());

                                // Auto-dismiss after delay
                                new Handler(Looper.getMainLooper())
                                        .postDelayed(() -> dismiss(), 2000);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                btnComplete.setEnabled(true);
                                btnSkip.setEnabled(true);
                            });
                        }
                    });
        });

        // Skip button (mark as missed, no penalty)
        btnSkip.setOnClickListener(v -> {
            repo.logCompletion(habitId, false, 0, new HabitRepository.CompletionCallback() {
                @Override
                public void onComplete(int pointsEarned, boolean isRestartWin,
                                     boolean difficultyIncreased, int streak) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> dismiss());
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> dismiss());
                    }
                }
            });
        });
    }
}
