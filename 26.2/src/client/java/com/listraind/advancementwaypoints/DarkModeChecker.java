package com.listraind.advancementwaypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;

public class DarkModeChecker {
   public static boolean isDarkModeEnabled() {
      for(Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
         if (pack.getTitle().getString().toLowerCase().contains("dark") || pack.getId().toLowerCase().contains("dark")) {
            return true;
         }
      }

      return false;
   }
}
