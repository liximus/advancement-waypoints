package com.listraind.advancementwaypoints.gui.dialogs;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;

public class MainMenuScreen extends BaseModScreen {
   private final Screen parent;

   public MainMenuScreen() {
      this((Screen)null);
   }

   public MainMenuScreen(Screen parent) {
      super(Component.translatable("advwp.menu.title"), 200, 120);
      this.parent = parent;
   }

   protected void initContent() {
      int buttonWidth = this.panelWidth - 40;
      this.addCenteredButton(Component.translatable("advwp.menu.create_waypoint"), 25, buttonWidth, 20, (b) -> this.minecraft.gui.setScreen(new CreateWaypointScreen()));
      this.addCenteredButton(Component.translatable("advwp.menu.create_tab"), 55, buttonWidth, 20, (b) -> this.minecraft.gui.setScreen(new CreateTabScreen()));
      this.addCenteredButton(Component.translatable("advwp.menu.edit"), 85, buttonWidth, 20, (b) -> {
         if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
            this.minecraft.gui.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this));
            Screen patt0$temp = this.minecraft.gui.screen();
            if (patt0$temp instanceof IAdvancementScreenCustom) {
               IAdvancementScreenCustom customScreen = (IAdvancementScreenCustom)patt0$temp;
               customScreen.advWaypoint_setParentScreen(this);
               customScreen.advWaypoint_setSelectMode((id) -> {
                  JsonObject data = WaypointStorage.getWaypointOrVanilla(id);
                  EditWaypointScreen editScreen = new EditWaypointScreen(data);
                  editScreen.onCloseAction = () -> {
                     if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.gui.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), (Screen)null));
                     }

                  };
                  customScreen.advWaypoint_setScreenToOpen(editScreen);
               });
            }

         }
      });
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(this.parent);
      }

   }
}
