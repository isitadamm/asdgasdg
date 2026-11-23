package dev.adam.client.module.sword;

import dev.adam.client.config.AdamConfig;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Friend Protection: players on your friend list can't be hit by you, and Kill Aura skips them.
 * Pick friends from the players on your current server in "Configure module".
 */
public class FriendsModule extends Module {

    public static FriendsModule INSTANCE;

    public FriendsModule() {
        super("friends", "Friend Protection",
                "Friends can't be hit by you or Kill Aura. Pick them from the players on your server.",
                Category.SWORD, -1);
        INSTANCE = this;
    }

    @Override
    protected boolean persistsEnabled() {
        return true;
    }

    @Override
    protected boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new FriendsScreen(parent);
    }

    /** True if this entity is a player on your friend list (and protection is on). */
    public static boolean isFriend(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !(entity instanceof Player)) return false;
        return isFriendName(entity.getName().getString());
    }

    public static boolean isFriendName(String name) {
        if (name == null) return false;
        for (String f : AdamConfig.get().friends) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static void add(String name) {
        name = name.trim();
        if (name.isEmpty() || isFriendName(name)) return;
        AdamConfig.get().friends.add(name);
        AdamConfig.save();
    }

    public static void remove(String name) {
        AdamConfig.get().friends.removeIf(f -> f.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)));
        AdamConfig.save();
    }
}
