package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TabVisibilityScreen extends BaseModScreen {

    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 6;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HANDLE_HEIGHT = 15;

    private final Screen parentScreen;

    private static class TabEntry {
        String rootId;
        Component title;
        ItemStack icon;
        boolean visible;

        TabEntry(String rootId, Component title, ItemStack icon, boolean visible) {
            this.rootId = rootId;
            this.title = title;
            this.icon = icon;
            this.visible = visible;
        }
    }

    private final List<TabEntry> tabEntries = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();

    private int scrollRow = 0;
    private float scrollProgress = 0f;
    private boolean dragging = false;

    public TabVisibilityScreen(Screen parentScreen) {
        super(Component.translatable("advwp.screen.tab_visibility"), 280, 210);
        this.parentScreen = parentScreen;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for (AdvancementNode rootNode : mc.player.connection.getAdvancements().getTree().roots()) {
                if (rootNode.holder().value().display().isPresent()) {
                    DisplayInfo display = rootNode.holder().value().display().get();
                    String rootId = rootNode.holder().id().toString();
                    Component title = display.getTitle();
                    ItemStack icon = display.getIcon().create();
                    boolean visible = !WaypointStorage.isTabHidden(rootId);
                    tabEntries.add(new TabEntry(rootId, title, icon, visible));
                }
            }
        }
    }

    private int listLeft() { return panelX + 15; }
    private int listTop() { return panelY + 28; }
    private int listWidth() { return panelWidth - 30; }
    private int listHeight() { return VISIBLE_ROWS * ROW_HEIGHT; }
    private int scrollbarX() { return listLeft() + listWidth() - SCROLLBAR_WIDTH; }
    private int maxRow() { return Math.max(0, tabEntries.size() - VISIBLE_ROWS); }

    @Override
    protected void initContent() {
        rowButtons.clear();
        int btnWidth = 70;

        for (int r = 0; r < VISIBLE_ROWS; r++) {
            final int rowIndex = r;
            int btnX = scrollbarX() - btnWidth - 5;
            int btnY = listTop() + r * ROW_HEIGHT + 3;

            Button btn = addRenderableWidget(Button.builder(Component.empty(), b -> {
                int actualIdx = scrollRow + rowIndex;
                if (actualIdx >= 0 && actualIdx < tabEntries.size()) {
                    TabEntry entry = tabEntries.get(actualIdx);
                    entry.visible = !entry.visible;
                    WaypointStorage.setTabHidden(entry.rootId, !entry.visible);
                    updateRowButtons();
                }
            }).bounds(btnX, btnY, btnWidth, 18).build());

            rowButtons.add(btn);
        }

        updateRowButtons();

        addCenteredButton(Component.translatable("advwp.dialog.done"), panelHeight - 26, panelWidth - 40, 20, b -> {
            onClose();
        });
    }

    private void updateRowButtons() {
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            Button btn = rowButtons.get(r);
            int idx = scrollRow + r;
            if (idx < tabEntries.size()) {
                TabEntry entry = tabEntries.get(idx);
                btn.visible = true;
                btn.active = true;
                btn.setMessage(Component.translatable(entry.visible ? "advwp.visibility.shown" : "advwp.visibility.hidden"));
            } else {
                btn.visible = false;
                btn.active = false;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxRow() > 0) {
            scrollProgress = Mth.clamp(scrollProgress - (float) (verticalAmount / maxRow()), 0f, 1f);
            scrollRow = Math.round(scrollProgress * maxRow());
            updateRowButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean unknown) {
        double mx = event.x();
        double my = event.y();

        if (maxRow() > 0 && mx >= scrollbarX() && mx < scrollbarX() + SCROLLBAR_WIDTH
                && my >= listTop() && my < listTop() + listHeight()) {
            dragging = true;
            updateScrollFromMouse(my);
            return true;
        }
        return super.mouseClicked(event, unknown);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && maxRow() > 0) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double my) {
        float relativeY = (float) (my - listTop() - SCROLLBAR_HANDLE_HEIGHT / 2.0);
        float trackHeight = listHeight() - SCROLLBAR_HANDLE_HEIGHT;
        scrollProgress = Mth.clamp(relativeY / trackHeight, 0f, 1f);
        scrollRow = Math.round(scrollProgress * maxRow());
        updateRowButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);

        for (int r = 0; r < VISIBLE_ROWS; r++) {
            int idx = scrollRow + r;
            if (idx >= tabEntries.size()) break;

            TabEntry entry = tabEntries.get(idx);
            int textX = listLeft() + 24;
            int textY = listTop() + r * ROW_HEIGHT + 4;
            int iconX = listLeft() + 2;
            int iconY = listTop() + r * ROW_HEIGHT + 4;

            g.item(entry.icon, iconX, iconY);

            int maxTextWidth = scrollbarX() - textX - 75;
            Component title = entry.title;
            if (font.width(title) > maxTextWidth) {
                String str = font.plainSubstrByWidth(title.getString(), maxTextWidth - 8) + "...";
                title = Component.literal(str);
            }
            g.text(font, title, textX, textY + 2, 0xFF333333, false);
        }

        if (maxRow() > 0) {
            int scrollHandleY = listTop() + (int) ((listHeight() - SCROLLBAR_HANDLE_HEIGHT) * scrollProgress);
            g.fill(scrollbarX(), listTop(), scrollbarX() + SCROLLBAR_WIDTH, listTop() + listHeight(), 0x40000000);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, scrollbarX(), scrollHandleY, SCROLLBAR_WIDTH, SCROLLBAR_HANDLE_HEIGHT);
        }
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements(), parentScreen));
        } else {
            mc.gui.setScreen(parentScreen);
        }
    }
}
