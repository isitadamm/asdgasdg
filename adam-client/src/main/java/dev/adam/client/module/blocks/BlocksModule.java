package dev.adam.client.module.blocks;

import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Adam Blocks: highlight chosen blocks through walls, with grouped counts on hover. */
public class BlocksModule extends Module {

    public static BlocksModule INSTANCE;

    private final IntSetting range = addSetting("range", "Range", 16, 128, 48);
    private final IntSetting group = addSetting("group", "Group distance", 0, 3, 1);
    private final BoolSetting tracers = addToggle("tracers", "Tracers", true);
    private final BoolSetting tooltip = addToggle("tooltip", "Hover tooltip", true);

    public BlocksModule() {
        super("blocks", "Adam Blocks",
                "Boxes (and optional tracers) on the blocks you choose, through walls.",
                Category.BLOCKS, -1);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public IntSetting rangeSetting() {
        return range;
    }

    public IntSetting groupSetting() {
        return group;
    }

    public BoolSetting tracersSetting() {
        return tracers;
    }

    public BoolSetting tooltipSetting() {
        return tooltip;
    }

    public boolean tracersOn() {
        return tracers.get();
    }

    public boolean tooltipOn() {
        return tooltip.get();
    }

    public int range() {
        return range.get();
    }

    public int groupDistance() {
        return group.get();
    }

    @Override
    protected boolean persistsEnabled() {
        return true;
    }

    @Override
    protected void onEnable(Minecraft mc) {
        Tracker.dirty = true;
    }

    @Override
    protected void onDisable(Minecraft mc) {
        Tracker.clear();
    }

    @Override
    public void onEndTick(Minecraft mc) {
        Tracker.tick(mc);
    }

    @Override
    public void onSettingChanged(IntSetting setting) {
        Tracker.dirty = true;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new BlocksScreen(parent);
    }
}
