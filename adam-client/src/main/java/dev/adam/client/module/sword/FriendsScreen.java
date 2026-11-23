package dev.adam.client.module.sword;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import dev.adam.client.config.AdamConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/** Pick friends: everyone on your server right now, plus friends who are offline. */
public class FriendsScreen extends Screen {
    private static final int PW = 280, PH = 232, ROWS = 7;
    private static final int PANEL = 0xF01B1E23, LINE = 0xFF3A3F48;

    private final Screen parent;
    private int page = 0;
    private int px, py;
    private final List<String> shown = new ArrayList<>();

    public FriendsScreen(Screen parent) {
        super(Component.literal("Friends"));
        this.parent = parent;
    }

    /** Online players (from the tab list) merged with saved friends, friends first. */
    private List<String> names() {
        TreeMap<String, String> byLower = new TreeMap<>();
        String me = minecraft.player == null ? "" : minecraft.player.getName().getString();
        if (minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                if (n != null && !n.equalsIgnoreCase(me)) byLower.put(n.toLowerCase(Locale.ROOT), n);
            }
        }
        for (String f : AdamConfig.get().friends) byLower.putIfAbsent(f.toLowerCase(Locale.ROOT), f);

        List<String> friends = new ArrayList<>();
        List<String> others = new ArrayList<>();
        for (String n : byLower.values()) {
            if (FriendsModule.isFriendName(n)) friends.add(n);
            else others.add(n);
        }
        friends.addAll(others);
        return friends;
    }

    @Override
    protected void init() {
        shown.clear();
        px = (width - PW) / 2;
        py = (height - PH) / 2;

        List<String> all = names();
        int maxPage = Math.max(0, (all.size() - 1) / ROWS);
        if (page > maxPage) page = maxPage;

        // Add a friend by typing a name (for someone who is offline or not listed)
        EditBox box = new EditBox(font, px + 10, py + 30, PW - 80, 18, Component.literal("Name"));
        box.setHint(Component.literal("Add a name..."));
        box.setMaxLength(16);
        addRenderableWidget(box);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            FriendsModule.add(box.getValue());
            rebuildWidgets();
        }).bounds(px + PW - 66, py + 30, 56, 18).build());

        int start = page * ROWS;
        for (int r = 0; r < ROWS; r++) {
            int idx = start + r;
            if (idx >= all.size()) break;
            String name = all.get(idx);
            boolean friend = FriendsModule.isFriendName(name);
            int y = py + 56 + r * 20;
            shown.add(name);
            addRenderableWidget(Button.builder(Component.literal(friend ? "Remove" : "Add friend"), b -> {
                if (friend) FriendsModule.remove(name);
                else FriendsModule.add(name);
                rebuildWidgets();
            }).bounds(px + PW - 96, y, 86, 18).build());
        }

        int navY = py + 56 + ROWS * 20 + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) page--;
            rebuildWidgets();
        }).bounds(px + 10, navY, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (page < maxPage) page++;
            rebuildWidgets();
        }).bounds(px + 38, navY, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(px + PW - 96, navY, 86, 18).build());
    }

    // Paint our own background, so nothing is drawn twice.
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(px, py, px + PW, py + PH, PANEL);
        g.fill(px, py, px + PW, py + 1, LINE);
        g.fill(px, py + PH - 1, px + PW, py + PH, LINE);
        g.fill(px, py, px + 1, py + PH, LINE);
        g.fill(px + PW - 1, py, px + PW, py + PH, LINE);

        super.render(g, mouseX, mouseY, delta);

        g.drawString(font, "Friend Protection", px + 12, py + 10, 0xFFFFFFFF, true);
        g.drawString(font, "Friends can't be hit by you or Kill Aura", px + 12, py + 20, 0xFF9AA1AD, false);

        for (int r = 0; r < shown.size(); r++) {
            String name = shown.get(r);
            boolean friend = FriendsModule.isFriendName(name);
            int y = py + 56 + r * 20;
            g.fill(px + 12, y + 5, px + 20, y + 13, friend ? 0xFF4BE3B0 : 0xFF3A3F48);
            g.drawString(font, font.plainSubstrByWidth(name, 130), px + 26, y + 5,
                    friend ? 0xFFFFFFFF : 0xFF9AA1AD, false);
        }
        if (shown.isEmpty()) {
            g.drawString(font, "No players found. Type a name above.", px + 12, py + 60, 0xFF9AA1AD, false);
        }
        List<String> all = names();
        int pages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
        g.drawString(font, (page + 1) + "/" + pages, px + 70, py + 56 + ROWS * 20 + 9, 0xFF9AA1AD, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
