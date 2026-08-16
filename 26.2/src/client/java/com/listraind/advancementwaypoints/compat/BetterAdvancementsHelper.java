package com.listraind.advancementwaypoints.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.mixin.compat.BetterAdvancementTabAccessor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

public class BetterAdvancementsHelper {

    private static Method cachedIsMouseOverMethod;

    public static boolean isTabHeaderClicked(
            Map<AdvancementHolder, BetterAdvancementTab> tabs,
            int screenWidth, int screenHeight, int internalWidth, int internalHeight,
            int tabPage, double mx, double my
    ) {
        return findClickedTab(tabs, screenWidth, screenHeight, internalWidth, internalHeight, tabPage, mx, my) != null;
    }

    @Nullable
    public static BetterAdvancementTab findClickedTab(
            Map<AdvancementHolder, BetterAdvancementTab> tabs,
            int screenWidth, int screenHeight, int internalWidth, int internalHeight,
            int tabPage, double mx, double my
    ) {
        if (tabs == null || tabs.isEmpty()) return null;
        int left = 30 + (screenWidth - internalWidth) / 2;
        int top = 40 + (screenHeight - internalHeight) / 2;
        int right = internalWidth - 30 + (screenWidth - internalWidth) / 2;
        int bottom = internalHeight - 30 + (screenHeight - internalHeight) / 2;
        int width = right - left;
        int height = bottom - top;
        int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);
        int skip = tabPage * maxTabs;

        for (BetterAdvancementTab tab : tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
            if (tab.isMouseOver(left, top, width, height, mx, my)) {
                return tab;
            }
        }
        return null;
    }

    @Nullable
    public static AdvancementHolder findHoveredHolder(
            BetterAdvancementTab selectedTab, float zoom,
            int screenWidth, int screenHeight, int internalWidth, int internalHeight,
            double mx, double my
    ) {
        if (selectedTab == null) return null;

        BetterAdvancementTabAccessor tab = (BetterAdvancementTabAccessor) selectedTab;
        int left = 30 + (screenWidth - internalWidth) / 2;
        int top = 40 + (screenHeight - internalHeight) / 2;
        int right = internalWidth - 30 + (screenWidth - internalWidth) / 2;
        int bottom = internalHeight - 30 + (screenHeight - internalHeight) / 2;

        int boxLeft = left + 9;
        int boxTop = top + 18;
        int boxRight = right - 9;
        int boxBottom = bottom - 9;

        boolean inGui = mx >= (double) boxLeft && mx <= (double) boxRight
                && my >= (double) boxTop && my <= (double) boxBottom;

        if (!inGui) return null;

        double relX = mx - (double) boxLeft;
        double relY = my - (double) boxTop;

        boolean isPlaneAdv = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("planeadvancements")
                && com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.isPlaneTab(selectedTab);

        double scrollX = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getPanX(selectedTab)
                : (double) tab.getScrollX();
        double scrollY = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getPanY(selectedTab)
                : (double) tab.getScrollY();

        java.util.Collection<?> widgets = isPlaneAdv
                ? com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getWidgets(selectedTab)
                : tab.getWidgets().values();

        try {
            for (Object w : widgets) {
                boolean hovered;
                if (isPlaneAdv) {
                    hovered = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.isWidgetHovered(w, scrollX, scrollY, mx, my, boxLeft, boxTop);
                } else if (w instanceof BetterAdvancementWidget bw) {
                    hovered = bw.isMouseOver(scrollX, scrollY, relX, relY, zoom);
                } else {
                    if (cachedIsMouseOverMethod == null) {
                        cachedIsMouseOverMethod = w.getClass().getMethod(
                                "isMouseOver",
                                double.class, double.class, double.class, double.class, float.class
                        );
                    }
                    hovered = (boolean) cachedIsMouseOverMethod.invoke(w, scrollX, scrollY, relX, relY, zoom);
                }

                if (!hovered) continue;

                AdvancementHolder holder = null;
                AdvancementNode node = null;
                if (isPlaneAdv) {
                    node = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getAdvancementNode(w);
                    if (node != null) holder = node.holder();
                }
                if (holder == null && w instanceof BetterAdvancementWidget bw) {
                    node = bw.getAdvancement();
                    if (node != null) {
                        holder = node.holder();
                    } else {
                        for (Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                            if (entry.getValue() == bw) {
                                holder = entry.getKey();
                                break;
                            }
                        }
                    }
                }

                if (node != null && WaypointStorage.isNodeHidden(node)) {
                    continue;
                }

                if (holder != null) {
                    return holder;
                }
            }
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Failed to check hovered advancement in BetterAdvancementsHelper", e);
        }
        return null;
    }
}
