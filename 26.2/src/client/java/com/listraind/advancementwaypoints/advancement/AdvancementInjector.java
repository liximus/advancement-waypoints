package com.listraind.advancementwaypoints.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementTree;

public class AdvancementInjector {
   public static LoadResult load(AdvancementTree tree) {
      List<JsonObject> custom = WaypointStorage.loadWaypoints();
      if (custom.isEmpty()) {
         return new LoadResult(List.of(), Map.of());
      } else {
         LayoutCalculator calc = new LayoutCalculator();
         calc.calculate(custom, tree);
         Map<String, LayoutCalculator.Point> pos = calc.getPositions();
         List<WaypointData> result = new ArrayList();

         for(JsonObject o : custom) {
            String id = ConfigIO.str(o, "id", "");
            LayoutCalculator.Point p = (LayoutCalculator.Point)pos.getOrDefault(id, new LayoutCalculator.Point(0.0F, 0.0F));
            String parent = ConfigIO.nullable(o, "parent");
            JsonObject display = o.has("display") && o.get("display").isJsonObject() ? o.getAsJsonObject("display") : o;
            String icon = parseStringProperty(display, "icon");
            if (icon == null) {
               icon = parseStringProperty(o, "icon");
            }

            if (icon == null || icon.isEmpty()) {
               icon = "minecraft:paper";
            }

            String title = parseStringProperty(display, "title");
            if (title == null) {
               title = parseStringProperty(o, "title");
            }

            if (title == null) {
               title = "???";
            }

            String desc = parseStringProperty(display, "description");
            if (desc == null) {
               desc = parseStringProperty(o, "description");
            }

            if (desc == null) {
               desc = "";
            }

            String bg = ConfigIO.nullable(display, "background");
            if (bg == null) {
               bg = ConfigIO.nullable(o, "background");
            }

            boolean isRoot = parent == null || parent.isEmpty();
            if (isRoot && (bg == null || bg.isEmpty())) {
               bg = "minecraft:torch";
            }

            result.add(new WaypointData(id, icon, title, desc, ConfigIO.str(o, "frame", "task"), bg, parent, p.x(), p.y()));
         }

         return new LoadResult(result, Map.of());
      }
   }

   private static String parseStringProperty(JsonObject obj, String key) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         JsonElement el = obj.get(key);
         if (el.isJsonPrimitive()) {
            return el.getAsString();
         } else {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               if (o.has("id")) {
                  return o.get("id").getAsString();
               }

               if (o.has("item")) {
                  return o.get("item").getAsString();
               }

               if (o.has("text")) {
                  return o.get("text").getAsString();
               }

               if (o.has("translate")) {
                  return o.get("translate").getAsString();
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static record LoadResult(List<WaypointData> advancements, Map<String, float[]> vanillaOverrides) {
   }
}
