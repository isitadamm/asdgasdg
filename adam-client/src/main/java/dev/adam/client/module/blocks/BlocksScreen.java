package dev.adam.client.module.blocks;

import dev.adam.client.config.AdamConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.IntConsumer;

/** The block list: pick which blocks are highlighted, and their color and opacity. */
public class BlocksScreen extends Screen {
    private static final int PW = 440, PH = 270, ROWS = 6;
    private static final int[] PRESETS = {0xFF3B3B, 0xF0A030, 0xE8E85A, 0x4BE35A, 0x35D0E0, 0x3D8BFF, 0xB36BFF, 0xFF6BD0, 0xFFFFFF};
    private static final int PANEL = 0xF01B1E23, SIDE = 0xFF16181C, LINE = 0xFF3A3F48, SELECT = 0xFF2A3140;

    private final Screen parent;
    private int selected = 0;
    private int page = 0;
    private int px, py;

    public BlocksScreen(Screen parent) {
        super(Component.literal("Adam Blocks"));
        this.parent = parent;
    }

    public void select(int index) {
        selected = index;
        page = index / ROWS;
    }

    @Override
    protected void init() {
        List<AdamConfig.BlockEntry> entries = AdamConfig.get().blocks;
        BlocksModule module = BlocksModule.INSTANCE;
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        if (selected >= entries.size()) selected = Math.max(0, entries.size() - 1);
        int maxPage = Math.max(0, (entries.size() - 1) / ROWS);
        if (page > maxPage) page = maxPage;

        addRenderableWidget(Button.builder(Component.literal(module.isEnabled() ? "Enabled" : "Disabled"), b -> {
            module.toggle();
            Tracker.dirty = true;
            rebuildWidgets();
        }).bounds(px + PW - 90, py + 8, 78, 18).build());

        int start = page * ROWS;
        for (int r = 0; r < ROWS; r++) {
            int idx = start + r;
            if (idx >= entries.size()) break;
            AdamConfig.BlockEntry e = entries.get(idx);
            int y = py + 34 + r * 22;
            int rowX = px + 10;
            addRenderableWidget(Button.builder(Component.literal(e.enabled ? "On" : "Off"), b -> {
                e.enabled = !e.enabled;
                Tracker.dirty = true;
                rebuildWidgets();
            }).bounds(rowX + 110, y, 34, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Edit"), b -> {
                selected = idx;
                rebuildWidgets();
            }).bounds(rowX + 148, y, 34, 18).build());
            addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                entries.remove(idx);
                Tracker.dirty = true;
                rebuildWidgets();
            }).bounds(rowX + 186, y, 20, 18).build());
        }

        int navY = py + 34 + ROWS * 22 + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) page--;
            rebuildWidgets();
        }).bounds(px + 10, navY, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (page < maxPage) page++;
            rebuildWidgets();
        }).bounds(px + 38, navY, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+ Add block"), b ->
                minecraft.setScreen(new BlockPickerScreen(this))
        ).bounds(px + 68, navY, 138, 18).build());

        int rx = px + 232, rw = 198;
        if (!entries.isEmpty()) {
            AdamConfig.BlockEntry e = entries.get(selected);
            int y = py + 52;
            addRenderableWidget(new IntSlider(rx, y, rw, 20, "Red", 0, 255, (e.color >> 16) & 0xFF,
                    v -> setColor(e, 16, v)));
            addRenderableWidget(new IntSlider(rx, y + 22, rw, 20, "Green", 0, 255, (e.color >> 8) & 0xFF,
                    v -> setColor(e, 8, v)));
            addRenderableWidget(new IntSlider(rx, y + 44, rw, 20, "Blue", 0, 255, e.color & 0xFF,
                    v -> setColor(e, 0, v)));
            addRenderableWidget(new IntSlider(rx, y + 66, rw, 20, "Fill %", 0, 100, e.fill,
                    v -> {
                        e.fill = v;
                        Tracker.dirty = true;
                    }));
            addRenderableWidget(Button.builder(Component.literal("Tracer to this block: " + (e.tracer ? "On" : "Off")), b -> {
                e.tracer = !e.tracer;
                Tracker.dirty = true;
                rebuildWidgets();
            }).bounds(rx, py + 140, rw, 20).build());
            for (int i = 0; i < PRESETS.length; i++) {
                int preset = PRESETS[i];
                addRenderableWidget(Button.builder(Component.empty(), b -> {
                    e.color = preset;
                    Tracker.dirty = true;
                    rebuildWidgets();
                }).bounds(rx + i * 22, py + 166, 18, 14).build());
            }
        }
        addRenderableWidget(new IntSlider(rx, py + 202, rw, 20, "Range", 16, 128, module.range(), v -> {
            module.rangeSetting().set(v);
            Tracker.dirty = true;
        }));
        addRenderableWidget(new IntSlider(rx, py + 224, rw, 20, "Group distance", 0, 3, module.groupDistance(), v -> {
            module.groupSetting().set(v);
            Tracker.dirty = true;
        }));
        var tracers = module.tracersSetting();
        var tooltip = module.tooltipSetting();
        addRenderableWidget(Button.builder(Component.literal("Tracers: " + (tracers.get() ? "On" : "Off")), b -> {
            tracers.set(!tracers.get());
            rebuildWidgets();
        }).bounds(px + 10, py + 196, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tooltip: " + (tooltip.get() ? "On" : "Off")), b -> {
            tooltip.set(!tooltip.get());
            rebuildWidgets();
        }).bounds(px + 110, py + 196, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(px + 10, py + 244, 196, 20).build());
    }

    private void setColor(AdamConfig.BlockEntry e, int shift, int value) {
        e.color = (e.color & ~(0xFF << shift)) | (value << shift);
        Tracker.dirty = true;
    }

    // Paint our own background instead of the vanilla one, so nothing is drawn twice.
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    private void paintBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(px, py, px + PW, py + PH, PANEL);
        g.fill(px, py, px + 224, py + PH, SIDE);
        g.fill(px, py, px + PW, py + 1, LINE);
        g.fill(px, py + PH - 1, px + PW, py + PH, LINE);
        g.fill(px, py, px + 1, py + PH, LINE);
        g.fill(px + PW - 1, py, px + PW, py + PH, LINE);
        g.fill(px + 224, py, px + 225, py + PH, LINE);

        List<AdamConfig.BlockEntry> entries = AdamConfig.get().blocks;
        int start = page * ROWS;
        for (int r = 0; r < ROWS; r++) {
            int idx = start + r;
            if (idx >= entries.size()) break;
            if (idx == selected) g.fill(px + 4, py + 33 + r * 22, px + 220, py + 33 + r * 22 + 20, SELECT);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        paintBackground(g);
        super.render(g, mouseX, mouseY, delta);
        List<AdamConfig.BlockEntry> entries = AdamConfig.get().blocks;

        g.drawString(font, "Adam Blocks", px + 12, py + 13, 0xFFFFFFFF, true);
        g.drawString(font, "Tracked blocks", px + 12, py + 24, 0xFF9AA1AD, false);

        int start = page * ROWS;
        for (int r = 0; r < ROWS; r++) {
            int idx = start + r;
            if (idx >= entries.size()) break;
            AdamConfig.BlockEntry e = entries.get(idx);
            int y = py + 34 + r * 22;
            g.fill(px + 12, y + 4, px + 22, y + 14, 0xFF000000 | (e.color & 0xFFFFFF));
            g.drawString(font, font.plainSubstrByWidth(nameOf(e.id), 76), px + 26, y + 5,
                    e.enabled ? 0xFFFFFFFF : 0xFF7D8491, false);
        }
        if (entries.isEmpty()) {
            g.drawString(font, "Press + Add block to start", px + 12, py + 40, 0xFF9AA1AD, false);
        }
        int pages = Math.max(1, (entries.size() + ROWS - 1) / ROWS);
        g.drawString(font, (page + 1) + "/" + pages, px + 70 + 142, py + 34 + ROWS * 22 + 9 - 4,
                0xFF9AA1AD, false);

        int rx = px + 232;
        if (!entries.isEmpty()) {
            AdamConfig.BlockEntry e = entries.get(selected);
            g.fill(rx, py + 14, rx + 12, py + 26, 0xFF000000 | (e.color & 0xFFFFFF));
            g.drawString(font, font.plainSubstrByWidth(nameOf(e.id), 150), rx + 18, py + 17,
                    0xFFFFFFFF, false);
            g.drawString(font, "Color and opacity", rx, py + 40, 0xFF9AA1AD, false);
            for (int i = 0; i < PRESETS.length; i++) {
                int x = rx + i * 22, y = py + 166;
                g.fill(x + 2, y + 2, x + 16, y + 12, 0xFF000000 | PRESETS[i]);
            }
            g.drawString(font, "Quick colors", rx, py + 182, 0xFF9AA1AD, false);
        }
        g.drawString(font, "Settings", rx, py + 192, 0xFF9AA1AD, false);
    }

    private static String nameOf(String id) {
        Block b = Tracker.blockOf(id);
        return b == null ? id : b.getName().getString();
    }

    @Override
    public void removed() {
        AdamConfig.save();
        Tracker.dirty = true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min, max;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int value, IntConsumer onChange) {
            super(x, y, w, h, Component.literal(label + ": " + value), (value - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Component.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            if (onChange != null) onChange.accept(current());
        }
    }
}
