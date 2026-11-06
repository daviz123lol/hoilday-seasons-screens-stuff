package com.festivescreens.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.festivescreens.client.Hooks;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void festive$hud(GuiGraphicsExtractor g, DeltaTracker deltaTracker, CallbackInfo ci) {
        Hooks.hud(g);
    }
}
