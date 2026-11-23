package dev.adam.client.module;

import dev.adam.client.config.AdamConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Base class for everything that shows up as a card in the Adam menu. */
public abstract class Module {

    /** An integer slider that is saved in the config. */
    public static final class IntSetting {
        public final String key;
        public final String label;
        public final int min;
        public final int max;
        public final int def;
        /** Shown value = stored value / divisor (so 40 with divisor 10 reads as 4.0). */
        public final int divisor;
        /** If set, the slider picks one of these names instead of a number. */
        public final String[] labels;

        IntSetting(String fullKey, String label, int min, int max, int def, int divisor, String[] labels) {
            this.key = fullKey;
            this.label = label;
            this.min = min;
            this.max = max;
            this.def = def;
            this.divisor = divisor;
            this.labels = labels;
        }

        public String format(int value) {
            if (labels != null) return labels[Math.max(0, Math.min(labels.length - 1, value))];
            if (divisor == 1) return String.valueOf(value);
            return String.format(java.util.Locale.ROOT, "%.1f", value / (double) divisor);
        }

        public int get() {
            int v = AdamConfig.get().getInt(key, def);
            return Math.max(min, Math.min(max, v));
        }

        public void set(int value) {
            AdamConfig.get().setInt(key, Math.max(min, Math.min(max, value)));
        }
    }

    /** An on/off switch that is saved in the config. */
    public static final class BoolSetting {
        public final String key;
        public final String label;
        public final boolean def;

        BoolSetting(String fullKey, String label, boolean def) {
            this.key = fullKey;
            this.label = label;
            this.def = def;
        }

        public boolean get() {
            return AdamConfig.get().getInt(key, def ? 1 : 0) != 0;
        }

        public void set(boolean value) {
            AdamConfig.get().setInt(key, value ? 1 : 0);
        }
    }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final int defaultKey;

    private final List<IntSetting> settings = new ArrayList<>();
    private final List<BoolSetting> toggles = new ArrayList<>();
    private KeyMapping keyMapping;
    private boolean enabled;

    protected Module(String id, String name, String description, Category category, int defaultKey) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultKey = defaultKey;
    }

    protected IntSetting addSetting(String key, String label, int min, int max, int def) {
        return addSetting(key, label, min, max, def, 1);
    }

    protected IntSetting addSetting(String key, String label, int min, int max, int def, int divisor) {
        IntSetting s = new IntSetting(id + "." + key, label, min, max, def, divisor, null);
        settings.add(s);
        return s;
    }

    /** A slider that picks one of several named options (stored as 0, 1, 2...). */
    protected IntSetting addChoice(String key, String label, int def, String... options) {
        IntSetting s = new IntSetting(id + "." + key, label, 0, options.length - 1, def, 1, options);
        settings.add(s);
        return s;
    }

    protected BoolSetting addToggle(String key, String label, boolean def) {
        BoolSetting t = new BoolSetting(id + "." + key, label, def);
        toggles.add(t);
        return t;
    }

    public List<BoolSetting> toggles() {
        return toggles;
    }

    public List<IntSetting> settings() {
        return settings;
    }

    public KeyMapping keyMapping() {
        return keyMapping;
    }

    void setKeyMapping(KeyMapping keyMapping) {
        this.keyMapping = keyMapping;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (value) {
            if (!canEnable(mc)) return;
            enabled = true;
            onEnable(mc);
        } else {
            enabled = false;
            onDisable(mc);
        }
        if (persistsEnabled()) {
            AdamConfig.get().enabled.put(id, enabled);
            AdamConfig.save();
        }
    }

    /** Called once at startup so modules that remember their state come back on. */
    void restore() {
        if (persistsEnabled() && AdamConfig.get().enabled.getOrDefault(id, defaultEnabled())) {
            enabled = true;
            onEnable(Minecraft.getInstance());
        }
    }

    /** Whether this module stays on between game launches. */
    protected boolean persistsEnabled() {
        return false;
    }

    /** Whether a persistent module is on the very first time (before anything is saved). */
    protected boolean defaultEnabled() {
        return false;
    }

    /** Return false to refuse being turned on (for example when you are not in a world). */
    protected boolean canEnable(Minecraft mc) {
        return true;
    }

    protected void onEnable(Minecraft mc) {
    }

    protected void onDisable(Minecraft mc) {
    }

    /** Called at the start of every client tick while enabled. */
    public void onStartTick(Minecraft mc) {
    }

    /** Called at the end of every client tick while enabled. */
    public void onEndTick(Minecraft mc) {
    }

    /** Called when one of this module's sliders changes. */
    public void onSettingChanged(IntSetting setting) {
    }

    /** True if this module has its own detailed screen (shows a "Configure module" button). */
    public boolean hasConfigScreen() {
        return false;
    }

    /** The detailed screen opened by the "Configure module" button. */
    public Screen createConfigScreen(Screen parent) {
        return null;
    }
}
