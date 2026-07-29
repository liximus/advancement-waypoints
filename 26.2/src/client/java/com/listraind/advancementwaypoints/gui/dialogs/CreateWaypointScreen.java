package com.listraind.advancementwaypoints.gui.dialogs;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CreateWaypointScreen extends WaypointFormScreen {
   private final boolean isTabMode;

   public CreateWaypointScreen() {
      this(false);
   }

   public CreateWaypointScreen(boolean isTabMode) {
      super(isTabMode ? Component.translatable("advwp.screen.create_tab") : Component.translatable("advwp.screen.create_waypoint"));
      this.isTabMode = isTabMode;
      if (!isTabMode) {
         Identifier lastParent = WaypointStorage.getLastParent();
         if (lastParent != null) {
            this.selectedParentId = lastParent;
            this.hadParentBefore = true;
            this.addDimRow(this.currentDim());
         }
      } else {
         this.selectedParentId = null;
         this.hadParentBefore = false;
      }

   }

   protected boolean showParentField() {
      return !this.isTabMode;
   }

   protected boolean showBackgroundField() {
      return this.isTabMode || this.isRoot();
   }

   protected boolean showCoordsField() {
      return !this.isTabMode;
   }

   protected void initActions(int centerX, int y) {
      int formWidth = this.panelWidth - 40;
      Button saveButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.action.save"), (b) -> {
         if (!this.nameField.getValue().trim().isEmpty()) {
            if (this.isTabMode || this.selectedParentId != null) {
               String namespace = this.selectedParentId != null ? this.selectedParentId.getNamespace() : "custom";
               String uniqueId = WaypointStorage.generateUniqueId(namespace);
               String parentStr = this.selectedParentId != null ? this.selectedParentId.toString() : null;
               String bgVal = this.showBackgroundField() ? this.getBackgroundValue() : null;
               JsonObject json = WaypointStorage.buildWaypointJson(uniqueId, this.colorCodes(this.nameField.getValue().trim()), this.iconId(), this.buildFinalDescription(), this.savedFrame, parentStr, bgVal);
               WaypointStorage.saveWaypoint(uniqueId, json);
               this.onCloseAction.run();
            }
         }
      }).bounds(centerX - formWidth / 2, y, formWidth, 20).build());
      if (!this.isTabMode && this.hideFieldsUntilParentSelected() && this.selectedParentId == null && !this.isVanilla) {
         saveButton.active = false;
      }

   }
}
