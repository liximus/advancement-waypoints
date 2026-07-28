package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.dialogs.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.listraind.advancementwaypoints.navigator.WaypointLocatorMode;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class AdvancementWaypointsClient implements ClientModInitializer {
   public void onInitializeClient() {
      Navigator.getInstance().initHud();
      KeyMapping.Category keyCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath("advancement-waypoints", "key_category"));
      KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("advwp.key.open_menu", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keyCategory));
      KeyMapping clearNavKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("advwp.key.clear_nav", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keyCategory));
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
         // Tick the locator mode to keep the vanilla waypoint in sync
         WaypointLocatorMode.getInstance().tick();

         while(openKey.consumeClick()) {
            client.execute(() -> client.gui.setScreen(new MainMenuScreen()));
         }

         while(clearNavKey.consumeClick()) {
            clearNavigation();
         }

      });
      Commands.initialize();
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(handler, client) -> {
         WaypointStorage.setLastParent((Identifier)null);
         WaypointLocatorMode.getInstance().removeWaypoint();
         clearNavigation();
      });
   }

   private static void clearNavigation() {
      Navigator nav = Navigator.getInstance();
      nav.clearAll();
      nav.setCurrentId((Identifier)null);
   }

   public static void reloadAdvancements() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ((IAdvancementInjector)mc.player.connection.getAdvancements()).advWaypoint_inject();
      }
   }
}
