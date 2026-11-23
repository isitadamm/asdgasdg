package dev.adam.client.gui;

import dev.adam.client.AdamClient;
import dev.adam.client.config.AdamConfig;
import dev.adam.client.module.Category;
import dev.adam.client.module.Module;
import dev.adam.client.module.ModuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Adam module menu: workspace sidebar, module cards, and a preview panel with settings.
 *
 * Input is handled by plain vanilla buttons and sliders; this class paints the dark
 * "module desk" look over them.
 */
public class AdamScreen extends Screen {

    // Colors (ARGB)
    private static final int DIM = 0x99000000;
    private static final int PANEL = 0xF214161B;
    private static final int HEADER = 0xFF171A20;
    private static final int SURFACE = 0xFF191C22;
    private static final int CARD = 0xFF1D2026;
    private static final int CARD_HOVER = 0xFF242830;
    private static final int CARD_SELECTED = 0xFF272D22;
    private static final int LINE = 0xFF2B2F37;
    private static final int LINE_HOVER = 0xFF48505C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA1AD;
    private static final int FAINT = 0xFF6F7682;
    private static final int ACCENT = 0xFFD7E339;
    private static final int ACCENT_DARK = 0xFF6F7A1A;
    private static final int BLUE = 0xFF5CC8FF;
    private static final int TRACK_ON = 0xFFC8D43A;
    private static final int TRACK_OFF = 0xFF5B4636;
    private static final int KNOB = 0xFFE8E8EE;
    private static final int SLIDER_BG = 0xFF111317;

    private record Rect(int x, int y, int w, int h) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record CardView(Module module, Rect rect) {
    }

    private record SideItem(Category category, String label, Rect rect) {
    }

    private record BoolView(Module.BoolSetting setting, Rect rect) {
    }

    // State that survives rebuildWidgets()
    private Category filter = null;
    private boolean enabledOnly = false;
    private String searchText = "";
    private Module selected;
    private int page = 0;

    // Layout, recomputed in init()
    private int px, py, pw, ph;
    private int bodyTop, bodyBottom;
    private int sideX, sideW, centerX, centerW, previewX, previewW;
    private int pageCount = 1;

    private final List<CardView> cards = new ArrayList<>();
    private final List<SideItem> sideItems = new ArrayList<>();
    private final List<PanelSlider> sliders = new ArrayList<>();
    private final List<BoolView> toggleViews = new ArrayList<>();
    private List<FormattedCharSequence> descLines = List.of();
    private Rect closeRect, enabledOnlyRect, configRect, prevRect, nextRect;

