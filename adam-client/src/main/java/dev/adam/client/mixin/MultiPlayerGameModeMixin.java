package dev.adam.client.mixin;

import dev.adam.client.module.cam.FreecamModule;
import dev.adam.client.module.sword.AutoWeaponModule;
import dev.adam.client.module.sword.CriticalsModule;
import dev.adam.client.module.sword.FriendsModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops block/entity interaction from a far-away camera position. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void adam$blockUseOnBlock(CallbackInfoReturnable<InteractionResult> cir) {
        if (FreecamModule.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void adam$blockInteractEntity(CallbackInfoReturnable<InteractionResult> cir) {
        if (FreecamModule.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void adam$blockInteractEntityAt(CallbackInfoReturnable<InteractionResult> cir) {
        if (FreecamModule.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void adam$blockAttackEntity(CallbackInfo ci) {
        if (FreecamModule.isActive()) {
            ci.cancel();
        }
    }

    /**
     * Every attack on an entity (yours or Kill Aura's) passes through here:
     * Friend Protection can stop it, then Auto Weapon and Criticals get ready for it.
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void adam$beforeAttack(Player player, Entity target, CallbackInfo ci) {
        if (FriendsModule.isFriend(target)) {
            ci.cancel();
            return;
        }
        AutoWeaponModule.prepare(player, target);
        CriticalsModule.prepare(player, target);
    }
}
