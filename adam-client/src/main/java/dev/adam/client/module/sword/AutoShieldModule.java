package dev.adam.client.module.sword;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Auto Shield: holds your shield up while an enemy is close, and lowers it when they are gone.
 * You need a shield in either hand, and your main hand must be empty, a shield, a sword or an axe
 * (so it never stops you from eating or shooting).
 */
public class AutoShieldModule extends Module {

    // Stored in tenths of a block (50 = 5.0).
    private final IntSetting range = addSetting("range", "Trigger range", 20, 80, 50, 10);
    private final BoolSetting players = addToggle("players", "Players", true);
    private final BoolSetting mobs = addToggle("mobs", "Hostile mobs", true);

    /** True while we are the ones holding the use key down. */
    private boolean holding;

    public AutoShieldModule() {
        super("autoshield", "Auto Shield",
                "Raises your shield when an enemy gets close.",
                Category.SWORD, -1);
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onDisable(Minecraft mc) {
        release(mc);
    }

    private void release(Minecraft mc) {
        if (holding) {
            mc.options.keyUse.setDown(false);
            holding = false;
        }
    }

    @Override
    public void onEndTick(Minecraft mc) {
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || mc.screen != null || FreecamModule.isActive() || !me.isAlive()) {
            release(mc);
            return;
        }
        boolean threat = canBlock(me) && threatNear(mc, me);
        if (threat && !holding) {
            mc.options.keyUse.setDown(true);
            holding = true;
        } else if (!threat && holding) {
            release(mc);
        }
    }

    private static boolean canBlock(LocalPlayer me) {
        boolean hasShield = me.getOffhandItem().is(Items.SHIELD) || me.getMainHandItem().is(Items.SHIELD);
        if (!hasShield) return false;
        ItemStack main = me.getMainHandItem();
        return main.isEmpty() || main.is(Items.SHIELD) || main.is(ItemTags.SWORDS) || main.is(ItemTags.AXES);
    }

    private boolean threatNear(Minecraft mc, LocalPlayer me) {
        double reach = range.get() / 10.0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == me || !(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (me.distanceTo(e) > reach) continue;
            if (e instanceof Player other) {
                if (players.get() && !other.isSpectator() && !FriendsModule.isFriend(other)) return true;
            } else if (mobs.get() && e.getType().getCategory() == MobCategory.MONSTER) {
                return true;
            }
        }
        return false;
    }
}
