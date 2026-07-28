package com.listraind.advancementwaypoints.gui.dialogs;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CreateTabScreen extends WaypointFormScreen {
   public CreateTabScreen() {
      super(Component.translatable("advwp.screen.create_tab"));
   }

   protected boolean showParentField() {
      return false;
   }

   protected boolean showBackgroundField() {
      return true;
   }

   protected boolean showCoordsField() {
      return false;
   }

   protected void initActions(int centerX, int y) {
      int formWidth = this.panelWidth - 40;
      this.addRenderableWidget(Button.builder(Component.translatable("advwp.action.save"), (b) -> {
         if (!this.nameField.getValue().trim().isEmpty()) {
            String uniqueId = WaypointStorage.generateUniqueId("customtab");
            JsonObject json = WaypointStorage.buildWaypointJson(uniqueId, this.nameField.getValue().trim(), this.iconId(), this.buildFinalDescription(), (String)null, this.getBackgroundValue());
            WaypointStorage.saveWaypoint(uniqueId, json);
            this.onCloseAction.run();
         }
      }).bounds(centerX - formWidth / 2, y, formWidth, 20).build());
   }
}
