package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static final ResourceLocation SLOTS =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/slots.png");
    private static final ResourceLocation SCROLLER_SPRITE =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");

    private static final int CELL_SIZE = 18;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;

    private final WaypointScreen parentScreen;
    private final Consumer<Item> onItemSelected;

    private EditBox searchField;
    private List<Item> allItems;
    private List<Item> filteredItems;

    private int scrollRow;
    private float scrollProgress;
    private boolean isDraggingScrollbar;

    private int columns;
    private int panelX, panelY, panelWidth, panelHeight;

    public ItemPickerScreen(WaypointScreen parentScreen, Consumer<Item> onItemSelected) {
        super(Component.literal("Выбор иконки"));
        this.parentScreen = parentScreen;
        this.onItemSelected = onItemSelected;
    }

    @Override
    protected void init() {
        allItems = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(allItems::add);
        allItems.remove(Items.AIR);

        panelWidth = Math.min(width - 20, 360);
        panelHeight = Math.min(height - 20, 280);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        columns = Math.max(1, (panelWidth - 20 - SCROLLBAR_WIDTH) / CELL_SIZE);

        int searchWidth = Math.min(panelWidth - 20, 200);
        searchField = addRenderableWidget(
                new EditBox(font, panelX + (panelWidth - searchWidth) / 2, panelY + 18, searchWidth, 16, Component.literal("")));
        searchField.setHint(Component.literal("Поиск предмета"));
        searchField.setResponder(text -> {
            applyFilter();
            scrollRow = 0;
            scrollProgress = 0.0F;
        });
        setInitialFocus(searchField);

        applyFilter();
        scrollRow = 0;
        scrollProgress = 0.0F;
        isDraggingScrollbar = false;
    }

    private void applyFilter() {
        String query = searchField.getValue().toLowerCase();
        if (query.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
            return;
        }
        filteredItems = new ArrayList<>();
        for (Item item : allItems) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String displayName = new ItemStack(item).getHoverName().getString().toLowerCase();
            if ((id != null && id.toString().contains(query)) || displayName.contains(query)) {
                filteredItems.add(item);
            }
        }
    }

    private int getMaxScrollRow() {
        return Math.max(0, (filteredItems.size() + columns - 1) / columns - getVisibleRows());
    }

    private void syncScrollProgressFromRow() {
        int max = getMaxScrollRow();
        scrollProgress = max > 0 ? (float) scrollRow / max : 0.0F;
    }

    private void syncScrollRowFromProgress() {
        int max = getMaxScrollRow();
        scrollRow = Math.round(scrollProgress * max);
        scrollRow = Math.max(0, Math.min(scrollRow, max));
    }

    private int getGridTop() {
        return panelY + 40;
    }

    private int getGridLeft() {
        return panelX + (panelWidth - columns * CELL_SIZE - SCROLLBAR_WIDTH - 4) / 2;
    }

    private int getVisibleRows() {
        return Math.max(1, (panelY + panelHeight - 8 - getGridTop()) / CELL_SIZE);
    }

    private int getScrollbarX() {
        return getGridLeft() + columns * CELL_SIZE + 4;
    }

    private int getScrollbarTop() {
        return getGridTop();
    }

    private int getScrollbarHeight() {
        return getVisibleRows() * CELL_SIZE;
    }

    private boolean canScroll() {
        return getMaxScrollRow() > 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                panelX, panelY, 0.0F, 0.0F, panelWidth, panelHeight, panelWidth, panelHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLOTS,
                getGridLeft(), getGridTop(), 0.0F, 0.0F,
                columns * CELL_SIZE, getVisibleRows() * CELL_SIZE, 90, 54);

        graphics.drawString(font, title, width / 2 - font.width(title) / 2, panelY + 8, 0xFF222222, false);

        super.render(graphics, mouseX, mouseY, delta);

        int gridTop = getGridTop();
        int gridLeft = getGridLeft();
        int visibleRows = getVisibleRows();
        Item hoveredItem = null;

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scrollRow + row) * columns + col;
                if (index >= filteredItems.size()) break;

                int x = gridLeft + col * CELL_SIZE;
                int y = gridTop + row * CELL_SIZE;
                Item item = filteredItems.get(index);

                graphics.renderItem(new ItemStack(item), x + 1, y + 1);

                if (mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE) {
                    graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x50FFFFFF);
                    hoveredItem = item;
                }
            }
        }

        int sbX = getScrollbarX();
        int sbY = getScrollbarTop();
        int sbH = getScrollbarHeight();
        int scrollerY = sbY + (int) ((sbH - SCROLLER_HEIGHT) * scrollProgress);

        graphics.fill(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + sbH, 0x80000000);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE,
                sbX, scrollerY, SCROLLBAR_WIDTH, SCROLLER_HEIGHT);

        if (hoveredItem != null) {
            String name = new ItemStack(hoveredItem).getHoverName().getString();
            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 4;
            int textWidth = font.width(name);
            graphics.fill(tooltipX - 3, tooltipY - 3, tooltipX + textWidth + 3, tooltipY + 12, 0xF0100010);
            graphics.fill(tooltipX - 2, tooltipY - 2, tooltipX + textWidth + 2, tooltipY + 11, 0xF0281050);
            graphics.drawString(font, name, tooltipX, tooltipY, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridTop = getGridTop();
        int gridLeft = getGridLeft();
        int visibleRows = getVisibleRows();

        int sbX = getScrollbarX();
        int sbY = getScrollbarTop();
        int sbH = getScrollbarHeight();

        if (canScroll() && mouseX >= sbX && mouseX < sbX + SCROLLBAR_WIDTH
                && mouseY >= sbY && mouseY < sbY + sbH) {
            isDraggingScrollbar = true;
            float relative = (float) (mouseY - sbY - SCROLLER_HEIGHT / 2.0) / (sbH - SCROLLER_HEIGHT);
            scrollProgress = Math.max(0.0F, Math.min(1.0F, relative));
            syncScrollRowFromProgress();
            return true;
        }

        if (mouseX >= gridLeft && mouseX < gridLeft + columns * CELL_SIZE
                && mouseY >= gridTop && mouseY < gridTop + visibleRows * CELL_SIZE) {
            int col = (int) (mouseX - gridLeft) / CELL_SIZE;
            int row = (int) (mouseY - gridTop) / CELL_SIZE;
            int index = (scrollRow + row) * columns + col;

            if (index >= 0 && index < filteredItems.size()) {
                onItemSelected.accept(filteredItems.get(index));
                minecraft.setScreen(parentScreen);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && canScroll()) {
            int sbY = getScrollbarTop();
            int sbH = getScrollbarHeight();
            float relative = (float) (mouseY - sbY - SCROLLER_HEIGHT / 2.0) / (sbH - SCROLLER_HEIGHT);
            scrollProgress = Math.max(0.0F, Math.min(1.0F, relative));
            syncScrollRowFromProgress();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int max = getMaxScrollRow();
        scrollRow = Math.max(0, Math.min(scrollRow - (int) deltaY, max));
        syncScrollProgressFromRow();
        return true;
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parentScreen);
    }


}