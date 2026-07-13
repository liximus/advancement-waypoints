package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.DarkModeChecker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {

    protected static Identifier BG = DarkModeChecker.isDarkModeEnabled()
            ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png")
            : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static Identifier SLOTS = DarkModeChecker.isDarkModeEnabled()
            ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/slotsdark.png")
            : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/slots.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final int CELL = 18, SCROLLBAR_WIDTH = 12, SCROLLBAR_HANDLE_HEIGHT = 15;

    private static final Set<String> FUNCTIONAL_BLOCK_IDS = Set.of(
            "crafting_table", "smithing_table", "cartography_table", "fletching_table",
            "tnt", "note_block", "observer",
            "piston", "sticky_piston",
            "target", "redstone_lamp", "redstone_block"
    );

    public static void setDarkMode(boolean darkMode) {
        BG = darkMode
                ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png")
                : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
        SLOTS = darkMode
                ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/slotsdark.png")
                : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/slots.png");
    }

    private final Screen parent;
    private final Consumer<Item> callback;
    private final boolean blocksOnly;

    private EditBox searchField;
    private List<Item> allItems, filtered;
    private int scrollRow;
    private float scrollProgress;
    private boolean dragging;
    private int cols, panelX, panelY, panelW, panelH;

    public ItemPickerScreen(Screen parent, Consumer<Item> callback) {
        this(parent, callback, false);
    }

    public ItemPickerScreen(Screen parent, Consumer<Item> callback, boolean blocksOnly) {
        super(Component.translatable(blocksOnly ? "advwp.picker.background.title" : "advwp.picker.icon.title"));
        this.parent = parent;
        this.callback = callback;
        this.blocksOnly = blocksOnly;
    }

    @Override
    protected void init() {
        allItems = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item == Items.AIR) return;
            if (blocksOnly) {
                if (!(item instanceof BlockItem blockItem)) return;
                Block block = blockItem.getBlock();
                if (!isFullBlock(block)) return;
                if (isFunctionalBlock(block)) return;
            }
            allItems.add(item);
        });

        panelW = Math.min(width - 20, 360);
        panelH = Math.min(height - 20, 280);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        cols = Math.max(1, (panelW - 20 - SCROLLBAR_WIDTH) / CELL);

        int searchWidth = Math.min(panelW - 20, 200);
        searchField = addRenderableWidget(new EditBox(font, panelX + (panelW - searchWidth) / 2, panelY + 18, searchWidth, 16, Component.literal("")));
        searchField.setHint(Component.translatable(blocksOnly ? "advwp.hint.search.background" : "advwp.hint.search.item"));
        searchField.setResponder(t -> {
            filter();
            scrollRow = 0;
            scrollProgress = 0;
        });
        setInitialFocus(searchField);
        filter();
        scrollRow = 0;
        scrollProgress = 0;
    }

    private boolean isFullBlock(Block block) {
        try {
            return block.defaultBlockState().isCollisionShapeFullBlock(null, null);
        } catch (Exception e) {
            try {
                var shape = block.defaultBlockState().getShape(null, null);
                return shape == Shapes.block();
            } catch (Exception e2) {
                return true;
            }
        }
    }

    private boolean isFunctionalBlock(Block block) {
        if (block instanceof EntityBlock) return true;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return FUNCTIONAL_BLOCK_IDS.contains(id.getPath());
    }

    private void filter() {
        String query = searchField.getValue().toLowerCase();
        if (query.isEmpty()) {
            filtered = new ArrayList<>(allItems);
            return;
        }
        filtered = new ArrayList<>();
        for (Item item : allItems) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            String name = new ItemStack(item).getHoverName().getString().toLowerCase();
            if ((id != null && id.toString().contains(query)) || name.contains(query)) filtered.add(item);
        }
    }

    private int maxRow() {
        return Math.max(0, (filtered.size() + cols - 1) / cols - visRows());
    }

    private int visRows() {
        return Math.max(1, (panelY + panelH - 8 - gridTop()) / CELL);
    }

    private int gridTop() {
        return panelY + 40;
    }

    private int gridLeft() {
        return panelX + (panelW - cols * CELL - SCROLLBAR_WIDTH - 4) / 2;
    }

    private int scrollbarX() {
        return gridLeft() + cols * CELL + 4;
    }

    private int scrollbarHeight() {
        return visRows() * CELL;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, panelX, panelY, 0f, 0f, panelW, panelH, panelW, panelH);
        g.blit(RenderPipelines.GUI_TEXTURED, SLOTS, gridLeft(), gridTop(), 0f, 0f, cols * CELL, visRows() * CELL, 90, 54);
        g.text(font, title, width / 2 - font.width(title) / 2, panelY + 8, 0xFF222222, false);
        super.extractRenderState(g, mx, my, d);

        Item hovered = null;
        for (int row = 0; row < visRows(); row++) {
            for (int col = 0; col < cols; col++) {
                int idx = (scrollRow + row) * cols + col;
                if (idx >= filtered.size()) break;
                int x = gridLeft() + col * CELL, y = gridTop() + row * CELL;
                Item item = filtered.get(idx);
                g.item(new ItemStack(item), x + 1, y + 1);
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                    g.fill(x, y, x + CELL, y + CELL, 0x50FFFFFF);
                    hovered = item;
                }
            }
        }

        int scrollHandleY = gridTop() + (int) ((scrollbarHeight() - SCROLLBAR_HANDLE_HEIGHT) * scrollProgress);
        g.fill(scrollbarX(), gridTop(), scrollbarX() + SCROLLBAR_WIDTH, gridTop() + scrollbarHeight(), 0x80000000);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, scrollbarX(), scrollHandleY, SCROLLBAR_WIDTH, SCROLLBAR_HANDLE_HEIGHT);

        if (hovered != null) {
            String tooltipText = new ItemStack(hovered).getHoverName().getString();
            int tx = mx + 12, ty = my - 4, tw = font.width(tooltipText);
            g.fill(tx - 3, ty - 3, tx + tw + 3, ty + 12, 0xF0100010);
            g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 11, 0xF0281050);
            g.text(font, tooltipText, tx, ty, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean unknown) {
        double mx = event.x();
        double my = event.y();
        if (maxRow() > 0 && mx >= scrollbarX() && mx < scrollbarX() + SCROLLBAR_WIDTH
                && my >= gridTop() && my < gridTop() + scrollbarHeight()) {
            dragging = true;
            scrollProgress = Math.max(0, Math.min(1, (float) (my - gridTop() - SCROLLBAR_HANDLE_HEIGHT / 2.0) / (scrollbarHeight() - SCROLLBAR_HANDLE_HEIGHT)));
            scrollRow = Math.round(scrollProgress * maxRow());
            return true;
        }
        if (mx >= gridLeft() && mx < gridLeft() + cols * CELL && my >= gridTop() && my < gridTop() + visRows() * CELL) {
            int idx = (scrollRow + (int) (my - gridTop()) / CELL) * cols + (int) (mx - gridLeft()) / CELL;
            if (idx >= 0 && idx < filtered.size()) {
                callback.accept(filtered.get(idx));
                minecraft.gui.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, unknown);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double my = event.y();
        if (dragging && maxRow() > 0) {
            scrollProgress = Math.max(0, Math.min(1, (float) (my - gridTop() - SCROLLBAR_HANDLE_HEIGHT / 2.0) / (scrollbarHeight() - SCROLLBAR_HANDLE_HEIGHT)));
            scrollRow = Math.round(scrollProgress * maxRow());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scrollRow = Math.max(0, Math.min(scrollRow - (int) scrollY, maxRow()));
        scrollProgress = maxRow() > 0 ? (float) scrollRow / maxRow() : 0;
        return true;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}