package dev.adam.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.module.blocks.BlocksModule;
import dev.adam.client.module.cam.FreecamModule;
import dev.adam.client.module.protection.SpectatorProtectionModule;
import dev.adam.client.module.protection.StaffProtectionModule;
import dev.adam.client.module.sword.AutoShieldModule;
import dev.adam.client.module.sword.AutoWeaponModule;
import dev.adam.client.module.sword.CriticalsModule;
import dev.adam.client.module.sword.FriendsModule;
import dev.adam.client.module.sword.HitboxModule;
import dev.adam.client.module.sword.KillAuraModule;
import dev.adam.client.module.sword.ReachModule;
import dev.adam.client.module.wings.BoatFlyModule;
import dev.adam.client.module.wings.FlyModule;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static int activeCount() {
        int n = 0;
        for (Module m : MODULES) if (m.isEnabled()) n++;
        return n;
    }

    /** Creates every module and registers one rebindable key per module. */
    public static void init(KeyMapping.Category keyCategory) {
        MODULES.add(new BlocksModule());
        MODULES.add(new FreecamModule());
        MODULES.add(new FlyModule());
        MODULES.add(new BoatFlyModule());
        MODULES.add(new KillAuraModule());
        MODULES.add(new ReachModule());
        MODULES.add(new CriticalsModule());
        MODULES.add(new AutoShieldModule());
        MODULES.add(new HitboxModule());
        MODULES.add(new AutoWeaponModule());
        MODULES.add(new FriendsModule());
        MODULES.add(new StaffProtectionModule());
        MODULES.add(new SpectatorProtectionModule());

        for (Module m : MODULES) {
            KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.adam.module." + m.id,
                    InputConstants.Type.KEYSYM,
                    m.defaultKey,
                    keyCategory));
            m.setKeyMapping(key);
            m.restore();
        }
    }

    public static void startTick(Minecraft mc) {
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onStartTick(mc);
        }
    }

    public static void endTick(Minecraft mc) {
        for (Module m : MODULES) {
            KeyMapping key = m.keyMapping();
            while (key != null && key.consumeClick()) {
                if (mc.player != null && mc.level != null) {
                    m.toggle();
                }
            }
        }
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onEndTick(mc);
        }
    }
}
