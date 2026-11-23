package dev.adam.client.module.blocks;

import dev.adam.client.config.AdamConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Search every block in the game and add one to the tracked list. */
public class BlockPickerScreen extends Screen {
    private static final int COLS = 2, ROWS = 7, BW = 190, BH = 20;

    private final BlocksScreen parent;
    private String filter = "";
    private int page = 0;
    private final List<Block> shown = new ArrayList<>();
    private final List<int[]> slots = new ArrayList<>();

    public BlockPickerScreen(BlocksScreen parent) {
        super(Component.literal("Choose a block"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        shown.clear();
        slots.clear();
        Set<String> tracked = new HashSet<>();
        for (AdamConfig.BlockEntry e : AdamConfig.get().blocks) tracked.add(e.id);

        String f = filter.toLowerCase(Locale.ROOT).trim();
        List<Block> all = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            if (b.defaultBlockState().isAir()) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (tracked.contains(id.toString())) continue;
            if (!f.isEmpty()) {
                String name = b.getName().getString().toLowerCase(Locale.ROOT);
                if (!name.contains(f) && !id.getPath().contains(f)) continue;
            }
            all.add(b);
        }
        all.sort((a, b) -> a.getName().getString().compareToIgnoreCase(b.getName().getString()));

        int perPage = COLS * ROWS;
        int maxPage = Math.max(0, (all.size() - 1) / perPage);
        if (page > maxPage) page = maxPage;

        int gridW = COLS * BW + 4;
        int left = (width - gridW) / 2;

        EditBox search = new EditBox(font, left, 22, gridW, 20, Component.literal("Search"));
        search.setValue(filter);
        search.setHint(Component.literal("Search blocks"));
        search.setResponder(s -> {
            filter = s;
            page = 0;
            rebuildWidgets();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            Block block = all.get(idx);
            int col = i % COLS, row = i / COLS;
            int x = left + col * (BW + 4);
            int y = 48 + row * 22;
            shown.add(block);
            slots.add(new int[]{x, y});
            String label = font.plainSubstrByWidth(block.getName().getString(), BW - 50);
            addRenderableWidget(Button.builder(Component.literal(label), b -> pick(block))
                    .bounds(x, y, BW, BH).build());
        }

        int navY = 48 + ROWS * 22 + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) page--;
            rebuildWidgets();
        }).bounds(left, navY, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (page < maxPage) page++;
            rebuildWidgets();
        }).bounds(left + 34, navY, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + gridW - 100, navY, 100, 20).build());
    }

    private void pick(Block block) {
        AdamConfig cfg = AdamConfig.get();
        int color = AdamConfig.PALETTE[cfg.blocks.size() % AdamConfig.PALETTE.length];
        cfg.blocks.add(new AdamConfig.BlockEntry(BuiltInRegistries.BLOCK.getKey(block).toString(), color, true));
        AdamConfig.save();
        Tracker.dirty = true;
        parent.select(cfg.blocks.size() - 1);
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, "Choose a block to track", width / 2, 8, 0xFFFFFFFF);
        for (int i = 0; i < shown.size(); i++) {
            Item item = shown.get(i).asItem();
            if (item == Items.AIR) continue;
            int[] s = slots.get(i);
            g.renderItem(new ItemStack(item), s[0] + 2, s[1] + 2);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
