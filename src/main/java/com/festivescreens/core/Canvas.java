package com.festivescreens.core;

/** The only drawing primitive the effects engine needs. Colours are ARGB. */
public interface Canvas {
    void fill(int x1, int y1, int x2, int y2, int argb);
}
