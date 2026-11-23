package dev.adam.client.module.wings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Boatfly: fly while you are sitting in a boat.
 *
 * The boat ignores gravity and goes where you point it: W/A/S/D move it relative to where you
 * look, Space goes up, Ctrl goes down. It hovers in place when you let go of everything.
 * Works in singleplayer; most multiplayer servers will rubber-band or kick boat flying.
 */
public class BoatFlyModule extends Module {

    // Shown as whole numbers: speed * 0.2 and vertical * 0.1 blocks per tick.
    private final IntSetting speed = addSetting("speed", "Speed", 1, 15, 5);
    private final IntSetting vertical = addSetting("vertical", "Vertical speed", 1, 10, 5);

    /** The boat we last took over, so we can give gravity back when you get out. */
    private Entity tracked;

    public BoatFlyModule() {
        super("boatfly", "Boatfly",
                "Fly while riding a boat. WASD moves it where you look, Space goes up, Ctrl goes down, and it hovers when you let go.",
                Category.WINGS, InputConstants.KEY_B);
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onDisable(Minecraft mc) {
        release();
    }

    private void release() {
        if (tracked != null) {
            tracked.setNoGravity(false);
            tracked = null;
        }
    }

    /** Boats and rafts, including the chest versions (oak_boat, bamboo_raft, ...). */
    private static boolean isBoat(Entity entity) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return path.endsWith("boat") || path.endsWith("raft");
    }

    @Override
    public void onEndTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            release();
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle != tracked) {
            release();
        }
        if (vehicle == null || !isBoat(vehicle) || vehicle.getControllingPassenger() != player) {
            return;
        }
        // Freecam owns the keyboard while it is on.
        if (FreecamModule.isActive()) {
            return;
        }

        tracked = vehicle;
        vehicle.setNoGravity(true);

        // Menu or chat open: just hover.
        if (mc.screen != null) {
            vehicle.setDeltaMovement(Vec3.ZERO);
            return;
        }

        var options = mc.options;
        double horizontal = speed.get() * 0.2;
        double up = vertical.get() * 0.1;

        float yaw = player.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90);

        double x = 0;
        double z = 0;
        if (options.keyUp.isDown()) { x += forward.x; z += forward.z; }
        if (options.keyDown.isDown()) { x -= forward.x; z -= forward.z; }
        if (options.keyRight.isDown()) { x += side.x; z += side.z; }
        if (options.keyLeft.isDown()) { x -= side.x; z -= side.z; }

        double length = Math.sqrt(x * x + z * z);
        if (length > 1.0E-6) {
            x = x / length * horizontal;
            z = z / length * horizontal;
        }

        double y = 0;
        if (options.keyJump.isDown()) y += up;
        if (options.keySprint.isDown()) y -= up;

        // Point the boat the way you look, and move it ourselves so vanilla boat physics can't fight us.
        vehicle.setYRot(yaw);
        vehicle.setDeltaMovement(Vec3.ZERO);
        vehicle.move(MoverType.SELF, new Vec3(x, y, z));
    }
}
