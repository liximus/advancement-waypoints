package com.listraind.advancementwaypoints.advancement;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.ConfigIO;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class LayoutCalculator {

    private final Map<String, float[]> positions = new LinkedHashMap<>();
    private final Map<String, float[]> vanillaShifts = new LinkedHashMap<>();

    public void calculate(List<JsonObject> customEntries, AdvancementTree tree) {
        positions.clear();
        vanillaShifts.clear();

        Set<String> customIds = new HashSet<>();
        for (JsonObject o : customEntries) {
            customIds.add(ConfigIO.str(o, "id", ""));
        }

        Map<String, List<JsonObject>> byParent = new LinkedHashMap<>();
        List<JsonObject> roots = new ArrayList<>();

        for (JsonObject o : customEntries) {
            String parent = ConfigIO.nullable(o, "parent");
            if (parent == null || parent.isEmpty()) {
                roots.add(o);
            } else {
                byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(o);
            }
        }

        for (JsonObject root : roots) {
            String id = ConfigIO.str(root, "id", "");
            positions.put(id, new float[]{0f, 0f});
        }

        for (var entry : byParent.entrySet()) {
            String parentId = entry.getKey();
            List<JsonObject> children = entry.getValue();

            float parentX = 0, parentY = 0;

            if (positions.containsKey(parentId)) {
                float[] pp = positions.get(parentId);
                parentX = pp[0];
                parentY = pp[1];
            } else {
                AdvancementNode parentNode = tree.get(ResourceLocation.parse(parentId));
                if (parentNode != null && parentNode.holder().value().display().isPresent()) {
                    var d = parentNode.holder().value().display().get();
                    parentX = d.getX();
                    parentY = d.getY();
                }
            }

            float maxChildY = findMaxChildY(parentId, tree, customIds);

            for (JsonObject prev : customEntries) {
                String prevId = ConfigIO.str(prev, "id", "");
                String prevParent = ConfigIO.nullable(prev, "parent");
                if (parentId.equals(prevParent) && positions.containsKey(prevId)) {
                    maxChildY = Math.max(maxChildY, positions.get(prevId)[1]);
                }
            }

            float childX = parentX + 1.0f;
            int count = children.size();

            boolean hasVanilla = hasVanillaChildren(parentId, tree, customIds);
            boolean hasExisting = hasVanilla;
            if (!hasExisting) {
                for (JsonObject prev : customEntries) {
                    String prevParent = ConfigIO.nullable(prev, "parent");
                    String prevId = ConfigIO.str(prev, "id", "");
                    if (parentId.equals(prevParent) && positions.containsKey(prevId)) {
                        hasExisting = true;
                        break;
                    }
                }
            }

            if (!hasExisting) {
                float startY = parentY - (count - 1) / 2.0f;
                for (int i = 0; i < count; i++) {
                    String id = ConfigIO.str(children.get(i), "id", "");
                    if (!positions.containsKey(id)) {
                        positions.put(id, new float[]{childX, startY + i});
                    }
                }
            } else {
                float startY = maxChildY + 1.0f;
                for (int i = 0; i < count; i++) {
                    String id = ConfigIO.str(children.get(i), "id", "");
                    if (!positions.containsKey(id)) {
                        positions.put(id, new float[]{childX, startY + i});
                    }
                }
            }
        }
    }

    private float findMaxChildY(String parentId, AdvancementTree tree, Set<String> customIds) {
        AdvancementNode parentNode = tree.get(ResourceLocation.parse(parentId));
        if (parentNode == null) return Float.NEGATIVE_INFINITY;

        float max = Float.NEGATIVE_INFINITY;
        for (AdvancementNode child : parentNode.children()) {
            String childId = child.holder().id().toString();
            if (customIds.contains(childId)) continue;
            if (child.holder().value().display().isPresent()) {
                max = Math.max(max, child.holder().value().display().get().getY());
            }
            max = Math.max(max, findMaxDescendantY(child, customIds));
        }
        return max;
    }

    private float findMaxDescendantY(AdvancementNode node, Set<String> customIds) {
        String nodeId = node.holder().id().toString();
        if (customIds.contains(nodeId)) return Float.NEGATIVE_INFINITY;

        float max = node.holder().value().display()
                .map(d -> d.getY()).orElse(Float.NEGATIVE_INFINITY);
        for (AdvancementNode child : node.children()) {
            String childId = child.holder().id().toString();
            if (customIds.contains(childId)) continue;
            max = Math.max(max, findMaxDescendantY(child, customIds));
        }
        return max;
    }

    private boolean hasVanillaChildren(String parentId, AdvancementTree tree, Set<String> customIds) {
        AdvancementNode parentNode = tree.get(ResourceLocation.parse(parentId));
        if (parentNode == null) return false;
        for (AdvancementNode child : parentNode.children()) {
            String childId = child.holder().id().toString();
            if (!customIds.contains(childId)) return true;
        }
        return false;
    }

    public Map<String, float[]> getPositions() { return positions; }
    public Map<String, float[]> getVanillaShifts() { return vanillaShifts; }
}