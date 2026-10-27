package com.example.pickupfilter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Config screen: a search box + a paged list of items. Click an item to allow/unallow it.
 * Built only from basic widgets (Button, EditBox) to stay stable across Minecraft versions.
 */
public class PickupFilterScreen extends Screen {
    private record Entry(Item item, ItemStack stack, String id, String name, String lower) {}

    private static final int LIST_TOP = 92;
    private static final int ROW_H = 22;

    private final Screen parent;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();

    private EditBox search;
    private Button enabledBtn, viewBtn, prevBtn, nextBtn;
    private boolean selectedOnly = false;
    private int page = 0;
    private int rows = 1;

    public PickupFilterScreen(Screen parent) {
        super(Component.literal("Pickup Filter"));
        this.parent = parent;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = item.getDefaultInstance();
            String name = stack.getHoverName().getString();
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            all.add(new Entry(item, stack, id, name, (name + " " + id).toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        enabledBtn = Button.builder(enabledText(), b -> {
            PickupFilterConfig.enabled = !PickupFilterConfig.enabled;
            b.setMessage(enabledText());
        }).bounds(cx - 155, 44, 150, 20).build();
        addRenderableWidget(enabledBtn);

        viewBtn = Button.builder(viewText(), b -> {
            selectedOnly = !selectedOnly;
            page = 0;
            b.setMessage(viewText());
            refresh();
        }).bounds(cx + 5, 44, 150, 20).build();
        addRenderableWidget(viewBtn);

        String old = search == null ? "" : search.getValue();
        search = new EditBox(this.font, cx - 155, 68, 310, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search items (name or id)...").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(old);
        search.setResponder(s -> {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);

        rows = Math.max(1, (this.height - 58 - LIST_TOP) / ROW_H);
        rowButtons.clear();
        for (int i = 0; i < rows; i++) {
            final int idx = i;
            Button b = Button.builder(Component.empty(), btn -> toggleRow(idx))
                    .bounds(cx - 155, LIST_TOP + i * ROW_H, 310, 20).build();
            rowButtons.add(b);
            addRenderableWidget(b);
        }

        prevBtn = Button.builder(Component.literal("< Prev"), b -> {
            page--;
            updateRows();
        }).bounds(cx - 155, this.height - 54, 70, 20).build();
        addRenderableWidget(prevBtn);

        nextBtn = Button.builder(Component.literal("Next >"), b -> {
            page++;
            updateRows();
        }).bounds(cx + 85, this.height - 54, 70, 20).build();
        addRenderableWidget(nextBtn);

        addRenderableWidget(Button.builder(Component.literal("Clear all"), b -> {
            PickupFilterConfig.allowed.clear();
            refresh();
        }).bounds(cx - 155, this.height - 28, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 55, this.height - 28, 100, 20).build());

        refresh();
    }

    private Component enabledText() {
        return PickupFilterConfig.enabled
                ? Component.literal("Filter: ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("Filter: OFF").withStyle(ChatFormatting.RED);
    }

    private Component viewText() {
        return Component.literal(selectedOnly ? "Showing: Allowed only" : "Showing: All items");
    }

    private boolean isAllowed(Entry e) {
        return PickupFilterConfig.allowed.contains(e.id());
    }

    /** Re-filters and re-sorts the list (allowed items first), then redraws the rows. */
    private void refresh() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        shown.clear();
        for (Entry e : all) {
            if (selectedOnly && !isAllowed(e)) continue;
            if (!q.isEmpty() && !e.lower().contains(q)) continue;
            shown.add(e);
        }
        shown.sort((a, b) -> Boolean.compare(isAllowed(b), isAllowed(a)));
        updateRows();
    }

    private void toggleRow(int i) {
        int idx = page * rows + i;
        if (idx >= shown.size()) return;
        String id = shown.get(idx).id();
        if (!PickupFilterConfig.allowed.remove(id)) PickupFilterConfig.allowed.add(id);
        updateRows();
    }

    private void updateRows() {
        int pages = Math.max(1, (shown.size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < rowButtons.size(); i++) {
            Button b = rowButtons.get(i);
            int idx = page * rows + i;
            if (idx < shown.size()) {
                Entry e = shown.get(idx);
                b.visible = true;
                b.setMessage(isAllowed(e)
                        ? Component.literal(e.name() + "  -  ALLOWED").withStyle(ChatFormatting.GREEN)
                        : Component.literal(e.name()));
            } else {
                b.visible = false;
            }
        }
        prevBtn.active = page > 0;
        nextBtn.active = page < pages - 1;
    }

    private void centered(GuiGraphicsExtractor g, String s, int y, int color) {
        g.text(this.font, s, this.width / 2 - this.font.width(s) / 2, y, color, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;

        centered(graphics, "Pickup Filter", 6, 0xFFFFFFFF);
        centered(graphics, "1) Click items below to mark them ALLOWED.", 18, 0xFFAAAAAA);
        centered(graphics, "2) Turn the filter ON. Only allowed items get picked up; the rest stay on the ground.", 29, 0xFFAAAAAA);

        // item icons on top of the row buttons
        for (int i = 0; i < rowButtons.size(); i++) {
            int idx = page * rows + i;
            if (rowButtons.get(i).visible && idx < shown.size()) {
                graphics.item(shown.get(idx).stack(), cx - 151, LIST_TOP + i * ROW_H + 2);
            }
        }

        int pages = Math.max(1, (shown.size() + rows - 1) / rows);
        centered(graphics, "Page " + (page + 1) + "/" + pages + "  |  "
                + PickupFilterConfig.allowed.size() + " allowed", this.height - 48, 0xFFFFFF55);
    }

    @Override
    public void onClose() {
        PickupFilterConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}
