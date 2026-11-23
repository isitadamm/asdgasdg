package dev.adam.client.module.wings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.UUID;

/**
 * Fly: creative-style flight on foot.
 *
 * Jump into the air and you start flying. Space goes up, Shift goes down, Ctrl (sprint) is faster.
 * Land by flying down onto the ground.
 *
 * In singleplayer the built-in server is told you may fly too, so you don't take fall damage
 * or get stopped. On a multiplayer server this is client-side only: most servers block flying.
 */
public class FlyModule extends Module {

    private static final float VANILLA_FLY_SPEED = 0.05f;

    private final IntSetting speed = addSetting("speed", "Speed", 1, 10, 3);

    public FlyModule() {
        super("fly", "Fly",
                "Fly like in creative. Jump to take off, Space goes up, Shift goes down, Ctrl is faster.",
                Category.WINGS, InputConstants.KEY_G);
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onEnable(Minecraft mc) {
        setServerFlight(mc, true);
    }

    @Override
    protected void onDisable(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player != null) {
            var abilities = player.getAbilities();
            abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
            // Creative and spectator keep their own flight; everyone else drops back to normal.
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly = false;
                if (abilities.flying) {
                    abilities.flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
        setServerFlight(mc, false);
    }

    @Override
    public void onEndTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        // Freecam owns the keyboard while it is on, and boats are Boatfly's job.
        if (FreecamModule.isActive() || player.isPassenger()) return;

        var abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.setFlyingSpeed(VANILLA_FLY_SPEED * speed.get());

        // Start flying as soon as you leave the ground.
        if (!abilities.flying && !player.onGround() && !player.isFallFlying()) {
            abilities.flying = true;
            player.onUpdateAbilities();
        }

        // A new server-side player appears after respawn or a dimension change, so refresh now and then.
        if (player.tickCount % 40 == 0) {
            setServerFlight(mc, true);
        }
    }

    /** Singleplayer only: tell the built-in server this player may fly. Does nothing on other servers. */
    private static void setServerFlight(Minecraft mc, boolean allow) {
        var server = mc.getSingleplayerServer();
        LocalPlayer me = mc.player;
        if (server == null || me == null) return;
        UUID id = me.getUUID();
        server.execute(() -> {
            var serverPlayer = server.getPlayerList().getPlayer(id);
            if (serverPlayer == null) return;
            var abilities = serverPlayer.getAbilities();
            boolean target = allow || serverPlayer.isCreative() || serverPlayer.isSpectator();
            if (abilities.mayfly != target) {
                abilities.mayfly = target;
                if (!target) abilities.flying = false;
                serverPlayer.onUpdateAbilities();
            }
        });
    }
}
