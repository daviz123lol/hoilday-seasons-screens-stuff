package com.festivescreens;

import java.time.LocalDate;

import com.festivescreens.core.Engine;
import com.festivescreens.core.Theme;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config (config/festivescreens-client.toml, or Mods > Festive Screens > Config in game). */
public final class Settings {
    public enum Mode {
        AUTO, OFF, HALLOWEEN, CHRISTMAS, NEW_YEARS, VALENTINES, ST_PATRICKS, SPRING, SUMMER, THANKSGIVING
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<Mode> MODE = B
            .comment("AUTO picks the theme from today's date. Pick a holiday to force it, or OFF for the vanilla look.")
            .defineEnum("mode", Mode.AUTO);
    public static final ModConfigSpec.BooleanValue BURSTS = B
            .comment("Open/close animations: bats, hearts, fireworks, Santa's sleigh and so on.")
            .define("openCloseAnimations", true);
    public static final ModConfigSpec.BooleanValue AMBIENT = B
            .comment("Slow ambient particles while a screen is open (snow, petals, leaves, bubbles...).")
            .define("ambientParticles", true);
    public static final ModConfigSpec.BooleanValue DECORATIONS = B
            .comment("Glowing border, corner decorations, Christmas lights, rainbow and waves.")
            .define("decorations", true);
    public static final ModConfigSpec.BooleanValue PANEL_TINT = B
            .comment("Re-colour the container panel (inventory, chests, furnaces...) in the theme's colours.")
            .define("panelTint", true);
    public static final ModConfigSpec.BooleanValue MUSIC = B
            .comment("Play the theme's music while a chest, furnace, inventory... is open.")
            .define("music", true);
    public static final ModConfigSpec.IntValue MUSIC_VOLUME = B
            .comment("Music volume (0-100), on top of your master volume.")
            .defineInRange("musicVolume", 40, 0, 100);
    public static final ModConfigSpec.BooleanValue FURNACE_FX = B
            .comment("Themed sparks over the flame of lit furnaces, blast furnaces and smokers.")
            .define("furnaceEffects", true);
    public static final ModConfigSpec.BooleanValue MENU_SCREENS = B
            .comment("Also dress up the title screen, pause menu, options and world list (decorations + particles).")
            .define("menuScreens", true);
    public static final ModConfigSpec.BooleanValue MENU_MUSIC = B
            .comment("Also play the theme music on the title screen and menus.")
            .define("menuMusic", true);
    public static final ModConfigSpec.BooleanValue REDUCE_MOTION = B
            .comment("Static decorations only: no particles, no flybys, no pulsing or blinking.")
            .define("reduceMotion", false);
    public static final ModConfigSpec.IntValue INTENSITY = B
            .comment("How many particles, in percent. Lower this if you want it calmer or need more FPS.")
            .defineInRange("intensity", 100, 25, 200);

    public static final ModConfigSpec SPEC = B.build();

    private static long cachedSecond = -1;
    private static Theme cachedAuto = Theme.NONE;

    private Settings() {}

    /** The theme to show right now. Safe to call every frame. */
    public static Theme currentTheme() {
        Mode m;
        try {
            m = MODE.get();
        } catch (IllegalStateException notLoaded) {
            m = Mode.AUTO;
        }
        switch (m) {
            case OFF:
                return Theme.NONE;
            case AUTO: {
                long sec = System.currentTimeMillis() / 1000L;
                if (sec != cachedSecond) {
                    cachedSecond = sec;
                    cachedAuto = Theme.forDate(LocalDate.now());
                }
                return cachedAuto;
            }
            default:
                return Theme.valueOf(m.name());
        }
    }

    public static boolean menuMusic() {
        try {
            return MENU_MUSIC.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /** 0 when music is off. */
    public static float musicVolume() {
        try {
            return MUSIC.get() ? MUSIC_VOLUME.get() / 100f : 0f;
        } catch (IllegalStateException notLoaded) {
            return 0f;
        }
    }

    public static void apply(Engine.Options o) {
        try {
            o.bursts = BURSTS.get();
            o.ambient = AMBIENT.get();
            o.decorations = DECORATIONS.get();
            o.panelTint = PANEL_TINT.get();
            o.furnaceFx = FURNACE_FX.get();
            o.otherScreens = MENU_SCREENS.get();
            o.reduceMotion = REDUCE_MOTION.get();
            o.intensity = INTENSITY.get() / 100f;
        } catch (IllegalStateException notLoaded) {
            // config not loaded yet: keep defaults
        }
    }
}
