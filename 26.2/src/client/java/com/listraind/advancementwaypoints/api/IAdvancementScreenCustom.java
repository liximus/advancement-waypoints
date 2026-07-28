package com.listraind.advancementwaypoints.api;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public interface IAdvancementScreenCustom {
   void advWaypoint_setSelectMode(Consumer<Identifier> var1);

   void advWaypoint_setScreenToOpen(Screen var1);

   void advWaypoint_setParentScreen(Screen var1);

   default boolean advWaypoint_isMouseOverContextMenu(double mx, double my) {
      return false;
   }
}
