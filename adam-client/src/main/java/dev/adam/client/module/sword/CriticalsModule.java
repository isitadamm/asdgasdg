package dev.adam.client.module.sword;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Criticals: right before each hit, sends two tiny "hop" movement packets so the server counts
 * you as falling, which makes the hit a critical. Works for your own hits and for Kill Aura.
 * Vanilla never gives crits while sprinting, so it does nothing then.
 */
public class CriticalsModule extends Module {

    public static CriticalsModule INSTANCE;

    public CriticalsModule() {
        super("criticals", "Criticals",
                "Your hits land as critical hits (not while sprinting).",
                Category.SWORD, -1);
        INSTANCE = this;
    }

    /** Called from the attack hook just before an attack is sent. */
    public static void prepare(Player player, Entity target) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (!(target instanceof LivingEntity)) return;
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.getConnection() == null) return;
        if (player.isSprinting() || player.isPassenger() || player.isInWater() || player.isInLava()
                || player.onClimbable() || player.isFallFlying() || player.getAbilities().flying) {
            return;
        }
        send(mc, player, 0.0625);
        send(mc, player, 0.0);
    }

    private static void send(Minecraft mc, Player player, double height) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                player.getX(), player.getY() + height, player.getZ(), false, false));
    }
}
