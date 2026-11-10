package com.festivescreens.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.festivescreens.client.Hooks;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tints container panels. Every textured blit funnels through innerBlit, whose last int is a colour that the texture
 * is multiplied by (white = unchanged), so we swap it for the theme colour while a container screen is drawing.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @ModifyVariable(method = {
            "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
            "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V"},
            at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int festive$tint(int colour) {
        return Hooks.tint(colour);
    }
}
