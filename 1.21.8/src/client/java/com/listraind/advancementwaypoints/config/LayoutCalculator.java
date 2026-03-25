package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class LayoutCalculator {

    private static final float STEP_X = 1.0f;
    private static final float STEP_Y = 1.0f;
    private static final float MIN_DIST_X = 0.9f;
    private static final float MIN_DIST_Y = 0.9f;

    private final Map<String, float[]> vanillaOverrides = new HashMap<>();
    private final Map<String, float[]> computedPositions = new LinkedHashMap<>();

    public Map<String, float[]> getVanillaOverrides() {
        return vanillaOverrides;
    }

    public Map<String, float[]> getComputedPositions() {
        return computedPositions;
    }

    private final List<float[]> allOccupied = new ArrayList<>();

    public void calculate(List<JsonObject> customAdvancements, AdvancementTree tree) {
        vanillaOverrides.clear();
        computedPositions.clear();
        allOccupied.clear();

        if (customAdvancements.isEmpty()) return;

        Map<String, JsonObject> byId = new LinkedHashMap<>();
        for (JsonObject obj : customAdvancements) {
            String id = obj.has("id") ? obj.get("id").getAsString() : "";
            if (!id.isEmpty()) byId.put(id, obj);
        }

        Map<String, List<String>> childrenMap = new LinkedHashMap<>();
        Set<String> roots = new LinkedHashSet<>();
        Map<String, String> parentMap = new HashMap<>();

        for (JsonObject obj : customAdvancements) {
            String id = obj.has("id") ? obj.get("id").getAsString() : "";
            String parentStr = obj.has("parent") && !obj.get("parent").isJsonNull()
                    ? obj.get("parent").getAsString() : "";

            parentMap.put(id, parentStr);

            if (parentStr.isEmpty() || !byId.containsKey(parentStr)) {
                roots.add(id);
            } else {
                childrenMap.computeIfAbsent(parentStr, k -> new ArrayList<>()).add(id);
            }
        }

        Map<String, float[]> knownPositions = new HashMap<>();

        for (String rootId : roots) {
            JsonObject rootObj = byId.get(rootId);
            String parentStr = parentMap.getOrDefault(rootId, "");

            String treeRootId = findRootId(parentStr.isEmpty() ? rootId : parentStr, tree);
            if (!treeRootId.isEmpty()) {
                collectAllVanillaPositions(treeRootId, tree, knownPositions);
            }
        }

        for (float[] pos : knownPositions.values()) {
            allOccupied.add(pos);
        }

        for (String rootId : roots) {
            String parentStr = parentMap.getOrDefault(rootId, "");

            float parentX = 0f;
            float parentY = 0f;

            if (!parentStr.isEmpty()) {
                float[] pp = resolvePosition(parentStr, tree, knownPositions);
                if (pp != null) {
                    parentX = pp[0];
                    parentY = pp[1];
                }
            }

            layoutSubtree(rootId, parentX, parentY, childrenMap, knownPositions, tree);
        }
    }

    private void layoutSubtree(String rootId, float anchorX, float anchorY,
                               Map<String, List<String>> childrenMap,
                               Map<String, float[]> knownPositions, AdvancementTree tree) {

        Map<String, Integer> subtreeWidths = new HashMap<>();
        measureAllWidths(rootId, childrenMap, subtreeWidths);

        layoutNodeRecursive(rootId, anchorX + STEP_X, anchorY, childrenMap, subtreeWidths, knownPositions, tree);
    }

    private void layoutNodeRecursive(String nodeId, float desiredX, float desiredY,
                                     Map<String, List<String>> childrenMap,
                                     Map<String, Integer> subtreeWidths,
                                     Map<String, float[]> knownPositions, AdvancementTree tree) {

        float x = desiredX;
        float y = desiredY;

        while (isOccupied(x, y)) {
            x += STEP_X;
        }

        List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());

        if (!children.isEmpty()) {
            boolean needShift = true;
            while (needShift) {
                needShift = false;
                int totalWidth = subtreeWidths.getOrDefault(nodeId, 1);
                float blockStartY = y - (totalWidth - 1) * STEP_Y / 2.0f;

                for (int i = 0; i < totalWidth; i++) {
                    float checkY = blockStartY + i * STEP_Y;
                    if (isOccupied(x + STEP_X, checkY)) {
                        x += STEP_X;
                        while (isOccupied(x, y)) {
                            x += STEP_X;
                        }
                        needShift = true;
                        break;
                    }
                }
            }
        }

        float[] pos = new float[]{x, y};
        computedPositions.put(nodeId, pos);
        knownPositions.put(nodeId, pos);
        allOccupied.add(pos);

        if (children.isEmpty()) return;

        int totalChildLeaves = 0;
        Map<String, Integer> childWidthMap = new LinkedHashMap<>();
        for (String childId : children) {
            int w = subtreeWidths.getOrDefault(childId, 1);
            childWidthMap.put(childId, w);
            totalChildLeaves += w;
        }

        float childBlockStartY = y - (totalChildLeaves - 1) * STEP_Y / 2.0f;
        float childX = x + STEP_X;

        float currentY = childBlockStartY;
        for (String childId : children) {
            int childW = childWidthMap.get(childId);
            float childCenterY = currentY + (childW - 1) * STEP_Y / 2.0f;

            layoutNodeRecursive(childId, childX, childCenterY, childrenMap, subtreeWidths, knownPositions, tree);

            currentY += childW * STEP_Y;
        }
    }

    private void measureAllWidths(String nodeId, Map<String, List<String>> childrenMap,
                                  Map<String, Integer> result) {
        List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());
        if (children.isEmpty()) {
            result.put(nodeId, 1);
            return;
        }

        int total = 0;
        for (String childId : children) {
            measureAllWidths(childId, childrenMap, result);
            total += result.get(childId);
        }
        result.put(nodeId, total);
    }

    private boolean isOccupied(float x, float y) {
        for (float[] pos : allOccupied) {
            if (Math.abs(pos[0] - x) < MIN_DIST_X && Math.abs(pos[1] - y) < MIN_DIST_Y) {
                return true;
            }
        }
        return false;
    }

    private float[] resolvePosition(String id, AdvancementTree tree, Map<String, float[]> knownPositions) {
        if (computedPositions.containsKey(id)) return computedPositions.get(id);
        if (knownPositions.containsKey(id)) return knownPositions.get(id);

        try {
            AdvancementNode node = tree.get(ResourceLocation.parse(id));
            if (node != null) {
                float[][] result = new float[1][];
                node.holder().value().display().ifPresent(d -> {
                    result[0] = new float[]{d.getX(), d.getY()};
                    knownPositions.put(id, result[0]);
                });
                return result[0];
            }
        } catch (Exception ignored) {}

        return null;
    }

    private void collectAllVanillaPositions(String rootId, AdvancementTree tree,
                                            Map<String, float[]> positions) {
        try {
            AdvancementNode rootNode = tree.get(ResourceLocation.parse(rootId));
            if (rootNode != null) {
                collectRecursive(rootNode, positions);
            }
        } catch (Exception ignored) {}
    }

    private void collectRecursive(AdvancementNode node, Map<String, float[]> positions) {
        node.holder().value().display().ifPresent(d -> {
            positions.put(node.holder().id().toString(), new float[]{d.getX(), d.getY()});
        });

        for (AdvancementNode child : node.children()) {
            collectRecursive(child, positions);
        }
    }

    private String findRootId(String parentId, AdvancementTree tree) {
        if (parentId == null || parentId.isEmpty()) return "";

        try {
            AdvancementNode node = tree.get(ResourceLocation.parse(parentId));
            if (node == null) return parentId;

            AdvancementNode current = node;
            while (current.parent() != null) {
                current = current.parent();
            }
            return current.holder().id().toString();
        } catch (Exception e) {
            return parentId;
        }
    }
}