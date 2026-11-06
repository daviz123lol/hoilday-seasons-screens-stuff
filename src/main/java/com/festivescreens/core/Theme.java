package com.festivescreens.core;

import java.time.LocalDate;

/** Every holiday / season theme, plus NONE (vanilla look). */
public enum Theme {
    NONE("None", 0x000000, 0x000000),
    HALLOWEEN("Halloween", 0xFF7A00, 0x8E24AA),
    CHRISTMAS("Christmas", 0xE53935, 0x43A047),
    NEW_YEARS("New Year's", 0xFFD54F, 0x90CAF9),
    VALENTINES("Valentine's Day", 0xFF4F7B, 0xC2185B),
    ST_PATRICKS("St. Patrick's Day", 0x2E9E3E, 0xFFC107),
    SPRING("Spring", 0x66BB6A, 0xF48FB1),
    SUMMER("Summer", 0x29B6F6, 0xFFD54F),
    THANKSGIVING("Thanksgiving", 0xE65100, 0xFFB300);

    public final String label;
    /** The two colours the border glow blends between (RGB). */
    public final int glowA;
    public final int glowB;

    Theme(String label, int glowA, int glowB) {
        this.label = label;
        this.glowA = glowA;
        this.glowB = glowB;
    }

    /**
     * Which theme is "in season" on a date. Gaps (most of January after the 6th, late February, September)
     * return NONE, which leaves the game looking vanilla.
     */
    public static Theme forDate(LocalDate d) {
        int k = d.getMonthValue() * 100 + d.getDayOfMonth();
        if (k >= 1201 && k <= 1225) return CHRISTMAS;
        if (k >= 1226 || k <= 106) return NEW_YEARS;
        if (k >= 201 && k <= 215) return VALENTINES;
        if (k >= 301 && k <= 318) return ST_PATRICKS;
        if (k >= 319 && k <= 614) return SPRING;
        if (k >= 615 && k <= 831) return SUMMER;
        if (k >= 1001 && k <= 1101) return HALLOWEEN;
        if (k >= 1102 && k <= 1130) return THANKSGIVING;
        return NONE;
    }
}
