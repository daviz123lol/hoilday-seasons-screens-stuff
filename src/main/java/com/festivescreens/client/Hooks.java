package com.festivescreens.client;

import org.slf4j.Logger;

import com.festivescreens.Settings;
import com.festivescreens.core.Engine;
import com.festivescreens.core.Theme;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.world.inventory.AbstractFurnaceMenu;

/**
 * The only place that talks to both Minecraft and the engine. Every entry point is wrapped so that a bug here can
 * never crash the game: on any error the mod switches itself off for the rest of the session and logs why.
 */
public final class Hooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Engine ENGINE = new Engine(System.nanoTime());
    private static final GfxCanvas CANVAS = new GfxCanvas();

    private static long lastNanos;
    private static Screen lastContainer;
    private static int pl, pt, pw, ph;
    private static boolean lastWasChest;
    private static boolean failed;
    private static boolean loggedTheme;
    private static boolean warnedNoPanel;

    private Hooks() {}

    /** Runs once per frame (first hook to fire). Returns false when there is nothing to draw. */
    private static boolean begin(GuiGraphicsExtractor g, boolean screenOpen, boolean container) {
        Theme t = Settings.currentTheme();
        ENGINE.setTheme(t);
        if (t == Theme.NONE) {
            lastNanos = 0L;
            lastContainer = null;
            return false;
        }
        if (!loggedTheme) {
            loggedTheme = true;
            LOGGER.info("Festive Screens: active theme is {}", t.label);
        }
        Settings.apply(ENGINE.opt);
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        ENGINE.setAmbientActive(screenOpen && (container || ENGINE.opt.otherScreens));
        ENGINE.update(dt, g.guiWidth(), g.guiHeight());
        return true;
    }

    /** Start of Screen.extractRenderStateWithTooltipAndSubtitles: draws behind the panel. */
    public static void backdrop(Screen s, GuiGraphicsExtractor g) {
        if (failed) return;
        try {
            boolean container = s instanceof PanelInfo;
            if (!container && s instanceof AbstractContainerScreen<?> && !warnedNoPanel) {
                warnedNoPanel = true;
                LOGGER.warn("Festive Screens: container screens were not hooked (mixin not applied); "
                        + "only ambient particles will show.");
            }
            if (!begin(g, true, container)) return;
            if (s instanceof PanelInfo p) {
                pl = p.festive$left();
                pt = p.festive$top();
                pw = p.festive$width();
                ph = p.festive$height();
                if (s != lastContainer) {
                    lastContainer = s;
                    // ContainerScreen = chests, ender chests, barrels
                    lastWasChest = s instanceof ContainerScreen || s instanceof ShulkerBoxScreen;
                    ENGINE.burst(true, pl, pt, pw, ph, lastWasChest);
                }
                ENGINE.renderBackdrop(CANVAS.with(g), pl, pt, pw, ph);
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** Just before tooltips are drawn: decorations, furnace sparks and particles, over the slots. */
    public static void foreground(Screen s, GuiGraphicsExtractor g) {
        if (failed || ENGINE.theme() == Theme.NONE) return;
        try {
            CANVAS.with(g);
            if (s instanceof PanelInfo p) {
                ENGINE.renderDecor(CANVAS, p.festive$left(), p.festive$top(), p.festive$width(), p.festive$height());
                if (s instanceof AbstractContainerScreen<?> acs && acs.getMenu() instanceof AbstractFurnaceMenu fm) {
                    ENGINE.furnace(p.festive$left(), p.festive$top(), fm.isLit());
                }
            }
            ENGINE.renderParticles(CANVAS);
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** End of Gui.extractRenderState: keeps particles flying after a screen closes, and fires the close animation. */
    public static void hud(GuiGraphicsExtractor g) {
        if (failed) return;
        try {
            if (Minecraft.getInstance().screen != null) return;
            if (!begin(g, false, false)) return;
            if (lastContainer != null) {
                lastContainer = null;
                ENGINE.burst(false, pl, pt, pw, ph, lastWasChest);
            }
            ENGINE.renderParticles(CANVAS.with(g));
        } catch (Throwable e) {
            fail(e);
        }
    }

    private static void fail(Throwable e) {
        failed = true;
        LOGGER.error("Festive Screens hit an error and has switched itself off for this session", e);
    }
}
