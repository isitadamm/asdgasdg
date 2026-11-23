package dev.adam.client.mixin;

import dev.adam.client.module.sword.HitboxModule;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hitbox Expand: grows the area the crosshair counts as "on" an entity. */
@Mixin(Entity.class)
public class EntityPickMixin {

    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void adam$expandHitbox(CallbackInfoReturnable<Float> cir) {
        float extra = HitboxModule.expansion((Entity) (Object) this);
        if (extra > 0.0F) {
            cir.setReturnValue(extra);
        }
    }
}
