package dev.adam.client.mixin;

import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops the real player from attacking, mining or picking blocks while the camera is out. */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void adam$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.isActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void adam$blockMining(CallbackInfo ci) {
        if (FreecamModule.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void adam$blockPick(CallbackInfo ci) {
        if (FreecamModule.isActive()) {
            ci.cancel();
        }
    }
}
