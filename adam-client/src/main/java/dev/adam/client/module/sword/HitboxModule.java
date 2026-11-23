package dev.adam.client.module.sword;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Hitbox Expand: makes living things easier to hit by growing the area your crosshair
 * counts as "on them". Only changes aiming on your side; the server still checks distance.
 */
public class HitboxModule extends Module {

    public static HitboxModule INSTANCE;

    // Stored in tenths of a block (3 = 0.3 blocks of extra size on every side).
    private final IntSetting size = addSetting("size", "Extra size", 1, 10, 3, 10);

    public HitboxModule() {
        super("hitbox", "Hitbox Expand",
                "Makes mobs and players easier to hit.",
                Category.SWORD, -1);
        INSTANCE = this;
    }

    /** Extra pick radius (in blocks) for this entity; 0 means unchanged. Used by a mixin. */
    public static float expansion(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return 0.0F;
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) return 0.0F;
        if (entity == Minecraft.getInstance().player || entity == FreecamModule.getCamera()) return 0.0F;
        return INSTANCE.size.get() / 10.0F;
    }
}
