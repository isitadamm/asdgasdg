package dev.adam.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adam.client.config.AdamConfig;
import dev.adam.client.gui.AdamScreen;
import dev.adam.client.module.ModuleManager;
import dev.adam.client.module.blocks.BlocksModule;
import dev.adam.client.module.blocks.AdamRender;
import dev.adam.client.module.blocks.Overlay;
import dev.adam.client.module.protection.ProtectionHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class AdamClient implements ClientModInitializer {

    public static final String MOD_ID = "adam";

    private static KeyMapping menuKey;

    public static KeyMapping menuKey() {
        return menuKey;
    }

    @Override
    public void onInitializeClient() {
        AdamConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        // Right Shift opens the menu; every module also gets its own rebindable key.
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.adam.menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT,
                category));

        ModuleManager.init(category);
        AdamRender.init();

        ClientTickEvents.START_CLIENT_TICK.register(ModuleManager::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    mc.setScreen(new AdamScreen());
                }
            }
            ModuleManager.endTick(mc);
        });

        // Draw the block ESP as a proper HUD element, underneath the hotbar and crosshair,
        // instead of on top of everything at the very end of the frame.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "block_esp"),
                (graphics, tickCounter) -> {
                    if (BlocksModule.isActive()) {
                        Overlay.render(graphics, tickCounter);
                    }
                });

        // Staff / Spectator Protection lists and alerts.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(MOD_ID, "protection_hud"),
                ProtectionHud::render);
    }
}
