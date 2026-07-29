package com.listraind.advancementwaypoints.gui.dialogs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class EditWaypointScreen extends WaypointFormScreen {
   private final JsonObject data;
   private final String fullId;
   private final String initialParentId;

   public EditWaypointScreen(JsonObject data) {
      super(Component.translatable("advwp.screen.edit_waypoint"));
      this.data = data;
      this.fullId = data.has("id") && !data.get("id").isJsonNull() ? data.get("id").getAsString() : "";
      this.isVanilla = !this.fullId.startsWith("advwaypoints:");
      JsonObject display = data.has("display") && data.get("display").isJsonObject() ? data.getAsJsonObject("display") : data;
      if (display.has("title") && !display.get("title").isJsonNull()) {
         JsonElement t = display.get("title");
         this.savedName = t.isJsonObject() ? this.parseTextFromComponentJson(t.getAsJsonObject()) : t.getAsString();
      }

      if (display.has("icon") && !display.get("icon").isJsonNull()) {
         JsonElement iconEl = display.get("icon");
         String iconStr = null;
         if (iconEl.isJsonPrimitive()) {
            iconStr = iconEl.getAsString();
         } else if (iconEl.isJsonObject()) {
            JsonObject iconObj = iconEl.getAsJsonObject();
            if (iconObj.has("id")) {
               iconStr = iconObj.get("id").getAsString();
            } else if (iconObj.has("item")) {
               iconStr = iconObj.get("item").getAsString();
            }
         }

         if (iconStr != null && !iconStr.isEmpty()) {
            try {
               Identifier iconId = Identifier.parse(iconStr);
               this.selectedIcon = (Item)BuiltInRegistries.ITEM.getValue(iconId);
            } catch (Exception var7) {
            }
         }
      }

      if (display.has("description") && !display.get("description").isJsonNull()) {
         JsonElement d = display.get("description");
         this.savedDesc = d.isJsonObject() ? this.parseTextFromComponentJson(d.getAsJsonObject()) : d.getAsString();
      }

      if (display.has("frame") && !display.get("frame").isJsonNull()) {
         this.savedFrame = display.get("frame").getAsString();
      } else if (data.has("frame") && !data.get("frame").isJsonNull()) {
         this.savedFrame = data.get("frame").getAsString();
      }

      if (display.has("background") && !display.get("background").isJsonNull()) {
         this.savedBackground = display.get("background").getAsString();
      }

      if (data.has("parent") && !data.get("parent").isJsonNull()) {
         String pStr = data.get("parent").getAsString();
         if (!pStr.isEmpty()) {
            try {
               this.selectedParentId = Identifier.parse(pStr);
            } catch (Exception var6) {
            }
         }
      }

      this.initialParentId = this.selectedParentId != null ? this.selectedParentId.toString() : null;
      this.parseCoordsFromDesc();
   }

   private String parseTextFromComponentJson(JsonObject obj) {
      if (obj.has("text")) {
         return obj.get("text").getAsString();
      } else {
         return obj.has("translate") ? obj.get("translate").getAsString() : obj.toString();
      }
   }

   protected boolean showParentField() {
      return this.selectedParentId != null || !this.isVanilla;
   }

   protected boolean showBackgroundField() {
      return this.isRoot();
   }

   protected boolean showCoordsField() {
      return true;
   }

   private void parseCoordsFromDesc() {
      if (this.savedDesc != null && !this.savedDesc.isEmpty()) {
         for(CoordParser.DimCoords dimCoords : CoordParser.parseAllCoords(this.savedDesc)) {
            for(String[] coords : dimCoords.coords()) {
               this.coordRows.add(new WaypointFormScreen.CoordRow(dimCoords.dim(), coords[0], coords[1], coords[2]));
            }
         }

         this.savedDesc = CoordParser.extractExtra(this.savedDesc);
      }
   }

   protected void initActions(int centerX, int y) {
      int formWidth = this.panelWidth - 40;
      Button actionButton = (Button)this.addRenderableWidget(Button.builder(this.isVanilla ? Component.translatable("advwp.button.read_only") : Component.translatable("advwp.action.save"), (b) -> {
         if (!this.isVanilla) {
            this.saveState();
            String titleVal = this.colorCodes(this.nameField != null ? this.nameField.getValue().trim() : this.savedName);
            String newParentVal = this.selectedParentId != null ? this.selectedParentId.toString() : null;
            String bgVal = this.showBackgroundField() ? this.getBackgroundValue() : (this.savedBackground != null && !this.savedBackground.isEmpty() ? this.savedBackground : null);
            boolean parentChanged = !Objects.equals(this.initialParentId, newParentVal);
            List<JsonObject> children = WaypointStorage.getChildren(this.fullId);
            if (parentChanged && !children.isEmpty()) {
               this.minecraft.gui.setScreen(new ConfirmParentChangeScreen(this, () -> {
                  WaypointStorage.reparentChildren(this.fullId, this.initialParentId);
                  JsonObject updated = WaypointStorage.buildWaypointJson(this.fullId, titleVal, this.iconId(), this.buildFinalDescription(), this.savedFrame, newParentVal, bgVal);
                  WaypointStorage.saveWaypoint(this.fullId, updated);
                  this.onCloseAction.run();
               }, () -> {
                  JsonObject updated = WaypointStorage.buildWaypointJson(this.fullId, titleVal, this.iconId(), this.buildFinalDescription(), this.savedFrame, newParentVal, bgVal);
                  WaypointStorage.saveWaypoint(this.fullId, updated);
                  this.onCloseAction.run();
               }));
            } else {
               JsonObject updated = WaypointStorage.buildWaypointJson(this.fullId, titleVal, this.iconId(), this.buildFinalDescription(), this.savedFrame, newParentVal, bgVal);
               WaypointStorage.saveWaypoint(this.fullId, updated);
               this.onCloseAction.run();
            }

         }
      }).bounds(centerX - formWidth / 2, y, formWidth, 20).build());
      if (this.isVanilla) {
         actionButton.active = false;
      }

   }
}
