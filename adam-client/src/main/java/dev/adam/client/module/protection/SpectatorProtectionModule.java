package dev.adam.client.module.protection;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Spectator Protection: lists every player on the server who is in spectator mode (read from the
 * tab list), and can pop up a red alert when a new one appears. Anyone hidden from the tab list
 * (for example by a vanish plugin) can't be seen.
 */
public class SpectatorProtectionModule extends Module {

    public static SpectatorProtectionModule INSTANCE;
    private static final int COLOR = 0xFFFF6B6B;

    private final IntSetting position = addChoice("position", "Position", 1,
            "Top left", "Top right", "Bottom left", "Bottom right");
    private final BoolSetting showList = addToggle("list", "Show spectators", true);
    private final BoolSetting screenAlert = addToggle("alert", "Screen alert", true);

    /** Read by the HUD. */
    volatile ProtectionHud.Panel panel;

    private Set<String> known = new HashSet<>();
    private ClientLevel knownLevel;

    public SpectatorProtectionModule() {
        super("spectatorprotection", "Spectator Protection",
                "Lists players in spectator mode, with an optional screen alert.",
                Category.PROTECTION, -1);
        INSTANCE = this;
    }

    @Override
    protected boolean persistsEnabled() {
        return true;
    }

    @Override
    protected void onEnable(Minecraft mc) {
        knownLevel = null; // first scan after enabling is silent
    }

    @Override
    protected void onDisable(Minecraft mc) {
        panel = null;
        known = new HashSet<>();
        knownLevel = null;
    }

    @Override
    public void onEndTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (mc.player == null || level == null || mc.getConnection() == null) {
            panel = null;
            knownLevel = null;
            return;
        }

        String me = mc.player.getName().getString();
        Set<String> spectators = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getGameMode() != GameType.SPECTATOR) continue;
            String name = info.getProfile().name();
            if (name != null && !name.equalsIgnoreCase(me)) spectators.add(name);
        }

        boolean silent = knownLevel != level;
        knownLevel = level;
        if (!silent && screenAlert.get()) {
            for (String name : spectators) {
                if (!known.contains(name)) {
                    ProtectionHud.alert("Spectator on server: " + name, COLOR);
                }
            }
        }
        known = new HashSet<>(spectators);

        if (!showList.get()) {
            panel = null;
            return;
        }
        List<String> lines = new ArrayList<>();
        int shown = 0;
        for (String name : spectators) {
            if (shown++ >= 12) break;
            lines.add(name);
        }
        if (spectators.size() > 12) lines.add("+" + (spectators.size() - 12) + " more");
        if (lines.isEmpty()) lines.add("No spectators");
        panel = new ProtectionHud.Panel(position.get(), "Spectators (" + spectators.size() + ")", COLOR, lines);
    }
}
