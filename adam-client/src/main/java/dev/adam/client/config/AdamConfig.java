package dev.adam.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One config file (config/adam.json) for the whole client. */
public final class AdamConfig {
    public static final int[] PALETTE = {0xF0A030, 0x35D0E0, 0xB36BFF, 0x4BE3B0, 0xFF5A6E, 0x3D8BFF, 0xE8E85A};

    /** One tracked block in the Blocks module. */
    public static final class BlockEntry {
        public String id;
        public boolean enabled = true;
        public int color = 0xF0A030;
        public int fill = 35;
        /** Draw a line from your crosshair to this block. */
        public boolean tracer = true;

        public BlockEntry() {
        }

        public BlockEntry(String id, int color, boolean enabled) {
            this.id = id;
            this.color = color;
            this.enabled = enabled;
        }
    }

    /** Saved on/off state, only for modules that remember it (Blocks). */
    public Map<String, Boolean> enabled = new HashMap<>();
    /** Slider values, keyed "moduleId.settingKey". */
    public Map<String, Integer> values = new HashMap<>();
    /** The Blocks module's tracked block list. */
    public List<BlockEntry> blocks = new ArrayList<>();
    /** Player names protected by Friend Protection (compared ignoring case). */
    public List<String> friends = new ArrayList<>();

    private static AdamConfig inst = new AdamConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static AdamConfig get() {
        return inst;
    }

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void load() {
        Path p = dir().resolve("adam.json");
        try {
            if (Files.exists(p)) {
                AdamConfig c = GSON.fromJson(Files.readString(p), AdamConfig.class);
                if (c != null) {
                    if (c.enabled == null) c.enabled = new HashMap<>();
                    if (c.values == null) c.values = new HashMap<>();
                    if (c.blocks == null) c.blocks = new ArrayList<>();
                    if (c.friends == null) c.friends = new ArrayList<>();
                    inst = c;
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        inst = defaults();
        importOldBlocksConfig(inst);
        save();
    }

    public static void save() {
        try {
            Files.writeString(dir().resolve("adam.json"), GSON.toJson(inst));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getInt(String key, int def) {
        Integer v = values.get(key);
        return v == null ? def : v;
    }

    public void setInt(String key, int value) {
        values.put(key, value);
    }

    private static AdamConfig defaults() {
        AdamConfig c = new AdamConfig();
        c.blocks.add(new BlockEntry("minecraft:chest", PALETTE[0], true));
        c.blocks.add(new BlockEntry("minecraft:hopper", PALETTE[1], true));
        c.blocks.add(new BlockEntry("minecraft:spawner", PALETTE[2], true));
        c.blocks.add(new BlockEntry("minecraft:diamond_ore", PALETTE[3], false));
        return c;
    }

    /** If you used the standalone Adam Blocks mod, bring its block list over. */
    private static void importOldBlocksConfig(AdamConfig c) {
        try {
            Path old = dir().resolve("adamblocks.json");
            if (!Files.exists(old)) return;
            JsonObject o = JsonParser.parseString(Files.readString(old)).getAsJsonObject();
            if (o.has("range")) c.values.put("blocks.range", o.get("range").getAsInt());
            if (o.has("groupDistance")) c.values.put("blocks.group", o.get("groupDistance").getAsInt());
            if (o.has("master")) c.enabled.put("blocks", o.get("master").getAsBoolean());
            if (o.has("entries")) {
                List<BlockEntry> list = new ArrayList<>();
                for (JsonElement el : o.getAsJsonArray("entries")) {
                    BlockEntry e = GSON.fromJson(el, BlockEntry.class);
                    if (e != null && e.id != null) list.add(e);
                }
                if (!list.isEmpty()) c.blocks = list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
