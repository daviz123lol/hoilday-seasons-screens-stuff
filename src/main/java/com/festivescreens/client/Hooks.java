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
    private static boolean tintOn;
    private static Screen lastMenu;

    private Hooks() {}

    /** Runs once per frame (first hook to fire). Returns false when there is nothing to draw. */
    private static boolean begin(GuiGraphicsExtractor g, boolean screenOpen, boolean container, boolean menu) {
        Theme t = Settings.currentTheme();
        ENGINE.setTheme(t);
        if (t == Theme.NONE) {
            Music.stop();
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
        ENGINE.setAmbientActive(screenOpen && (container || (menu && ENGINE.opt.otherScreens)));
        ENGINE.update(dt, g.guiWidth(), g.guiHeight());
        return true;
    }

    /** Start of Screen.extractRenderStateWithTooltipAndSubtitles: draws behind the panel. */
    public static void backdrop(Screen s, GuiGraphicsExtractor g) {
        tintOn = false;
        if (failed) return;
        try {
            boolean container = s instanceof PanelInfo;
            if (!container && s instanceof AbstractContainerScreen<?> && !warnedNoPanel) {
                warnedNoPanel = true;
                LOGGER.warn("Festive Screens: container screens were not hooked (mixin not applied); "
                        + "only ambient particles will show.");
            }
            boolean menu = !container && isMenu(s);
            if (!begin(g, true, container, menu)) return;
            if (container || (menu && Settings.menuMusic())) Music.tick(ENGINE.theme(), Settings.musicVolume());
            else Music.stop();
            if (menu && s != lastMenu) {
                lastMenu = s;
                if (s.getClass().getName().contains("TitleScreen")) ENGINE.burst(true, 0, 0, 0, 0, false);
            }
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
                tintOn = ENGINE.opt.panelTint;
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** Just before tooltips are drawn: decorations, furnace sparks and particles, over the slots. */
    public static void foreground(Screen s, GuiGraphicsExtractor g) {
        tintOn = false;
        if (failed || ENGINE.theme() == Theme.NONE) return;
        try {
            CANVAS.with(g);
            if (!(s instanceof PanelInfo) && ENGINE.opt.otherScreens && isMenu(s)) {
                ENGINE.renderScreen(CANVAS, g.guiWidth(), g.guiHeight());
            }
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
            tintOn = false;
            if (Minecraft.getInstance().screen != null) return;
            lastMenu = null;
            Music.stop();
            if (!begin(g, false, false, false)) return;
            if (lastContainer != null) {
                lastContainer = null;
                Music.stop();
                ENGINE.burst(false, pl, pt, pw, ph, lastWasChest);
            }
            ENGINE.renderParticles(CANVAS.with(g));
        } catch (Throwable e) {
            fail(e);
        }
    }

    /**
     * Called for every plain GUI texture blit (colour = what the texture is multiplied by, white = unchanged).
     * Only active between the start of a container screen's rendering and its tooltips, so the panel, slots and
     * buttons take the theme colour while tooltips and the HUD stay normal.
     */
    public static int tint(int colour) {
        if (!tintOn || colour != -1) return colour;
        try {
            return ENGINE.panelTint();
        } catch (Throwable e) {
            tintOn = false;
            fail(e);
            return colour;
        }
    }

    private static final String[] MENU_NAMES = {"TitleScreen", "PauseScreen", "OptionsScreen", "OptionsSubScreen",
            "SelectWorld", "WorldSelection", "JoinMultiplayer", "CreateWorld", "EditWorld", "ControlsScreen",
            "KeyBindsScreen", "LanguageSelect", "PackSelection", "SoundOptions", "SkinCustomization", "Accessibility",
            "VideoSettings", "ShareToLan", "Realms", "Credits", "Telemetry", "ModListScreen", "ModsScreen"};

    /** Whole-screen menus that get the themed dressing (title, pause, options, world list...). */
    private static boolean isMenu(Screen s) {
        String n = s.getClass().getName();
        for (String m : MENU_NAMES) if (n.contains(m)) return true;
        return false;
    }

    /** True while one of our tracks is playing, so vanilla music can stay quiet. */
    public static boolean musicPlaying() {
        return Music.isPlaying();
    }

    private static void fail(Throwable e) {
        try {
            Music.stop();
        } catch (Throwable ignored) {
            // nothing more we can do
        }
        failed = true;
        LOGGER.error("Festive Screens hit an error and has switched itself off for this session", e);
    }
}
