package com.festivescreens.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.festivescreens.client.Hooks;

/** Keeps vanilla background / menu music from starting while one of our theme tracks is playing. */
@Mixin(targets = "net.minecraft.client.sounds.MusicManager")
public abstract class MusicManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void festive$quiet(CallbackInfo ci) {
        if (Hooks.musicPlaying()) ci.cancel();
    }
}
