package com.listraind.advancementwaypoints.gui.base;

import com.listraind.advancementwaypoints.DarkModeChecker;
import net.minecraft.resources.Identifier;

public class ModBackground {
   private static final Identifier BG_LIGHT = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/waypointscreenbackground.png");
   private static final Identifier BG_DARK = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/waypointscreenbackgrounddark.png");

   public static Identifier current() {
      return DarkModeChecker.isDarkModeEnabled() ? BG_DARK : BG_LIGHT;
   }
}