    public AdamScreen() {
        super(Component.literal("Adam"));
        List<Module> all = ModuleManager.all();
        if (!all.isEmpty()) selected = all.get(0);
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        String q = searchText.toLowerCase(Locale.ROOT).trim();
        for (Module m : ModuleManager.all()) {
            if (filter != null && m.category != filter) continue;
            if (enabledOnly && !m.isEnabled()) continue;
            if (!q.isEmpty()
                    && !m.name.toLowerCase(Locale.ROOT).contains(q)
                    && !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    @Override
    protected void init() {
        cards.clear();
        sideItems.clear();
        sliders.clear();
        toggleViews.clear();
        closeRect = enabledOnlyRect = configRect = prevRect = nextRect = null;

        final int pad = 6, headerH = 28, footerH = 16;
        pw = Math.min(520, width - 12);
        ph = Math.min(284, height - 12);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + headerH + pad;
        bodyBottom = py + ph - footerH - pad;
        sideX = px + pad;
        sideW = 92;
        previewW = 150;
        previewX = px + pw - pad - previewW;
        centerX = sideX + sideW + pad;
        centerW = previewX - pad - centerX;

        // Close button
        closeRect = new Rect(px + pw - pad - 14, py + 7, 14, 14);
        addRenderableWidget(Button.builder(Component.empty(), b -> onClose())
                .bounds(closeRect.x, closeRect.y, closeRect.w, closeRect.h).build());

        // Sidebar: All modules + one item per category
        Category[] cats = {null, Category.BLOCKS, Category.CAM, Category.WINGS, Category.SWORD, Category.PROTECTION};
        for (int i = 0; i < cats.length; i++) {
            final Category cat = cats[i];
            Rect r = new Rect(sideX + 4, bodyTop + 22 + i * 22, sideW - 8, 20);
            sideItems.add(new SideItem(cat, cat == null ? "All modules" : cat.label, r));
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                filter = cat;
                page = 0;
                rebuildWidgets();
            }).bounds(r.x, r.y, r.w, r.h).build());
        }
        enabledOnlyRect = new Rect(sideX + 4, bodyBottom - 22, sideW - 8, 18);
        addRenderableWidget(Button.builder(Component.empty(), b -> {
            enabledOnly = !enabledOnly;
            page = 0;
            rebuildWidgets();
        }).bounds(enabledOnlyRect.x, enabledOnlyRect.y, enabledOnlyRect.w, enabledOnlyRect.h).build());

        // Search box
        int searchY = bodyTop + 46;
        EditBox search = new EditBox(font, centerX, searchY, centerW, 18, Component.literal("Search"));
        search.setValue(searchText);
        search.setHint(Component.literal("Search modules..."));
        search.setResponder(s -> {
            searchText = s;
            page = 0;
            rebuildWidgets();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        // Module cards
        List<Module> list = visibleModules();
        int cardH = 30, gap = 4;
        int cardW = (centerW - gap) / 2;
        int cardsTop = searchY + 18 + 6;
        int rows = Math.max(1, (bodyBottom - cardsTop + gap) / (cardH + gap));
        boolean paging = list.size() > rows * 2;
        if (paging) rows = Math.max(1, rows - 1);
        int perPage = rows * 2;
        pageCount = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page >= pageCount) page = pageCount - 1;

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            Module m = list.get(idx);
            int x = centerX + (i % 2) * (cardW + gap);
            int y = cardsTop + (i / 2) * (cardH + gap);
            Rect card = new Rect(x, y, cardW, cardH);
            cards.add(new CardView(m, card));
            // The toggle goes in first so it wins clicks over the card underneath it.
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                m.toggle();
                if (enabledOnly) rebuildWidgets();
            }).bounds(x + cardW - 34, y + 4, 30, cardH - 8).build());
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                selected = m;
                rebuildWidgets();
            }).bounds(x, y, cardW, cardH).build());
        }
        if (paging) {
            int navY = bodyBottom - 16;
            prevRect = new Rect(centerX, navY, 24, 16);
            nextRect = new Rect(centerX + 28, navY, 24, 16);
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (page > 0) page--;
                rebuildWidgets();
            }).bounds(prevRect.x, prevRect.y, prevRect.w, prevRect.h).build());
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (page < pageCount - 1) page++;
                rebuildWidgets();
            }).bounds(nextRect.x, nextRect.y, nextRect.w, nextRect.h).build());
        }

        // Preview panel: description, sliders, switches, configure button
        descLines = List.of();
        if (selected != null) {
            List<FormattedCharSequence> lines = font.split(Component.literal(selected.description), previewW - 16);
            descLines = lines.size() > 4 ? lines.subList(0, 4) : lines;
            int limit = selected.hasConfigScreen() ? bodyBottom - 40 : bodyBottom - 18;
            int sy = bodyTop + 52 + descLines.size() * 10 + 6;
            for (Module.IntSetting setting : selected.settings()) {
                if (sy + 14 > limit) break;
                PanelSlider slider = new PanelSlider(previewX + 8, sy, previewW - 16, 14, selected, setting);
                sliders.add(slider);
                addRenderableWidget(slider);
                sy += 17;
            }
            for (Module.BoolSetting toggle : selected.toggles()) {
                if (sy + 13 > limit) break;
                Rect r = new Rect(previewX + 8, sy, previewW - 16, 13);
                toggleViews.add(new BoolView(toggle, r));
                addRenderableWidget(Button.builder(Component.empty(), b -> toggle.set(!toggle.get()))
                        .bounds(r.x, r.y, r.w, r.h).build());
                sy += 16;
            }
            if (selected.hasConfigScreen()) {
                configRect = new Rect(previewX + 8, bodyBottom - 36, previewW - 16, 18);
                final Module m = selected;
                addRenderableWidget(Button.builder(Component.empty(), b -> {
                    Screen next = m.createConfigScreen(this);
                    if (next != null) minecraft.setScreen(next);
                }).bounds(configRect.x, configRect.y, configRect.w, configRect.h).build());
            }
        }
    }

    // We paint the whole backdrop ourselves in render(), so the vanilla one must do nothing.
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        paintBackdrop(g);
        super.render(g, mouseX, mouseY, delta);
        paintOverWidgets(g, mouseX, mouseY);
    }

    // ---------------------------------------------------------------- painting helpers

    private void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    private void bold(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, Component.literal(s).withStyle(ChatFormatting.BOLD), x, y, color, false);
    }

    private void toggleSwitch(GuiGraphics g, int x, int y, boolean on) {
        g.fill(x, y, x + 22, y + 10, on ? TRACK_ON : TRACK_OFF);
        g.fill(on ? x + 14 : x + 2, y + 2, on ? x + 20 : x + 8, y + 8, KNOB);
    }

    // ---------------------------------------------------------------- layers

    /** Everything that sits behind the widgets. */
    private void paintBackdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, DIM);
        box(g, px, py, pw, ph, PANEL, LINE);

        // Header
        g.fill(px + 1, py + 1, px + pw - 1, py + 28, HEADER);
        g.fill(px + 1, py + 28, px + pw - 1, py + 29, LINE);
        bold(g, "ADAM", px + 12, py + 6, BLUE);
        text(g, "MODULE CONTROL", px + 12, py + 16, MUTED);
        // "Modules" tab (the only tab for now)
        int tabW = font.width("Modules") + 16;
        int tabX = px + pw - 6 - 14 - 10 - tabW;
        g.fill(tabX, py + 5, tabX + tabW, py + 23, 0xFF22281A);
        g.fill(tabX + 4, py + 22, tabX + tabW - 4, py + 23, ACCENT);
        text(g, "Modules", tabX + 8, py + 10, TEXT);

        // Sidebar
        box(g, sideX, bodyTop, sideW, bodyBottom - bodyTop, SURFACE, LINE);
        text(g, "WORKSPACE", sideX + 8, bodyTop + 8, BLUE);

        // Hero card
        box(g, centerX, bodyTop, centerW, 40, SURFACE, LINE);
        text(g, "YOUR MODULE DESK", centerX + 8, bodyTop + 6, ACCENT);
        bold(g, filter == null ? "All modules" : filter.label, centerX + 8, bodyTop + 17, TEXT);
        text(g, font.plainSubstrByWidth(
                        filter == null ? "Hover to explore. Select to configure." : filter.blurb, centerW - 16),
                centerX + 8, bodyTop + 28, MUTED);

        // Preview panel
        box(g, previewX, bodyTop, previewW, bodyBottom - bodyTop, SURFACE, LINE);
        text(g, "MODULE PREVIEW", previewX + 8, bodyTop + 8, MUTED);
        if (selected != null) {
            bold(g, font.plainSubstrByWidth(selected.name, previewW - 16), previewX + 8, bodyTop + 22, TEXT);
            text(g, selected.category.label, previewX + 8, bodyTop + 33, MUTED);
            int dy = bodyTop + 52;
            for (FormattedCharSequence line : descLines) {
                g.drawString(font, line, previewX + 8, dy, TEXT, false);
                dy += 10;
            }
            String key = selected.keyMapping() == null ? "-"
                    : selected.keyMapping().getTranslatedKeyMessage().getString();
            text(g, font.plainSubstrByWidth("Key: " + key, previewW - 16), previewX + 8, bodyBottom - 14, FAINT);
        }

        // Footer
        text(g, "ADAM  /  YOUR WORKSPACE", px + 12, py + ph - 14, FAINT);
        String active = ModuleManager.activeCount() + " modules active";
        text(g, active, px + pw - 12 - font.width(active), py + ph - 14, FAINT);
        String menuKey = AdamClient.menuKey() == null ? ""
                : "Menu key: " + AdamClient.menuKey().getTranslatedKeyMessage().getString();
        text(g, menuKey, px + (pw - font.width(menuKey)) / 2, py + ph - 14, FAINT);
    }

    /** Everything that has to cover the plain vanilla button and slider textures. */
    private void paintOverWidgets(GuiGraphics g, int mx, int my) {
        // Close "x"
        boolean closeHover = closeRect.contains(mx, my);
        g.fill(closeRect.x, closeRect.y, closeRect.x + closeRect.w, closeRect.y + closeRect.h,
                closeHover ? 0xFF2B2F37 : HEADER);
        text(g, "x", closeRect.x + 4, closeRect.y + 3, closeHover ? TEXT : MUTED);

        // Sidebar items
        for (SideItem item : sideItems) {
            Rect r = item.rect;
            boolean on = item.category == filter;
            boolean hover = r.contains(mx, my);
            g.fill(r.x, r.y, r.x + r.w, r.y + r.h, on ? 0xFF262B22 : hover ? CARD_HOVER : SURFACE);
            if (on) g.fill(r.x, r.y + 2, r.x + 2, r.y + r.h - 2, ACCENT);
            text(g, item.label, r.x + 10, r.y + 6, on ? TEXT : MUTED);
        }
        // "Enabled only"
        Rect eo = enabledOnlyRect;
        boolean eoHover = eo.contains(mx, my);
        g.fill(eo.x, eo.y, eo.x + eo.w, eo.y + eo.h, eoHover ? CARD_HOVER : SURFACE);
        g.fill(eo.x + 4, eo.y + 6, eo.x + 8, eo.y + 10, enabledOnly ? ACCENT : FAINT);
        text(g, "Enabled only", eo.x + 14, eo.y + 5, enabledOnly ? TEXT : MUTED);

        // Module cards
        for (CardView c : cards) {
            Rect r = c.rect;
            Module m = c.module;
            boolean sel = m == selected;
            boolean hover = r.contains(mx, my);
            box(g, r.x, r.y, r.w, r.h,
                    sel ? CARD_SELECTED : hover ? CARD_HOVER : CARD,
                    sel ? ACCENT_DARK : hover ? LINE_HOVER : LINE);
            text(g, font.plainSubstrByWidth(m.name, r.w - 48), r.x + 8, r.y + 6, TEXT);
            text(g, m.isEnabled() ? "ACTIVE" : "INACTIVE", r.x + 8, r.y + 17,
                    m.isEnabled() ? ACCENT : MUTED);
            toggleSwitch(g, r.x + r.w - 30, r.y + (r.h - 10) / 2, m.isEnabled());
        }
        if (cards.isEmpty()) {
            text(g, "No modules match.", centerX + 8, bodyTop + 80, MUTED);
        }

        // Page arrows
        if (prevRect != null) {
            arrow(g, prevRect, "<", mx, my);
            arrow(g, nextRect, ">", mx, my);
            text(g, (page + 1) + "/" + pageCount, nextRect.x + nextRect.w + 8, nextRect.y + 4, MUTED);
        }

        // Sliders in the preview panel
        for (PanelSlider s : sliders) {
            int x = s.getX(), y = s.getY(), w = s.getWidth(), h = s.getHeight();
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
            box(g, x, y, w, h, SLIDER_BG, hover ? LINE_HOVER : LINE);
            int fillW = (int) Math.round((w - 2) * s.ratio());
            if (fillW > 0) g.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, ACCENT_DARK);
            g.fill(Math.min(x + w - 3, x + fillW), y + 1, Math.min(x + w - 1, x + fillW + 2), y + h - 1, ACCENT);
            String label = s.getMessage().getString();
            g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, TEXT, false);
        }

        // On/off switches in the preview panel
        for (BoolView v : toggleViews) {
            Rect r = v.rect;
            boolean hover = r.contains(mx, my);
            g.fill(r.x, r.y, r.x + r.w, r.y + r.h, hover ? CARD_HOVER : SURFACE);
            text(g, font.plainSubstrByWidth(v.setting.label, r.w - 30), r.x + 2, r.y + 3, TEXT);
            toggleSwitch(g, r.x + r.w - 24, r.y + 1, v.setting.get());
        }

        // Configure button
        if (configRect != null) {
            boolean hover = configRect.contains(mx, my);
            box(g, configRect.x, configRect.y, configRect.w, configRect.h,
                    hover ? CARD_HOVER : CARD, hover ? LINE_HOVER : LINE);
            String label = "Configure module";
            text(g, label, configRect.x + (configRect.w - font.width(label)) / 2, configRect.y + 5, TEXT);
        }
    }

    private void arrow(GuiGraphics g, Rect r, String label, int mx, int my) {
        boolean hover = r.contains(mx, my);
        box(g, r.x, r.y, r.w, r.h, hover ? CARD_HOVER : CARD, hover ? LINE_HOVER : LINE);
        text(g, label, r.x + (r.w - font.width(label)) / 2, r.y + 4, TEXT);
    }

    @Override
    public void removed() {
        AdamConfig.save();
    }

    /** A vanilla slider (it handles the mouse), repainted by AdamScreen to match the menu. */
    private static final class PanelSlider extends AbstractSliderButton {
        private final Module module;
        private final Module.IntSetting setting;

        PanelSlider(int x, int y, int w, int h, Module module, Module.IntSetting setting) {
            super(x, y, w, h, Component.literal(setting.label + ": " + setting.format(setting.get())),
                    (setting.get() - setting.min) / (double) (setting.max - setting.min));
            this.module = module;
            this.setting = setting;
        }

        double ratio() {
            return value;
        }

        private int current() {
            return setting.min + (int) Math.round(value * (setting.max - setting.min));
        }

        @Override
        protected void updateMessage() {
            if (setting == null) return;
            setMessage(Component.literal(setting.label + ": " + setting.format(current())));
        }

        @Override
        protected void applyValue() {
            if (setting == null) return;
            setting.set(current());
            module.onSettingChanged(setting);
        }
    }
}
