package com.listraind.advancementwaypoints.gui.base;

import com.listraind.advancementwaypoints.DarkModeChecker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class ModBackground {
   private static final Identifier BG_LIGHT = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/waypointscreenbackground.png");
   private static final Identifier BG_DARK = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/waypointscreenbackgrounddark.png");

   private static final int LIGHT_BG = 0xFFC6C6C6;
   private static final int LIGHT_HIGHLIGHT = 0xFFFFFFFF;
   private static final int LIGHT_SHADOW = 0xFF555555;

   private static final int DARK_BG = 0xFF303030;
   private static final int DARK_HIGHLIGHT = 0xFF5C5C5C;
   private static final int DARK_SHADOW = 0xFF1A1A1A;

   private static final int BORDER_BLACK = 0xFF000000;

   public static Identifier current() {
      return DarkModeChecker.isDarkModeEnabled() ? BG_DARK : BG_LIGHT;
   }

   public static void render(GuiGraphicsExtractor g, int x, int y, int width, int height) {
      render(g, x, y, width, height, DarkModeChecker.isDarkModeEnabled());
   }

   public static void render(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean isDark) {
      int bg = isDark ? DARK_BG : LIGHT_BG;
      int hi = isDark ? DARK_HIGHLIGHT : LIGHT_HIGHLIGHT;
      int sh = isDark ? DARK_SHADOW : LIGHT_SHADOW;
      int black = BORDER_BLACK;

      g.fill(x + 3, y + 3, x + width - 3, y + height - 3, bg);

      g.fill(x + 2, y, x + width - 2, y + 1, black);
      g.fill(x + 2, y + height - 1, x + width - 2, y + height, black);
      g.fill(x, y + 2, x + 1, y + height - 2, black);
      g.fill(x + width - 1, y + 2, x + width, y + height - 2, black);

      g.fill(x + 1, y + 1, x + 2, y + 2, black);
      g.fill(x + width - 2, y + 1, x + width - 1, y + 2, black);
      g.fill(x + 1, y + height - 2, x + 2, y + height - 1, black);
      g.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, black);

      g.fill(x + 2, y + 1, x + width - 2, y + 3, hi);
      g.fill(x + 1, y + 2, x + 3, y + height - 2, hi);

      g.fill(x + 2, y + height - 3, x + width - 2, y + height - 1, sh);
      g.fill(x + width - 3, y + 2, x + width - 1, y + height - 2, sh);
   }
}
