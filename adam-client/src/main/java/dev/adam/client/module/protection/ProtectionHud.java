package dev.adam.client.module.protection;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Staff / Spectator Protection lists (in the corner you chose) and the red alert
 * banners at the top of the screen.
 */
public final class ProtectionHud {

    /** corner: 0 = top left, 1 = top right, 2 = bottom left, 3 = bottom right. */
    public record Panel(int corner, String title, int titleColor, List<String> lines) {
    }

    private record Alert(String text, int color, long until) {
    }

    private static final List<Alert> ALERTS = new ArrayList<>();
    private static final long ALERT_MS = 5000;

    private ProtectionHud() {
    }

    public static void alert(String text, int color) {
        synchronized (ALERTS) {
            ALERTS.add(new Alert(text, color, System.currentTimeMillis() + ALERT_MS));
            while (ALERTS.size() > 4) ALERTS.remove(0);
        }
    }

    public static void render(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Lists: stack panels that share a corner so they never overlap.
        int[] used = new int[4];
        List<Panel> panels = new ArrayList<>();
        if (StaffProtectionModule.INSTANCE != null && StaffProtectionModule.INSTANCE.isEnabled()
                && StaffProtectionModule.INSTANCE.panel != null) {
            panels.add(StaffProtectionModule.INSTANCE.panel);
        }
        if (SpectatorProtectionModule.INSTANCE != null && SpectatorProtectionModule.INSTANCE.isEnabled()
                && SpectatorProtectionModule.INSTANCE.panel != null) {
            panels.add(SpectatorProtectionModule.INSTANCE.panel);
        }
        for (Panel p : panels) {
            int corner = Math.max(0, Math.min(3, p.corner()));
            int boxW = font.width(p.title());
            for (String line : p.lines()) boxW = Math.max(boxW, font.width(line));
            boxW += 12;
            int boxH = 8 + (p.lines().size() + 1) * 11;
            boolean right = corner == 1 || corner == 3;
            boolean bottom = corner >= 2;
            int x = right ? w - boxW - 4 : 4;
            // Bottom corners sit above the hotbar and chat line.
            int y = bottom ? h - 48 - boxH - used[corner] : 4 + used[corner];
            g.fill(x, y, x + boxW, y + boxH, 0xAA101216);
            g.fill(x, y, x + 2, y + boxH, p.titleColor());
            g.drawString(font, p.title(), x + 6, y + 4, p.titleColor(), true);
            int ty = y + 4 + 11;
            for (String line : p.lines()) {
                g.drawString(font, line, x + 6, ty, 0xFFFFFFFF, true);
                ty += 11;
            }
            used[corner] += boxH + 4;
        }

        // Alerts: red banners, top centre, newest at the bottom of the stack.
        long now = System.currentTimeMillis();
        List<Alert> live = new ArrayList<>();
        synchronized (ALERTS) {
            ALERTS.removeIf(a -> a.until() < now);
            live.addAll(ALERTS);
        }
        int y = 22;
        for (Alert a : live) {
            int tw = font.width(a.text());
            int x = (w - tw) / 2;
            g.fill(x - 8, y - 3, x + tw + 8, y + 12, 0xCC1A0D0D);
            g.fill(x - 8, y - 3, x + tw + 8, y - 2, a.color());
            g.drawString(font, a.text(), x, y + 1, a.color(), true);
            y += 18;
        }
    }
}
