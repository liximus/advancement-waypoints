package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.components.Button;

import java.lang.reflect.Method;
import java.util.Map;

public class BetterAdvancementsHelper {

    private static Method cachedIsMouseOverMethod;

    public static void syncModButton(Button modButton, int screenWidth, int screenHeight, int internalWidth, int internalHeight) {
        if (modButton == null) return;
        int panelLeft = (screenWidth - internalWidth) / 2 + 30;
        int panelTop = (screenHeight - internalHeight) / 2 + 40;
        int panelRight = panelLeft + internalWidth - 70;

        int btnW = 26, btnH = 26;
        int gap = 5;
        int btnX = Math.max(2, Math.min(panelRight - btnW - gap, screenWidth - btnW - 2));
        int btnY = Math.max(2, Math.min(panelTop + 20, screenHeight - btnH - 2));

        modButton.setX(btnX);
        modButton.setY(btnY);
        modButton.setWidth(btnW);
        modButton.setHeight(btnH);
    }

    public static boolean isTabHeaderClicked(int screenWidth, int screenHeight, int internalWidth, int internalHeight, double mx, double my) {
        int left = 30 + (screenWidth - internalWidth) / 2;
        int top = 40 + (screenHeight - internalHeight) / 2;
        return my >= top - 36 && my <= top + 24 && mx >= left - 20 && mx <= left + internalWidth + 20;
    }

    public static AdvancementHolder findHoveredHolder(
            BetterAdvancementTab selectedTab, float zoom,
            int screenWidth, int screenHeight, int internalWidth, int internalHeight,
            double mx, double my
    ) {
        if (selectedTab == null) return null;

        BetterAdvancementTabAccessor tab = (BetterAdvancementTabAccessor) selectedTab;
        int left = 30 + (screenWidth - internalWidth) / 2;
        int top = 40 + (screenHeight - internalHeight) / 2;

        boolean inGui = mx < (double) (left + internalWidth - 60 - 9)
                && mx > (double) (left + 9)
                && my < (double) (top + internalHeight - 40 + 1)
                && my > (double) (top + 18);

        if (!inGui) return null;

        double relX = mx - left - 9.0;
        double relY = my - top - 18.0;

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
                    hovered = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.isWidgetHovered(w, scrollX, scrollY, mx, my, left + 9, top + 18);
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
                if (isPlaneAdv) {
                    AdvancementNode node = com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper.getAdvancementNode(w);
                    if (node != null) holder = node.holder();
                }
                if (holder == null && w instanceof BetterAdvancementWidget bw) {
                    for (Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                        if (entry.getValue() == bw) {
                            holder = entry.getKey();
                            break;
                        }
                    }
                }

                if (holder != null) {
                    return holder;
                }
            }
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Failed to invoke isMouseOver in BetterAdvancementsHelper", e);
        }
        return null;
    }
}
