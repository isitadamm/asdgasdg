package dev.adam.client.module.sword;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Kill Aura: automatically attacks the closest valid target in range.
 * Friends are skipped, tamed pets are never hit, and the target must be in line of sight.
 */
public class KillAuraModule extends Module {

    // Range is stored in tenths of a block (40 = 4.0).
    private final IntSetting range = addSetting("range", "Range", 20, 60, 40, 10);
    private final IntSetting attacksPerSecond = addSetting("cps", "Attacks/sec", 1, 20, 10);
    private final BoolSetting players = addToggle("players", "Players", true);
    private final BoolSetting hostile = addToggle("hostile", "Hostile mobs", true);
    private final BoolSetting passive = addToggle("passive", "Passive mobs", false);
    private final BoolSetting rotate = addToggle("rotate", "Look at target", false);
    private final BoolSetting fullCharge = addToggle("charge", "Wait for full charge", true);

    private int ticksSinceAttack = 100;

    public KillAuraModule() {
        super("killaura", "Kill Aura",
                "Attacks the closest target in range. Skips friends and pets.",
                Category.SWORD, InputConstants.KEY_R);
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    public void onEndTick(Minecraft mc) {
        LocalPlayer me = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (me == null || mc.level == null || gameMode == null) return;
        if (mc.screen != null || FreecamModule.isActive()) return;
        if (!me.isAlive() || me.isSpectator()) return;
        // Don't interrupt eating or drawing a bow (raising a shield is fine).
        if (me.isUsingItem() && !me.getUseItem().is(Items.SHIELD)) return;

        ticksSinceAttack++;
        int wait = Math.max(1, 20 / attacksPerSecond.get());
        if (ticksSinceAttack < wait) return;
        if (fullCharge.get() && me.getAttackStrengthScale(0.5F) < 1.0F) return;

        Entity target = findTarget(mc, me);
        if (target == null) return;

        if (rotate.get()) lookAt(me, target);

        // The attack itself goes through the game mode, so Friend Protection, Auto Weapon and
        // Criticals all see it (they hook into that same call).
        gameMode.attack(me, target);
        me.swing(InteractionHand.MAIN_HAND);
        ticksSinceAttack = 0;
    }

    private Entity findTarget(Minecraft mc, LocalPlayer me) {
        double reach = range.get() / 10.0;
        double reachSq = reach * reach;
        Vec3 eye = me.getEyePosition();
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!isValid(me, e)) continue;
            double d = e.getBoundingBox().distanceToSqr(eye);
            if (d > reachSq || d >= bestDist) continue;
            if (!me.hasLineOfSight(e)) continue;
            best = e;
            bestDist = d;
        }
        return best;
    }

    private boolean isValid(LocalPlayer me, Entity e) {
        if (e == me || !(e instanceof LivingEntity living) || !living.isAlive()) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof Player other) {
            if (!players.get()) return false;
            if (other.isSpectator() || other.getAbilities().invulnerable) return false;
            return !FriendsModule.isFriend(other);
        }
        if (e instanceof TamableAnimal pet && pet.isTame()) return false;
        if (e.getType().getCategory() == MobCategory.MONSTER) return hostile.get();
        return passive.get();
    }

    private static void lookAt(LocalPlayer me, Entity target) {
        Vec3 eye = me.getEyePosition();
        Vec3 t = target.getBoundingBox().getCenter();
        double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        me.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        me.setXRot((float) -Math.toDegrees(Math.atan2(dy, flat)));
    }
}
