package com.festivescreens;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Festive Screens: seasonal, animated container screens. Client-side only, so it is safe to add to or remove
 * from a modpack without touching the server.
 */
@Mod(value = FestiveScreens.MODID, dist = Dist.CLIENT)
public class FestiveScreens {
    public static final String MODID = "festivescreens";

    public FestiveScreens(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        // Mods screen > Festive Screens > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
