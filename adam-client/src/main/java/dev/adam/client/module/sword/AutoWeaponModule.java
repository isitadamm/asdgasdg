package dev.adam.client.module.sword;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Auto Weapon: right before you hit something, switches your hotbar to the best sword
 * (or axe if you have no sword). Works for your own hits and for Kill Aura.
 */
public class AutoWeaponModule extends Module {

    public static AutoWeaponModule INSTANCE;

    public AutoWeaponModule() {
        super("autoweapon", "Auto Weapon",
                "Switches to your best sword before every hit.",
                Category.SWORD, -1);
        INSTANCE = this;
    }

    /** Called from the attack hook just before an attack is sent. */
    public static void prepare(Player player, Entity target) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (!(target instanceof LivingEntity)) return;
        if (player != Minecraft.getInstance().player) return;

        var inventory = player.getInventory();
        int current = inventory.getSelectedSlot();
        int bestSlot = current;
        int bestScore = score(inventory.getItem(current));
        for (int slot = 0; slot < 9; slot++) {
            int s = score(inventory.getItem(slot));
            if (s > bestScore) {
                bestScore = s;
                bestSlot = slot;
            }
        }
        if (bestSlot != current) {
            inventory.setSelectedSlot(bestSlot);
        }
    }

    /** Swords beat axes; within a kind, a better material wins. Not a weapon = -1. */
    private static int score(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int base;
        if (path.endsWith("_sword")) base = 100;
        else if (path.endsWith("_axe")) base = 50;
        else return -1;
        int tier = 0;
        if (path.startsWith("netherite_")) tier = 6;
        else if (path.startsWith("diamond_")) tier = 5;
        else if (path.startsWith("iron_")) tier = 4;
        else if (path.startsWith("copper_")) tier = 3;
        else if (path.startsWith("stone_")) tier = 2;
        else tier = 1;
        return base + tier;
    }
}
