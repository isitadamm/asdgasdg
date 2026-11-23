package dev.adam.client.module.cam;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The detached camera. It is a client-only fake player that the game uses as the
 * "camera entity", so vanilla handles rendering and mouse-look for us.
 *
 * Movement is NOT done through vanilla physics (that was unreliable for a fake
 * client entity). Instead the Freecam module calls {@link #updateMovement} once
 * per client tick and we simply move the position ourselves.
 */
public class AdamCamera extends AbstractClientPlayer {

    // Negative id so it can never clash with a real entity id from the server.
    private static final int CAMERA_ENTITY_ID = -8080;

    private final KeyboardInput keyboardInput;

    public AdamCamera(ClientLevel level) {
        super(level, new GameProfile(UUID.randomUUID(), "AdamCamera"));
        setId(CAMERA_ENTITY_ID);
        setPose(Pose.SWIMMING);
        setNoGravity(true);
        this.noPhysics = true;
        getAbilities().flying = true;
        keyboardInput = new KeyboardInput(Minecraft.getInstance().options);
    }

    public void spawn() {
        ((ClientLevel) level()).addEntity(this);
    }

    public void despawn() {
        ((ClientLevel) level()).removeEntity(getId(), Entity.RemovalReason.DISCARDED);
    }

    /** Vanilla physics/AI is intentionally skipped; see updateMovement(). */
    @Override
    public void tick() {
    }

    /**
     * Called once per client tick. WASD = move, Space = up, Ctrl = down, Shift = fast.
     * Speeds are in blocks per tick (20 ticks = 1 second).
     */
    public void updateMovement(double normalSpeed, double fastSpeed) {
        keyboardInput.tick();
        var keys = keyboardInput.keyPresses;
        Minecraft mc = Minecraft.getInstance();

        // Remember where we were so rendering can interpolate smoothly.
        setOldPosAndRot();

        double speed = keys.shift() ? fastSpeed : normalSpeed;

        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90);

        double x = 0;
        double z = 0;
        if (keys.forward()) { x += forward.x; z += forward.z; }
        if (keys.backward()) { x -= forward.x; z -= forward.z; }
        if (keys.right()) { x += side.x; z += side.z; }
        if (keys.left()) { x -= side.x; z -= side.z; }

        // Keep diagonal movement the same speed as straight movement.
        double length = Math.sqrt(x * x + z * z);
        if (length > 1.0E-6) {
            x = x / length * speed;
            z = z / length * speed;
        }

        double y = 0;
        if (keys.jump()) y += speed;
        if (mc.options.keySprint.isDown()) y -= speed;

        setPos(getX() + x, getY() + y, getZ() + z);
        setDeltaMovement(Vec3.ZERO);
    }

    // Always stay in the small swimming pose so the camera isn't a tall standing hitbox.
    @Override
    public void setPose(Pose pose) {
        super.setPose(Pose.SWIMMING);
    }

    // Use the exact current rotation (no tick interpolation) so mouse-look feels instant.
    @Override
    public float getViewXRot(float partialTick) {
        return getXRot();
    }

    @Override
    public float getViewYRot(float partialTick) {
        return getYRot();
    }
}
