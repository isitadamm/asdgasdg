package dev.adam.client.module.protection;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.cam.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Staff Protection: shows the names of every player within a radius of you, and can pop up a
 * red alert on screen when someone new comes near. It can only see players your game has loaded.
 */
public class StaffProtectionModule extends Module {

    public static StaffProtectionModule INSTANCE;
    private static final int COLOR = 0xFFFFC857;

    private final IntSetting radius = addSetting("radius", "Radius", 10, 128, 50);
    private final IntSetting position = addChoice("position", "Position", 0,
            "Top left", "Top right", "Bottom left", "Bottom right");
    private final BoolSetting showList = addToggle("list", "Show nearby players", true);
    private final BoolSetting screenAlert = addToggle("alert", "Screen alert", true);

    /** Read by the HUD. */
    volatile ProtectionHud.Panel panel;

    private Set<String> known = new HashSet<>();
    private ClientLevel knownLevel;

    public StaffProtectionModule() {
        super("staffprotection", "Staff Protection",
                "Lists players near you by name, with an optional screen alert.",
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
        LocalPlayer me = mc.player;
        ClientLevel level = mc.level;
        if (me == null || level == null) {
            panel = null;
            knownLevel = null;
            return;
        }

        double max = radius.get();
        // name -> distance, sorted by name first, then re-sorted by distance for display
        Map<String, Double> found = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Player p : level.players()) {
            if (p == me || p == FreecamModule.getCamera()) continue;
            double d = me.distanceTo(p);
            if (d <= max) found.put(p.getName().getString(), d);
        }

        boolean silent = knownLevel != level;
        knownLevel = level;
        if (!silent && screenAlert.get()) {
            for (Map.Entry<String, Double> e : found.entrySet()) {
                if (!known.contains(e.getKey())) {
                    ProtectionHud.alert("Player nearby: " + e.getKey() + " (" + Math.round(e.getValue()) + " m)",
                            COLOR);
                }
            }
        }
        known = new HashSet<>(found.keySet());

        if (!showList.get()) {
            panel = null;
            return;
        }
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(found.entrySet());
        sorted.sort(Map.Entry.comparingByValue());
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < 12; i++) {
            lines.add(sorted.get(i).getKey() + "  " + Math.round(sorted.get(i).getValue()) + " m");
        }
        if (sorted.size() > 12) lines.add("+" + (sorted.size() - 12) + " more");
        if (lines.isEmpty()) lines.add("No one nearby");
        panel = new ProtectionHud.Panel(position.get(), "Players within " + (int) max + " m (" + sorted.size() + ")",
                COLOR, lines);
    }
}
