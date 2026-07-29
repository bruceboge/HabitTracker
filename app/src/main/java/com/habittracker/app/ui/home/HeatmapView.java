package com.habittracker.app.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.DateUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom View that renders a Streaks-style contribution heatmap grid.
 * 
 * Grid: 13 columns × 7 rows = 91 cells (last ~3 months)
 * Each cell = one day. Color intensity shows completion status.
 * 
 * Design decisions:
 * - Completed cells: habit's base color at full opacity
 * - Missed cells: habit's base color at 12% opacity (soft fade, not a stark hole)
 * - Pre-creation cells: transparent (invisible)
 * - "Recovery glow" on completions after miss streaks
 */
public class HeatmapView extends View {

    private static final int COLUMNS = Constants.HEATMAP_COLUMNS; // 13
    private static final int ROWS = Constants.HEATMAP_ROWS;       // 7
    private static final float CELL_CORNER_RADIUS = 3f;
    private static final float CELL_GAP = 2f;

    private Paint cellPaint;
    private Paint glowPaint;
    private RectF cellRect;

    private String habitColor = "#4CAF50"; // Default green
    private LocalDate habitCreatedAt;
    private Map<String, Boolean> completionMap = new HashMap<>(); // date → completed
    private LocalDate gridStartDate;

    // Parsed color values (cached)
    private int baseColor;
    private int completedColor;
    private int missedColor;

    public HeatmapView(Context context) {
        super(context);
        init();
    }

    public HeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeatmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellRect = new RectF();
        gridStartDate = DateUtils.getHeatmapStartDate();
        updateColors();
    }

    /**
     * Set the habit's data for rendering.
     * Call this whenever the habit's logs or color changes.
     */
    public void setData(List<DailyLogEntity> logs, String color, LocalDate createdAt) {
        this.habitColor = color;
        this.habitCreatedAt = createdAt;
        updateColors();

        // Build lookup map: date string → completed
        completionMap.clear();
        if (logs != null) {
            for (DailyLogEntity log : logs) {
                completionMap.put(log.logDate, log.completed);
            }
        }

        invalidate(); // Trigger redraw
    }

    private void updateColors() {
        try {
            baseColor = Color.parseColor(habitColor);
        } catch (Exception e) {
            baseColor = Color.parseColor("#4CAF50"); // Fallback
        }

        // Completed: full opacity
        completedColor = baseColor;

        // Missed: 12% opacity (soft fade, not a stark hole)
        missedColor = Color.argb(
                (int) (255 * 0.12),
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float cellSize = (width - (COLUMNS - 1) * CELL_GAP * getResources().getDisplayMetrics().density)
                / COLUMNS;
        float density = getResources().getDisplayMetrics().density;
        float gap = CELL_GAP * density;
        float actualCellSize = (width - (COLUMNS - 1) * gap) / COLUMNS;
        int height = (int) (actualCellSize * ROWS + gap * (ROWS - 1));
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float gap = CELL_GAP * density;
        float cornerRadius = CELL_CORNER_RADIUS * density;

        float cellSize = (getWidth() - (COLUMNS - 1) * gap) / COLUMNS;

        LocalDate today = LocalDate.now();

        for (int col = 0; col < COLUMNS; col++) {
            for (int row = 0; row < ROWS; row++) {
                // Calculate which date this cell represents
                int dayIndex = col * ROWS + row;
                LocalDate cellDate = gridStartDate.plusDays(dayIndex);

                // Skip future dates
                if (cellDate.isAfter(today)) continue;

                // Skip dates before habit creation
                if (habitCreatedAt != null && cellDate.isBefore(habitCreatedAt)) continue;

                // Determine cell color
                String dateKey = DateUtils.formatIso(cellDate);
                Boolean completed = completionMap.get(dateKey);

                if (completed != null && completed) {
                    cellPaint.setColor(completedColor);
                } else {
                    cellPaint.setColor(missedColor);
                }

                // Calculate cell position
                float left = col * (cellSize + gap);
                float top = row * (cellSize + gap);
                cellRect.set(left, top, left + cellSize, top + cellSize);

                // Draw the cell
                canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellPaint);
            }
        }
    }
}
