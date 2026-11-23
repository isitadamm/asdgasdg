package dev.adam.client.mixin;

import dev.adam.client.module.cam.AdamCamera;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla mouse-look turns the real player. While the camera is detached we
 * send that turning to the camera instead, so the real player stays put.
 */
@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void adam$turnCameraInstead(double yRot, double xRot, CallbackInfo ci) {
        if (!FreecamModule.isActive()) {
            return;
        }
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        AdamCamera camera = FreecamModule.getCamera();
        if (camera != null) {
            camera.turn(yRot, xRot);
            ci.cancel();
        }
    }
}
