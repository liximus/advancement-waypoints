package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class WaypointStorage {
   private static Identifier lastParent = null;
   private static Set<String> hiddenBranchesCache = null;
   private static Set<String> hiddenTabsCache = null;

   private static Path waypointsFolder() {
      return ConfigIO.worldDir().resolve("custom_advancements");
   }

   private static Path mainFile() {
      return waypointsFolder().resolve("custom_advancements.json");
   }

   private static Path overridesPath() {
      return ConfigIO.worldDir().resolve("overrides.json");
   }

   private static Path findFileContaining(String id) {
      if (!Files.exists(waypointsFolder(), new LinkOption[0])) {
         return null;
      } else {
         List<Path> jsonFiles;
         try {
            Stream<Path> stream = Files.list(waypointsFolder());

            try {
               jsonFiles = (List)stream.filter((p) -> p.toString().endsWith(".json")).collect(Collectors.toList());
            } catch (Throwable var6) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception var7) {
            return null;
         }

         for(Path file : jsonFiles) {
            List<JsonObject> contents = ConfigIO.readArray(file);
            if (contents.stream().anyMatch((o) -> id.equals(ConfigIO.str(o, "id", "")))) {
               return file;
            }
         }

         return null;
      }
   }

   public static List<JsonObject> loadWaypoints() {
      return ConfigIO.readAllInFolder(waypointsFolder());
   }

   public static List<JsonObject> loadOverrides() {
      return ConfigIO.readArray(overridesPath());
   }

   public static void saveOverrides(List<JsonObject> list) {
      ConfigIO.writeArray(overridesPath(), list);
   }

   public static void saveOrUpdateWaypoint(JsonObject data) {
      String id = ConfigIO.str(data, "id", "");
      if (!id.isEmpty()) {
         Path file = findFileContaining(id);
         if (file == null) {
            file = mainFile();
         }

         List<JsonObject> fileContents = (List<JsonObject>)(Files.exists(file, new LinkOption[0]) ? ConfigIO.readArray(file) : new ArrayList());
         int existingIndex = -1;

         for(int i = 0; i < fileContents.size(); ++i) {
            if (id.equals(ConfigIO.str((JsonObject)fileContents.get(i), "id", ""))) {
               existingIndex = i;
               break;
            }
         }

         if (existingIndex >= 0) {
            fileContents.set(existingIndex, data);
         } else {
            fileContents.add(data);
         }

         ConfigIO.writeArray(file, fileContents);
         AdvancementWaypointsClient.reloadAdvancements();
      }
   }

   public static List<JsonObject> getChildren(String parentId) {
      List<JsonObject> result = new ArrayList();
      if (parentId != null && !parentId.isEmpty()) {
         for(JsonObject o : loadWaypoints()) {
            String p = ConfigIO.nullable(o, "parent");
            if (parentId.equals(p)) {
               result.add(o);
            }
         }

         return result;
      } else {
         return result;
      }
   }

   public static boolean hasChildren(Identifier id) {
      if (id == null) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            AdvancementNode node = mc.player.connection.getAdvancements().getTree().get(id);
            if (node != null && node.children() != null && node.children().iterator().hasNext()) {
               return true;
            }
         }

         List<JsonObject> customChildren = getChildren(id.toString());
         return customChildren != null && !customChildren.isEmpty();
      }
   }

   public static void reparentChildren(String oldParentId, String newParentId) {
      if (oldParentId != null && !oldParentId.isEmpty()) {
         Path folder = waypointsFolder();
         if (Files.exists(folder, new LinkOption[0])) {
            List<Path> jsonFiles;
            try {
               Stream<Path> stream = Files.list(folder);

               try {
                  jsonFiles = (List)stream.filter((px) -> px.toString().endsWith(".json")).collect(Collectors.toList());
               } catch (Throwable var12) {
                  if (stream != null) {
                     try {
                        stream.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (stream != null) {
                  stream.close();
               }
            } catch (Exception var13) {
               return;
            }

            for(Path file : jsonFiles) {
               List<JsonObject> contents = ConfigIO.readArray(file);
               boolean changed = false;

               for(JsonObject obj : contents) {
                  String p = ConfigIO.nullable(obj, "parent");
                  if (oldParentId.equals(p)) {
                     if (newParentId != null && !newParentId.isEmpty()) {
                        obj.addProperty("parent", newParentId);
                     } else {
                        obj.remove("parent");
                     }

                     changed = true;
                  }
               }

               if (changed) {
                  ConfigIO.writeArray(file, contents);
               }
            }

         }
      }
   }

   public static void deleteWaypoint(String id) {
      Path folder = waypointsFolder();
      if (Files.exists(folder, new LinkOption[0])) {
         List<Path> jsonFiles;
         try {
            Stream<Path> stream = Files.list(folder);

            try {
               jsonFiles = (List)stream.filter((px) -> px.toString().endsWith(".json")).collect(Collectors.toList());
            } catch (Throwable var13) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var11) {
                     var13.addSuppressed(var11);
                  }
               }

               throw var13;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception var14) {
            return;
         }

         String oldParent = null;

         for(Path file : jsonFiles) {
            for(JsonObject o : ConfigIO.readArray(file)) {
               if (id.equals(ConfigIO.str(o, "id", ""))) {
                  oldParent = ConfigIO.nullable(o, "parent");
                  break;
               }
            }

            if (oldParent != null) {
               break;
            }
         }

         for(Path file : jsonFiles) {
            List<JsonObject> contents = ConfigIO.readArray(file);
            boolean changed = false;

            for(JsonObject obj : contents) {
               String p = ConfigIO.nullable(obj, "parent");
               if (id.equals(p)) {
                  if (oldParent != null && !oldParent.isEmpty()) {
                     obj.addProperty("parent", oldParent);
                  } else {
                     obj.remove("parent");
                  }

                  changed = true;
               }
            }

            int sizeBefore = contents.size();
            contents.removeIf((ox) -> id.equals(ConfigIO.str(ox, "id", "")));
            if (contents.size() != sizeBefore) {
               changed = true;
            }

            if (changed) {
               if (contents.isEmpty()) {
                  try {
                     Files.deleteIfExists(file);
                  } catch (Exception var12) {
                  }
               } else {
                  ConfigIO.writeArray(file, contents);
               }
            }
         }

         AdvancementWaypointsClient.reloadAdvancements();
      }
   }

   public static JsonObject getWaypointOrVanilla(Identifier id) {
      for(JsonObject obj : loadWaypoints()) {
         if (id.toString().equals(ConfigIO.str(obj, "id", ""))) {
            return obj;
         }
      }

      JsonObject result = new JsonObject();
      result.addProperty("id", id.toString());
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         AdvancementNode node = mc.player.connection.getAdvancements().getTree().get(id);
         if (node != null && node.holder().value().display().isPresent()) {
            DisplayInfo display = (DisplayInfo)node.holder().value().display().get();
            JsonObject displayObj = new JsonObject();
            displayObj.addProperty("title", display.getTitle().getString());
            displayObj.addProperty("description", display.getDescription().getString());
            Identifier iconId = BuiltInRegistries.ITEM.getKey((Item)display.getIcon().item().value());
            displayObj.addProperty("icon", iconId != null ? iconId.toString() : "minecraft:stone");
            result.add("display", displayObj);
            if (node.parent() != null) {
               result.addProperty("parent", node.parent().holder().id().toString());
            }
         }
      }

      return result;
   }

   public static void saveOverride(JsonObject data) {
      List<JsonObject> overrides = loadOverrides();
      String id = ConfigIO.str(data, "id", "");
      if (!id.isEmpty()) {
         JsonObject existing = null;

         for(JsonObject o : overrides) {
            if (id.equals(ConfigIO.str(o, "id", ""))) {
               existing = o;
               break;
            }
         }

         if (existing != null) {
            for(Map.Entry<String, JsonElement> entry : data.entrySet()) {
               existing.add((String)entry.getKey(), (JsonElement)entry.getValue());
            }
         } else {
            overrides.add(data);
         }

         saveOverrides(overrides);
      }
   }

   public static Identifier getLastParent() {
      return lastParent;
   }

   public static void setLastParent(Identifier parent) {
      lastParent = parent;
   }

   public static String generateUniqueId(String namespace) {
      String ns = namespace != null && !namespace.isEmpty() && !namespace.equals("minecraft") ? namespace : "custom";
      return "advwaypoints:" + ns + "_" + System.currentTimeMillis();
   }

   public static JsonObject buildWaypointJson(String id, String title, String icon, String desc, String frame, String parent, String bg) {
      JsonObject json = new JsonObject();
      json.addProperty("id", id);
      if (parent != null && !parent.isEmpty()) {
         json.addProperty("parent", parent);
      }

      JsonObject display = new JsonObject();
      display.addProperty("title", title);
      display.addProperty("icon", icon);
      display.addProperty("description", desc);
      if (frame != null && !frame.isEmpty()) {
         display.addProperty("frame", frame);
      }
      if (bg != null && !bg.isEmpty()) {
         display.addProperty("background", bg);
      }

      json.add("display", display);
      return json;
   }

   public static JsonObject buildWaypointJson(String id, String title, String icon, String desc, String parent, String bg) {
      return buildWaypointJson(id, title, icon, desc, "task", parent, bg);
   }

   public static void saveWaypoint(String id, JsonObject data) {
      saveOrUpdateWaypoint(data);
   }

   private static Path hiddenBranchesPath() {
      return ConfigIO.worldDir().resolve("hidden_branches.json");
   }

   public static Set<String> loadHiddenBranches() {
      if (hiddenBranchesCache != null) {
         return hiddenBranchesCache;
      } else {
         hiddenBranchesCache = new HashSet();
         Path path = hiddenBranchesPath();
         if (Files.exists(path, new LinkOption[0])) {
            for(JsonObject obj : ConfigIO.readArray(path)) {
               String id = ConfigIO.str(obj, "id", "");
               if (!id.isEmpty()) {
                  hiddenBranchesCache.add(id);
               }
            }
         }

         return hiddenBranchesCache;
      }
   }

   public static boolean isBranchHidden(String id) {
      return id != null && !id.isEmpty() ? loadHiddenBranches().contains(id) : false;
   }

   public static void toggleBranchHidden(String id) {
      if (id != null && !id.isEmpty()) {
         Set<String> set = loadHiddenBranches();
         if (set.contains(id)) {
            set.remove(id);
         } else {
            set.add(id);
         }

         saveHiddenBranches(set);
      }
   }

   public static void setBranchHidden(String id, boolean hidden) {
      if (id != null && !id.isEmpty()) {
         Set<String> set = loadHiddenBranches();
         if (hidden) {
            set.add(id);
         } else {
            set.remove(id);
         }

         saveHiddenBranches(set);
      }
   }

   private static void saveHiddenBranches(Set<String> set) {
      hiddenBranchesCache = new HashSet(set);
      List<JsonObject> list = new ArrayList();

      for(String id : set) {
         JsonObject obj = new JsonObject();
         obj.addProperty("id", id);
         list.add(obj);
      }

      ConfigIO.writeArray(hiddenBranchesPath(), list);
   }

   public static boolean isNodeHidden(AdvancementNode node) {
      if (node == null) {
         return false;
      } else {
         for(AdvancementNode parent = node.parent(); parent != null; parent = parent.parent()) {
            String parentId = parent.holder().id().toString();
            if (isBranchHidden(parentId)) {
               return true;
            }
         }

         return false;
      }
   }

   private static Path hiddenTabsPath() {
      return ConfigIO.worldDir().resolve("hidden_tabs.json");
   }

   public static Set<String> loadHiddenTabs() {
      if (hiddenTabsCache != null) {
         return hiddenTabsCache;
      } else {
         hiddenTabsCache = new HashSet();
         Path path = hiddenTabsPath();
         if (Files.exists(path, new LinkOption[0])) {
            for(JsonObject obj : ConfigIO.readArray(path)) {
               String id = ConfigIO.str(obj, "id", "");
               if (!id.isEmpty()) {
                  hiddenTabsCache.add(id);
               }
            }
         }

         return hiddenTabsCache;
      }
   }

   public static boolean isTabHidden(String tabRootId) {
      return tabRootId != null && !tabRootId.isEmpty() ? loadHiddenTabs().contains(tabRootId) : false;
   }

   public static void setTabHidden(String tabRootId, boolean hidden) {
      if (tabRootId != null && !tabRootId.isEmpty()) {
         Set<String> set = loadHiddenTabs();
         if (hidden) {
            set.add(tabRootId);
         } else {
            set.remove(tabRootId);
         }

         saveHiddenTabs(set);
      }
   }

   private static void saveHiddenTabs(Set<String> set) {
      hiddenTabsCache = new HashSet(set);
      List<JsonObject> list = new ArrayList();

      for(String id : set) {
         JsonObject obj = new JsonObject();
         obj.addProperty("id", id);
         list.add(obj);
      }

      ConfigIO.writeArray(hiddenTabsPath(), list);
   }
}
