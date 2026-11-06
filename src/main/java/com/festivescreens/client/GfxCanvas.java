package com.festivescreens.client;

import com.festivescreens.core.Canvas;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adapts Minecraft's GUI drawing to the engine's tiny Canvas interface. */
final class GfxCanvas implements Canvas {
    private GuiGraphicsExtractor g;

    GfxCanvas with(GuiGraphicsExtractor g) {
        this.g = g;
        return this;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }
}
