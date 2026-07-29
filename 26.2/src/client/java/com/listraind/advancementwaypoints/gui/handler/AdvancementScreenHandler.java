package com.listraind.advancementwaypoints.gui.handler;

import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.context.AdvancementContextMenu;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdvancementScreenHandler {

    private boolean selectMode;
    private Screen advancementsLastScreen;
    private Consumer<Identifier> selectCallback;
    private Screen screenToOpen;
    private Screen parentScreen;

    private ButtonState pressState = ButtonState.NONE;
    private AdvancementNode pressedNode = null;

    @Nullable
    private AdvancementContextMenu contextMenu;

    public void setSelectMode(Consumer<Identifier> cb) {
        selectMode = true;
        selectCallback = cb;
    }

    public void setScreenToOpen(Screen s) {
        screenToOpen = s;
    }

    public void setParentScreen(Screen screen) {
        parentScreen = screen;
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    @Nullable
    public Consumer<Identifier> getSelectCallback() {
        return selectCallback;
    }

    @Nullable
    public Screen getScreenToOpen() {
        return screenToOpen;
    }

    @Nullable
    public Screen getParentScreen() {
        return parentScreen;
    }

    public Screen resolveTargetScreen(@Nullable Screen lastScreen) {
        return screenToOpen != null ? screenToOpen : (parentScreen != null ? parentScreen : lastScreen);
    }

    public void setAdvancementsLastScreen(Screen screen) {
        advancementsLastScreen = screen;
    }

    public Screen getAdvancementsLastScreen() {
        return advancementsLastScreen;
    }

    public void syncModButton(Button modButton, int screenWidth, int screenHeight) {
        if (modButton == null) return;
        int btnW = modButton.getWidth(), btnH = modButton.getHeight();
        int captX = AdvancementTabCapture.getX();
        int captY = AdvancementTabCapture.getY();
        int panelLeft, panelTop, panelRight;

        if (captX != 0 || captY != 0) {
            panelLeft = captX - 9;
            panelTop = captY - 18;
            panelRight = screenWidth - panelLeft;
        } else {
            panelLeft = (screenWidth - AdvancementsScreen.WINDOW_WIDTH) / 2;
            panelTop = (screenHeight - AdvancementsScreen.WINDOW_HEIGHT) / 2;
            panelRight = panelLeft + AdvancementsScreen.WINDOW_WIDTH;
        }

        int btnX = Math.max(2, Math.min(panelRight - 9 - 3 - btnW, screenWidth - btnW - 2));
        int btnY = Math.max(2, Math.min(panelTop + 20, screenHeight - btnH - 2));
        modButton.setX(btnX);
        modButton.setY(btnY);
    }

    public boolean handleMouseClick(
            Screen screen, double mx, double my, int btn,
            Map<AdvancementHolder, AdvancementTab> tabs, AdvancementTab selectedTab
    ) {
        if (handleContextMenuClick(mx, my, btn)) {
            return true;
        }

        pressState = new ButtonState(mx, my, btn);
        pressedNode = AdvancementHoverHelper.findHoveredNode(screen, screen.width, screen.height, tabs, selectedTab, mx, my);

        if (btn == 1 && isTabAreaClicked(screen, mx, my, tabs)) {
            return true;
        }
        return false;
    }

    public void handleMouseRelease(
            Screen screen, double mx, double my, int btn,
            Map<AdvancementHolder, AdvancementTab> tabs, AdvancementTab selectedTab
    ) {
        if (!pressState.isClick(mx, my, btn)) {
            pressState = ButtonState.NONE;
            pressedNode = null;
            return;
        }
        pressState = ButtonState.NONE;

        AdvancementNode node = pressedNode;
        pressedNode = null;

        if (node == null) {
            node = AdvancementHoverHelper.findHoveredNode(screen, screen.width, screen.height, tabs, selectedTab, mx, my);
        }

        final AdvancementNode targetNode = node;
        if (targetNode != null) {
            targetNode.holder().value().display().ifPresent(d -> {
                Identifier id = targetNode.holder().id();
                String idStr = id.toString();
                if (btn == 0) {
                    if (WaypointStorage.isBranchHidden(idStr)) {
                        WaypointStorage.setBranchHidden(idStr, false);
                    } else if (com.listraind.advancementwaypoints.config.ModConfig.getInstance().isEnableNavigation()) {
                        Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                        handleLeftClick(id, targets);
                    }
                } else if (btn == 1) {
                    Map<Navigator.Dimension, List<BlockPos>> parsed = CoordParser.parseForNavigation(d.getDescription().getString());
                    showContextMenu(mx, my, id, parsed);
                }
            });
        } else if (btn == 1 && isTabAreaClicked(screen, mx, my, tabs)) {
            showTabContextMenu(mx, my);
        }
    }

    private boolean isTabAreaClicked(Screen screen, double mx, double my, Map<AdvancementHolder, AdvancementTab> tabs) {
        int captX = AdvancementTabCapture.getX();
        int captY = AdvancementTabCapture.getY();
        int panelLeft = (captX != 0 || captY != 0) ? captX - 9 : (screen.width - AdvancementsScreen.WINDOW_WIDTH) / 2;
        int panelTop = (captX != 0 || captY != 0) ? captY - 18 : (screen.height - AdvancementsScreen.WINDOW_HEIGHT) / 2;

        if (tabs != null) {
            for (AdvancementTab tab : tabs.values()) {
                if (tab.isMouseOver(panelLeft, panelTop, mx, my)) {
                    return true;
                }
            }
        }
        return mx >= panelLeft && mx <= panelLeft + AdvancementsScreen.WINDOW_WIDTH
                && my >= panelTop - 32 && my <= panelTop + 20;
    }

    public void handleLeftClick(Identifier id, @Nullable Map<Navigator.Dimension, List<BlockPos>> targets) {
        if (selectMode) {
            if (selectCallback != null) selectCallback.accept(id);
            Minecraft.getInstance().gui.setScreen(resolveTargetScreen(null));
        } else {
            Navigator nav = Navigator.getInstance();
            if (!java.util.Objects.equals(nav.getCurrentId(), id) && targets != null) {
                nav.clearAll();
                nav.setCurrentId(id);
                targets.forEach((dim, posList) -> {
                    if (posList != null) nav.setTargets(dim, posList);
                });
            } else {
                nav.clearAll();
            }
            if (targets != null) Minecraft.getInstance().gui.setScreen(null);
        }
    }

    public void showContextMenu(double mx, double my, Identifier advancementId, Map<Navigator.Dimension, List<BlockPos>> targets) {
        if (contextMenu == null) contextMenu = new AdvancementContextMenu();
        contextMenu.show((int) mx, (int) my, advancementId, targets);
        contextMenu.setLastScreen(advancementsLastScreen);
    }

    public void showTabContextMenu(double mx, double my) {
        if (contextMenu == null) contextMenu = new AdvancementContextMenu();
        contextMenu.showTabMenu((int) mx, (int) my);
        contextMenu.setLastScreen(advancementsLastScreen);
    }

    public boolean handleContextMenuClick(double mx, double my, int btn) {
        return contextMenu != null && contextMenu.isVisible() && contextMenu.mouseClicked(mx, my, btn);
    }

    public boolean isMouseOverContextMenu(double mx, double my) {
        return contextMenu != null && contextMenu.isVisible() && contextMenu.isMouseOver(mx, my);
    }

    @Nullable
    public AdvancementContextMenu getContextMenu() {
        return contextMenu;
    }

    public void renderContextMenu(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (contextMenu != null && contextMenu.isVisible()) {
            contextMenu.render(g, mx, my, pt);
        }
    }

    public void resetSelectMode() {
        selectMode = false;
        selectCallback = null;
        screenToOpen = null;
    }

    public record ButtonState(double pressMx, double pressMy, int pressBtn) {
        public static final ButtonState NONE = new ButtonState(0, 0, -1);
        private static final double DRAG_THRESHOLD = 10.0;

        public boolean isClick(double mx, double my, int btn) {
            if (pressBtn != btn) return false;
            double dx = mx - pressMx;
            double dy = my - pressMy;
            return dx * dx + dy * dy <= DRAG_THRESHOLD * DRAG_THRESHOLD;
        }
    }
}
