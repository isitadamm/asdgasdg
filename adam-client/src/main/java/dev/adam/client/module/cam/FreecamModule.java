package dev.adam.client.module.cam;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Adam Camera: detach the camera and fly around while your real player stands still. */
public class FreecamModule extends Module {

    public static FreecamModule INSTANCE;

    // Speeds are shown as whole numbers and divided by 10 into blocks per tick.
    private final IntSetting speed = addSetting("speed", "Speed", 1, 20, 5);
    private final IntSetting fastSpeed = addSetting("fast", "Fast speed (Shift)", 1, 40, 20);

    private AdamCamera camera;
    private LocalPlayer owner;
    private ClientLevel ownerLevel;
    private ClientInput realInput;
    private ClientInput dummyInput;
    private CameraType rememberedCameraType;
    /** True while we switch ourselves off because the world changed (no chat message). */
    private boolean quiet;

    public FreecamModule() {
        super("freecam", "Freecam",
                "Detach your camera and fly around. WASD moves, Space goes up, Ctrl goes down, Shift is faster. Your real player stays put.",
                Category.CAM, InputConstants.KEY_F4);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.camera != null;
    }

    public static AdamCamera getCamera() {
        return INSTANCE == null ? null : INSTANCE.camera;
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onEnable(Minecraft mc) {
        LocalPlayer player = mc.player;
        owner = player;
        ownerLevel = mc.level;

        // Freeze the real player by swapping in an input that never presses anything.
        realInput = player.input;
        dummyInput = new ClientInput();
        player.input = dummyInput;

        rememberedCameraType = mc.options.getCameraType();
        if (rememberedCameraType != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }

        camera = new AdamCamera(mc.level);
        camera.snapTo(player.getX(), player.getEyeY() - camera.getEyeHeight(), player.getZ(),
                player.getYRot(), player.getXRot());
        camera.spawn();
        mc.setCameraEntity(camera);
        mc.smartCull = false;

        player.displayClientMessage(Component.translatable("adam.freecam.enabled"), true);
    }

    @Override
    protected void onDisable(Minecraft mc) {
        release(mc, !quiet);
    }

    private void release(Minecraft mc, boolean announce) {
        AdamCamera oldCamera = camera;
        camera = null;
        mc.smartCull = true;

        if (mc.player != null) {
            // Only hand the old input back if it's still the same player object.
            if (mc.player == owner && realInput != null) {
                mc.player.input = realInput;
            }
            mc.setCameraEntity(mc.player);
        }

        if (oldCamera != null && mc.level != null && mc.level == ownerLevel) {
            oldCamera.despawn();
        }

        if (rememberedCameraType != null) {
            mc.options.setCameraType(rememberedCameraType);
        }

        if (announce && mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("adam.freecam.disabled"), true);
        }

        owner = null;
        ownerLevel = null;
        realInput = null;
        dummyInput = null;
        rememberedCameraType = null;
    }

    @Override
    public void onStartTick(Minecraft mc) {
        if (camera == null) {
            return;
        }
        // Turn the camera off if the world/player changed under us (disconnect, respawn, dimension change, death).
        if (mc.player == null || mc.level == null || mc.player != owner || mc.level != ownerLevel
                || mc.player.isDeadOrDying()) {
            quiet = true;
            setEnabled(false);
            quiet = false;
            return;
        }
        // Keep the real player frozen while the camera is out.
        mc.player.input = dummyInput;
    }

    @Override
    public void onEndTick(Minecraft mc) {
        // Move the camera ourselves, once per tick.
        if (camera != null && mc.screen == null) {
            camera.updateMovement(speed.get() / 10.0, fastSpeed.get() / 10.0);
        }
    }
}
