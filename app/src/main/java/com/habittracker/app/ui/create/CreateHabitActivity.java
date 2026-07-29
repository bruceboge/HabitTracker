package com.habittracker.app.ui.create;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.domain.model.Habit;
import com.habittracker.app.util.Constants;

import java.util.Arrays;

/**
 * Create/Edit habit screen.
 * Lets users set name, icon, color, cadence, and three difficulty labels.
 */
public class CreateHabitActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText diffTinyInput;
    private TextInputEditText diffNormalInput;
    private TextInputEditText diffStretchInput;
    private String selectedIcon = "⭐";
    private String selectedColor = Constants.HABIT_COLORS[0];
    private String selectedCadence = "daily";
    private HabitRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);

        repository = new HabitRepository(AppDatabase.getInstance(this));

        // Bind views
        nameInput = findViewById(R.id.name_input);
        diffTinyInput = findViewById(R.id.difficulty_tiny_input);
        diffNormalInput = findViewById(R.id.difficulty_normal_input);
        diffStretchInput = findViewById(R.id.difficulty_stretch_input);
        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        btnBack.setOnClickListener(v -> finish());

        // Setup icon grid
        setupIconGrid();

        // Setup color palette
        setupColorPalette();

        // Setup cadence chips
        setupCadenceChips();

        // Save
        btnSave.setOnClickListener(v -> saveHabit());
    }

    private void setupIconGrid() {
        RecyclerView iconGrid = findViewById(R.id.icon_grid);
        iconGrid.setLayoutManager(new GridLayoutManager(this, 8));

        // Simple adapter for emoji icons
        iconGrid.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private int selectedPosition = 14; // Default: ⭐ (index 14)

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                TextView tv = new TextView(CreateHabitActivity.this);
                tv.setTextSize(24);
                tv.setGravity(Gravity.CENTER);
                int size = (int) (48 * getResources().getDisplayMetrics().density);
                tv.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT, size));
                return new RecyclerView.ViewHolder(tv) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = (TextView) holder.itemView;
                tv.setText(Constants.HABIT_ICONS[position]);
                tv.setAlpha(position == selectedPosition ? 1.0f : 0.5f);
                tv.setOnClickListener(v -> {
                    int prev = selectedPosition;
                    selectedPosition = holder.getAdapterPosition();
                    selectedIcon = Constants.HABIT_ICONS[selectedPosition];
                    notifyItemChanged(prev);
                    notifyItemChanged(selectedPosition);
                });
            }

            @Override
            public int getItemCount() { return Constants.HABIT_ICONS.length; }
        });
    }

    private void setupColorPalette() {
        LinearLayout palette = findViewById(R.id.color_palette);
        float density = getResources().getDisplayMetrics().density;
        int circleSize = (int) (36 * density);
        int margin = (int) (6 * density);

        final View[] colorViews = new View[Constants.HABIT_COLORS.length];

        for (int i = 0; i < Constants.HABIT_COLORS.length; i++) {
            View circle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(circleSize, circleSize);
            params.setMargins(margin, 0, margin, 0);
            circle.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor(Constants.HABIT_COLORS[i]));
            if (i == 0) bg.setStroke((int) (3 * density), Color.WHITE); // Default selected
            circle.setBackground(bg);

            final int index = i;
            circle.setOnClickListener(v -> {
                selectedColor = Constants.HABIT_COLORS[index];
                // Update selection visuals
                for (int j = 0; j < colorViews.length; j++) {
                    GradientDrawable d = new GradientDrawable();
                    d.setShape(GradientDrawable.OVAL);
                    d.setColor(Color.parseColor(Constants.HABIT_COLORS[j]));
                    if (j == index) d.setStroke((int) (3 * density), Color.WHITE);
                    colorViews[j].setBackground(d);
                }
            });

            colorViews[i] = circle;
            palette.addView(circle);
        }
    }

    private void setupCadenceChips() {
        Chip chipDaily = findViewById(R.id.chip_daily);
        Chip chipWeekdays = findViewById(R.id.chip_weekdays);
        Chip chipCustom = findViewById(R.id.chip_custom);

        chipDaily.setChecked(true);

        chipDaily.setOnCheckedChangeListener((v, checked) -> {
            if (checked) selectedCadence = "daily";
        });
        chipWeekdays.setOnCheckedChangeListener((v, checked) -> {
            if (checked) selectedCadence = "weekdays";
        });
        chipCustom.setOnCheckedChangeListener((v, checked) -> {
            if (checked) selectedCadence = "custom";
        });
    }

    private void saveHabit() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        if (name.isEmpty()) {
            nameInput.setError("Please enter a name");
            return;
        }

        Habit habit = new Habit(name, selectedIcon, selectedColor);
        habit.setCadence(selectedCadence);

        // Set difficulty labels
        String tiny = diffTinyInput.getText() != null ? diffTinyInput.getText().toString().trim() : "";
        String normal = diffNormalInput.getText() != null ? diffNormalInput.getText().toString().trim() : "";
        String stretch = diffStretchInput.getText() != null ? diffStretchInput.getText().toString().trim() : "";

        if (!tiny.isEmpty()) habit.setDifficultyTiny(tiny);
        if (!normal.isEmpty()) habit.setDifficultyNormal(normal);
        if (!stretch.isEmpty()) habit.setDifficultyStretch(stretch);

        // Set cadence days
        switch (selectedCadence) {
            case "daily":
                habit.setCadenceDays(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
                break;
            case "weekdays":
                habit.setCadenceDays(Arrays.asList(1, 2, 3, 4, 5));
                break;
            default:
                habit.setCadenceDays(Arrays.asList(1, 2, 3, 4, 5, 6, 7)); // Default to daily for now
                break;
        }

        repository.createHabit(habit, () -> runOnUiThread(() -> {
            Toast.makeText(this, "Habit created! 🎯", Toast.LENGTH_SHORT).show();
            finish();
        }));
    }
}
