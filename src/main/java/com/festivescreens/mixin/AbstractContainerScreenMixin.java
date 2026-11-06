package com.festivescreens.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.festivescreens.client.PanelInfo;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Exposes the panel rectangle of every container screen (inventory, chests, furnaces, modded ones...). */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements PanelInfo {
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected int imageWidth;
    @Shadow
    protected int imageHeight;

    @Override
    public int festive$left() {
        return leftPos;
    }

    @Override
    public int festive$top() {
        return topPos;
    }

    @Override
    public int festive$width() {
        return imageWidth;
    }

    @Override
    public int festive$height() {
        return imageHeight;
    }
}
