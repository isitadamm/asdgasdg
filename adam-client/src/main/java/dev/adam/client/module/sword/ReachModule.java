package dev.adam.client.module.sword;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Reach: hit entities and reach blocks from further away.
 *
 * It raises your interaction range on the client, and in singleplayer also on the built-in server
 * (which checks distance too). On multiplayer servers only the client changes, so hits further than
 * the server allows are simply ignored.
 */
public class ReachModule extends Module {

    private static final double VANILLA_ENTITY_REACH = 3.0;
    private static final double VANILLA_BLOCK_REACH = 4.5;

    // Stored in tenths of a block (50 = 5.0).
    private final IntSetting entityReach = addSetting("entity", "Hit reach", 30, 100, 50, 10);
    private final IntSetting blockReach = addSetting("block", "Block reach", 45, 100, 60, 10);

    public ReachModule() {
        super("reach", "Reach",
                "Hit mobs and reach blocks from further away.",
                Category.SWORD, -1);
    }

    @Override
    protected boolean canEnable(Minecraft mc) {
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onEnable(Minecraft mc) {
        syncServer(mc, true);
    }

    @Override
    protected void onDisable(Minecraft mc) {
        applyClient(mc.player, VANILLA_ENTITY_REACH, VANILLA_BLOCK_REACH);
        syncServer(mc, false);
    }

    @Override
    public void onSettingChanged(IntSetting setting) {
        syncServer(Minecraft.getInstance(), true);
    }

    @Override
    public void onEndTick(Minecraft mc) {
        LocalPlayer me = mc.player;
        if (me == null) return;
        applyClient(me, entityReach.get() / 10.0, blockReach.get() / 10.0);
        // A new server-side player appears after respawn or a dimension change, so refresh now and then.
        if (me.tickCount % 40 == 0) syncServer(mc, true);
    }

    private static void applyClient(LocalPlayer me, double entity, double block) {
        if (me == null) return;
        var e = me.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (e != null && e.getBaseValue() != entity) e.setBaseValue(entity);
        var b = me.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (b != null && b.getBaseValue() != block) b.setBaseValue(block);
    }

    /** Singleplayer only: give the built-in server's copy of you the same reach. */
    private void syncServer(Minecraft mc, boolean on) {
        var server = mc.getSingleplayerServer();
        LocalPlayer me = mc.player;
        if (server == null || me == null) return;
        UUID id = me.getUUID();
        double entity = on ? entityReach.get() / 10.0 : VANILLA_ENTITY_REACH;
        double block = on ? blockReach.get() / 10.0 : VANILLA_BLOCK_REACH;
        server.execute(() -> {
            var serverPlayer = server.getPlayerList().getPlayer(id);
            if (serverPlayer == null) return;
            var e = serverPlayer.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null && e.getBaseValue() != entity) e.setBaseValue(entity);
            var b = serverPlayer.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null && b.getBaseValue() != block) b.setBaseValue(block);
        });
    }
}
