package com.habittracker.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for user profile.
 * Stores XP, level, points (for reward system), and avatar config.
 * For the MVP, avatar_config is unused — it's a placeholder for post-MVP.
 */
@Entity(tableName = "profiles")
public class ProfileEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "xp")
    public int xp;

    @ColumnInfo(name = "level")
    public int level;

    @ColumnInfo(name = "points")
    public int points;

    @ColumnInfo(name = "avatar_config")
    public String avatarConfig; // JSON string, unused in MVP

    public ProfileEntity() {
        this.xp = 0;
        this.level = 1;
        this.points = 0;
        this.avatarConfig = "{}";
    }
}
