package com.listraind.advancementwaypoints.advancement;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.ConfigIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;

public class LayoutCalculator {
   private final Map<String, Point> positions = new LinkedHashMap();
   private final Map<String, float[]> subtreeBounds = new HashMap();

   public void calculate(List<JsonObject> customEntries, AdvancementTree tree) {
      this.positions.clear();
      this.subtreeBounds.clear();
      if (!customEntries.isEmpty()) {
         Set<String> customIds = new HashSet();

         for(JsonObject o : customEntries) {
            customIds.add(ConfigIO.str(o, "id", ""));
         }

         Map<String, List<String>> childrenMap = new LinkedHashMap();
         Map<String, String> parentMap = new HashMap();
         List<String> roots = new ArrayList();

         for(JsonObject o : customEntries) {
            String id = ConfigIO.str(o, "id", "");
            String parent = ConfigIO.nullable(o, "parent");
            childrenMap.putIfAbsent(id, new ArrayList());
            if (parent != null && !parent.isEmpty()) {
               parentMap.put(id, parent);
               if (customIds.contains(parent)) {
                  ((List)childrenMap.computeIfAbsent(parent, (k) -> new ArrayList())).add(id);
               } else {
                  roots.add(id);
               }
            } else {
               roots.add(id);
            }
         }

         float nextAvailableY = 0.0F;

         for(String rootId : roots) {
            String parentId = (String)parentMap.get(rootId);
            float parentX = 0.0F;
            if (parentId != null) {
               AdvancementNode parentNode = tree.get(Identifier.parse(parentId));
               if (parentNode != null && parentNode.holder().value().display().isPresent()) {
                  DisplayInfo d = (DisplayInfo)parentNode.holder().value().display().get();
                  parentX = d.getX();
                  nextAvailableY = d.getY();
               }
            }

            int treeSize = this.subtreeSize(rootId, childrenMap);
            float rootY = nextAvailableY + (float)(treeSize - 1) / 2.0F;
            if (parentId != null) {
               float vanillaMax = this.getVanillaSubtreeMaxY(parentId, tree, customIds);
               if (vanillaMax > Float.NEGATIVE_INFINITY) {
                  float requiredStart = vanillaMax + 1.0F;
                  if (requiredStart > nextAvailableY) {
                     rootY = requiredStart + (float)(treeSize - 1) / 2.0F;
                  }
               }
            }

            this.positions.put(rootId, new Point(parentX + 1.0F, rootY));
            this.placeChildren(rootId, parentX + 1.0F, rootY, childrenMap, customIds, tree);
            float[] bounds = this.computeSubtreeBounds(rootId, childrenMap);
            this.subtreeBounds.put(rootId, bounds);
            nextAvailableY = bounds[1] + 1.5F;
         }

      }
   }

   private void placeChildren(String nodeId, float nodeX, float nodeY, Map<String, List<String>> childrenMap, Set<String> customIds, AdvancementTree tree) {
      List<String> children = (List)childrenMap.getOrDefault(nodeId, Collections.emptyList());
      if (!children.isEmpty()) {
         float childX = nodeX + 1.0F;
         int totalSize = 0;

         for(String child : children) {
            totalSize += this.subtreeSize(child, childrenMap);
         }

         float vanillaMax = this.getVanillaSubtreeMaxY(nodeId, tree, customIds);
         float blockStart = nodeY - (float)(totalSize - 1) / 2.0F;
         if (vanillaMax > Float.NEGATIVE_INFINITY) {
            float blockEnd = blockStart + (float)totalSize - 1.0F;
            float vanillaMin = this.getVanillaSubtreeMinY(nodeId, tree, customIds);
            if (vanillaMin < blockEnd && vanillaMax > blockStart) {
               blockStart = vanillaMax + 1.0F;
            }
         }

         float cursor = blockStart;

         for(String childId : children) {
            int size = this.subtreeSize(childId, childrenMap);
            float childY = cursor + (float)(size - 1) / 2.0F;
            this.positions.put(childId, new Point(childX, childY));
            this.placeChildren(childId, childX, childY, childrenMap, customIds, tree);
            cursor += (float)size;
         }

      }
   }

   private int subtreeSize(String nodeId, Map<String, List<String>> childrenMap) {
      List<String> children = (List)childrenMap.getOrDefault(nodeId, Collections.emptyList());
      if (children.isEmpty()) {
         return 1;
      } else {
         int size = 0;

         for(String child : children) {
            size += this.subtreeSize(child, childrenMap);
         }

         return Math.max(size, 1);
      }
   }

   private float[] computeSubtreeBounds(String nodeId, Map<String, List<String>> childrenMap) {
      Point pos = (Point)this.positions.get(nodeId);
      float min = pos != null ? pos.y() : 0.0F;
      float max = pos != null ? pos.y() : 0.0F;

      for(String child : childrenMap.getOrDefault(nodeId, Collections.emptyList())) {
         float[] childBounds = this.computeSubtreeBounds(child, childrenMap);
         min = Math.min(min, childBounds[0]);
         max = Math.max(max, childBounds[1]);
      }

      return new float[]{min, max};
   }

   private float getVanillaSubtreeMaxY(String parentId, AdvancementTree tree, Set<String> customIds) {
      AdvancementNode node = tree.get(Identifier.parse(parentId));
      return node == null ? Float.NEGATIVE_INFINITY : this.findExtremeY(node, customIds, true);
   }

   private float getVanillaSubtreeMinY(String parentId, AdvancementTree tree, Set<String> customIds) {
      AdvancementNode node = tree.get(Identifier.parse(parentId));
      return node == null ? Float.POSITIVE_INFINITY : this.findExtremeY(node, customIds, false);
   }

   private float findExtremeY(AdvancementNode node, Set<String> customIds, boolean findMax) {
      float extreme = findMax ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;

      for(AdvancementNode child : node.children()) {
         String childId = child.holder().id().toString();
         if (!customIds.contains(childId)) {
            if (child.holder().value().display().isPresent()) {
               float y = ((DisplayInfo)child.holder().value().display().get()).getY();
               extreme = findMax ? Math.max(extreme, y) : Math.min(extreme, y);
            }

            float childExtreme = this.findExtremeY(child, customIds, findMax);
            extreme = findMax ? Math.max(extreme, childExtreme) : Math.min(extreme, childExtreme);
         }
      }

      return extreme;
   }

   public Map<String, Point> getPositions() {
      return this.positions;
   }

   public static record Point(float x, float y) {
   }
}
