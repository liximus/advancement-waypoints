package com.listraind.advancementwaypoints.compat;

import com.nettakrim.planeadvancements.AdvancementTabInterface;
import com.nettakrim.planeadvancements.AdvancementWidgetInterface;
import com.nettakrim.planeadvancements.FullscreenInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

public class PlaneAdvancementsHelper {
   public static boolean isPlaneTab(Object tab) {
      return tab instanceof AdvancementTabInterface;
   }

   public static double getPanX(Object tab) {
      if (tab instanceof AdvancementTabInterface planeTab) {
         return planeTab.planeAdvancements$getPanX();
      } else {
         return (double)0.0F;
      }
   }

   public static double getPanY(Object tab) {
      if (tab instanceof AdvancementTabInterface planeTab) {
         return planeTab.planeAdvancements$getPanY();
      } else {
         return (double)0.0F;
      }
   }

   public static int getContentLeft(Object screen, int defaultLeft) {
      if (screen instanceof FullscreenInterface fullScreen) {
         if (screen instanceof Screen s) {
            try {
               int w = fullScreen._advancements_fullscreen_getFullscreenWindowWidth();
               if (w > 0) {
                  return (s.width - w) / 2 + 9;
               }
            } catch (Throwable var5) {
            }
         }
      }

      return defaultLeft;
   }

   public static int getContentTop(Object screen, int defaultTop) {
      if (screen instanceof FullscreenInterface fullScreen) {
         if (screen instanceof Screen s) {
            try {
               int h = fullScreen._advancements_fullscreen_getFullscreenWindowHeight();
               if (h > 0) {
                  return (s.height - h) / 2 + 18;
               }
            } catch (Throwable var5) {
            }
         }
      }

      return defaultTop;
   }

   public static Collection<?> getWidgets(Object tab) {
      if (tab instanceof AdvancementTabInterface planeTab) {
         Map<AdvancementHolder, AdvancementWidgetInterface> widgetsMap = planeTab.planeAdvancements$getWidgets();
         if (widgetsMap != null) {
            return widgetsMap.values();
         }
      }

      return Collections.emptyList();
   }

   public static boolean isWidgetHovered(Object widget, double panX, double panY, double mx, double my, int contentLeft, int contentTop) {
      if (widget instanceof AdvancementWidgetInterface planeWidget) {
         int relX = Mth.floor(mx - (double)contentLeft);
         int relY = Mth.floor(my - (double)contentTop);
         if (relY < -15) {
            return false;
         } else {
            planeWidget.planeAdvancements$updatePos();
            return planeWidget.planeAdvancements$isHovering(panX, panY, relX, relY);
         }
      } else {
         return false;
      }
   }

   public static AdvancementNode getAdvancementNode(Object widget) {
      if (widget instanceof AdvancementWidgetInterface planeWidget) {
         return planeWidget.planeAdvancements$getPlaced();
      } else {
         return null;
      }
   }
}
