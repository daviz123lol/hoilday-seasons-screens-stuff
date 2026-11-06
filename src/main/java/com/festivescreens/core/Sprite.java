package com.festivescreens.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny pixel-art sprite defined by text rows, drawn as horizontal runs of filled rectangles.
 * No texture files are needed, so there is nothing to ship, reload or recolour.
 */
public final class Sprite {
    public final int w;
    public final int h;
    private final int[] runRow;
    private final int[] runCol;
    private final int[] runLen;
    private final int[] runColor;

    private Sprite(int w, int h, List<int[]> runs) {
        this.w = w;
        this.h = h;
        int n = runs.size();
        runRow = new int[n];
        runCol = new int[n];
        runLen = new int[n];
        runColor = new int[n];
        for (int i = 0; i < n; i++) {
            int[] r = runs.get(i);
            runRow[i] = r[0];
            runCol[i] = r[1];
            runLen[i] = r[2];
            runColor[i] = r[3];
        }
    }

    /**
     * @param legend comma separated {@code c=RRGGBB} or {@code c=AARRGGBB} pairs, one per character used
     * @param rows   equal-length strings; '.' and ' ' are transparent
     */
    public static Sprite of(String legend, String... rows) {
        Map<Character, Integer> colours = new HashMap<>();
        for (String part : legend.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (p.length() < 3 || p.charAt(1) != '=') {
                throw new IllegalArgumentException("bad legend entry: " + p);
            }
            String hex = p.substring(2);
            int v = (int) Long.parseLong(hex, 16);
            if (hex.length() <= 6) v |= 0xFF000000;
            colours.put(p.charAt(0), v);
        }
        if (rows.length == 0) throw new IllegalArgumentException("no rows");
        int w = rows[0].length();
        List<int[]> runs = new ArrayList<>();
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            if (row.length() != w) {
                throw new IllegalArgumentException(
                        "row " + y + " has width " + row.length() + " but expected " + w + ": '" + row + "'");
            }
            int x = 0;
            while (x < w) {
                char ch = row.charAt(x);
                if (ch == '.' || ch == ' ') {
                    x++;
                    continue;
                }
                Integer col = colours.get(ch);
                if (col == null) throw new IllegalArgumentException("no colour for '" + ch + "' in row " + y);
                int start = x;
                while (x < w && row.charAt(x) == ch) x++;
                runs.add(new int[] {y, start, x - start, col});
            }
        }
        return new Sprite(w, rows.length, runs);
    }

    public void draw(Canvas c, int x, int y, int scale, float alpha, boolean flipX, boolean flipY) {
        for (int i = 0; i < runRow.length; i++) {
            int argb = withAlpha(runColor[i], alpha);
            if ((argb >>> 24) == 0) continue;
            int col = flipX ? w - (runCol[i] + runLen[i]) : runCol[i];
            int row = flipY ? h - 1 - runRow[i] : runRow[i];
            int x1 = x + col * scale;
            int y1 = y + row * scale;
            c.fill(x1, y1, x1 + runLen[i] * scale, y1 + scale, argb);
        }
    }

    public int runCount() {
        return runRow.length;
    }

    /** Multiplies the colour's own alpha by {@code alpha} (0..1). */
    public static int withAlpha(int argb, float alpha) {
        if (alpha >= 1f) return argb;
        if (alpha < 0f) alpha = 0f;
        int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
