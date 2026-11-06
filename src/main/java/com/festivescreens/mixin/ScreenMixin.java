package com.festivescreens.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.festivescreens.client.Hooks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void festive$backdrop(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick,
                                  CallbackInfo ci) {
        Hooks.backdrop((Screen) (Object) this, g);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void festive$foreground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick,
                                    CallbackInfo ci) {
        Hooks.foreground((Screen) (Object) this, g);
    }
}
