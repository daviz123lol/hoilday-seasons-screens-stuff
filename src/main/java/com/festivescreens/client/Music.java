package com.festivescreens.client;

import com.festivescreens.core.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Plays the current theme's music loop while a container screen is open. Tracks are streamed from the mod's assets. */
public final class Music {
    private static SoundInstance current;
    private static Theme playing = Theme.NONE;

    private Music() {}

    /** Keep the right track going (restarts it when it ends, switches it when the theme changes). */
    static void tick(Theme theme, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (theme == Theme.NONE || volume <= 0f) {
            stop();
            return;
        }
        boolean running = current != null && mc.getSoundManager().isActive(current);
        if (running && playing == theme) return;
        stop();
        String id = switch (theme) {
            case CHRISTMAS -> "christmas";
            case HALLOWEEN -> "halloween";
            case NEW_YEARS -> "new_years";
            case VALENTINES -> "valentines";
            case ST_PATRICKS -> "st_patricks";
            case SPRING -> "spring";
            case SUMMER -> "summer";
            case THANKSGIVING -> "thanksgiving";
            default -> null;
        };
        if (id == null) return;
        SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("festivescreens", "music." + id));
        current = SimpleSoundInstance.forUI(ev, 1.0f, volume);
        playing = theme;
        mc.getSoundManager().play(current);
    }

    static boolean isPlaying() {
        return current != null && playing != Theme.NONE;
    }

    static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
        playing = Theme.NONE;
    }
}
