package com.listraind.advancementwaypoints.gui.handler;

import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.mixin.client.AdvancementTabAccessor;
import com.listraind.advancementwaypoints.mixin.client.AdvancementWidgetAccessor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.util.Mth;

import java.util.Collection;
import java.util.Map;

public class AdvancementHoverHelper {

    private static final boolean IS_PLANE_MOD =
            net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("planeadvancements");

    public static AdvancementNode findHoveredNode(
            Object screen, int screenWidth, int screenHeight,
            Map<AdvancementHolder, AdvancementTab> tabs,
            AdvancementTab selectedTab,
            double mx, double my
    ) {
        if (selectedTab == null) return null;

        int defLeft = (screenWidth - AdvancementsScreen.WINDOW_WIDTH) / 2 + 9;
        int defTop = (screenHeight - AdvancementsScreen.WINDOW_HEIGHT) / 2 + 18;

        int contentLeft = defLeft;
        int contentTop = defTop;

        int captX = AdvancementTabCapture.getX();
        int captY = AdvancementTabCapture.getY();
        if (captX != 0 || captY != 0) {
            contentLeft = captX;
            contentTop = captY;
        }

        if (IS_PLANE_MOD) {
            contentLeft = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getContentLeft(screen, defLeft);
            contentTop = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getContentTop(screen, defTop);
        }

        int relX = (int) mx - contentLeft;
        int relY = (int) my - contentTop;

        if (!IS_PLANE_MOD) {
            if (relX < 0 || relX >= 234 || relY < 0 || relY >= 113) {
                return null;
            }
        } else {
            if (relY < 0) {
                return null;
            }
        }

        return findInTab(selectedTab, contentLeft, contentTop, mx, my);
    }

    private static AdvancementNode findInTab(AdvancementTab currentTab, int contentLeft, int contentTop, double mx, double my) {
        AdvancementTabAccessor tabAccessor = (AdvancementTabAccessor) currentTab;
        boolean isPlaneAdv = IS_PLANE_MOD && com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.isPlaneTab(currentTab);

        double scrollX = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getPanX(currentTab)
                : tabAccessor.getScrollX();
        double scrollY = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getPanY(currentTab)
                : tabAccessor.getScrollY();

        Collection<?> widgets = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getWidgets(currentTab)
                : tabAccessor.getWidgets().values();

        int relX = (int) mx - contentLeft;
        int relY = (int) my - contentTop;

        for (Object w : widgets) {
            boolean isHovered = isPlaneAdv
                    ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.isWidgetHovered(w, scrollX, scrollY, mx, my, contentLeft, contentTop)
                    : ((AdvancementWidgetAccessor) w).invokeIsMouseOver(Mth.floor(scrollX), Mth.floor(scrollY), relX, relY);
            if (!isHovered) continue;

            AdvancementNode node = isPlaneAdv
                    ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getAdvancementNode(w)
                    : ((AdvancementWidgetAccessor) w).getAdvancementNode();
            if (node != null && !WaypointStorage.isNodeHidden(node)) {
                return node;
            }
        }
        return null;
    }
}
