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
    private final Map<String, float[]> subtreeBounds = new HashMap<>();

    public void calculate(List<JsonObject> customEntries, AdvancementTree tree) {
        positions.clear();
        vanillaShifts.clear();
        subtreeBounds.clear();

        if (customEntries.isEmpty()) return;

        Set<String> customIds = new HashSet<>();
        for (JsonObject o : customEntries) {
            customIds.add(ConfigIO.str(o, "id", ""));
        }

        Map<String, List<String>> childrenMap = new LinkedHashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        List<String> roots = new ArrayList<>();

        for (JsonObject o : customEntries) {
            String id = ConfigIO.str(o, "id", "");
            String parent = ConfigIO.nullable(o, "parent");
            childrenMap.putIfAbsent(id, new ArrayList<>());

            if (parent == null || parent.isEmpty()) {
                roots.add(id);
            } else {
                parentMap.put(id, parent);
                if (customIds.contains(parent)) {
                    childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(id);
                } else {
                    roots.add(id);
                }
            }
        }

        float[] nextAvailableY = {0f};

        for (String rootId : roots) {
            String parentId = parentMap.get(rootId);
            float parentX = 0f;

            if (parentId != null) {
                AdvancementNode parentNode = tree.get(ResourceLocation.parse(parentId));
                if (parentNode != null && parentNode.holder().value().display().isPresent()) {
                    var d = parentNode.holder().value().display().get();
                    parentX = d.getX();
                    nextAvailableY[0] = d.getY();
                }
            }

            int treeSize = subtreeSize(rootId, childrenMap);
            float startY = nextAvailableY[0];
            float rootY = startY + (treeSize - 1) / 2.0f;

            if (parentId != null) {
                float vanillaMaxY = getVanillaSubtreeMaxY(parentId, tree, customIds);
                if (vanillaMaxY > Float.NEGATIVE_INFINITY) {
                    float requiredStart = vanillaMaxY + 1.0f;
                    if (requiredStart > startY) {
                        startY = requiredStart;
                        rootY = startY + (treeSize - 1) / 2.0f;
                    }
                }
            }

            positions.put(rootId, new float[]{parentX + 1.0f, rootY});
            placeChildren(rootId, parentX + 1.0f, rootY, childrenMap, customIds, tree);

            float[] bounds = computeSubtreeBounds(rootId, childrenMap);
            subtreeBounds.put(rootId, bounds);

            nextAvailableY[0] = bounds[1] + 1.5f;
        }
    }

    private void placeChildren(String nodeId, float nodeX, float nodeY,
                               Map<String, List<String>> childrenMap,
                               Set<String> customIds,
                               AdvancementTree tree) {
        List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());
        if (children.isEmpty()) return;

        float childX = nodeX + 1.0f;

        int totalSize = 0;
        for (String child : children) {
            totalSize += subtreeSize(child, childrenMap);
        }

        float vanillaMax = getVanillaSubtreeMaxY(nodeId, tree, customIds);
        float vanillaMin = getVanillaSubtreeMinY(nodeId, tree, customIds);

        float blockStart = nodeY - (totalSize - 1) / 2.0f;

        if (vanillaMax > Float.NEGATIVE_INFINITY) {
            float blockEnd = blockStart + totalSize - 1;
            if (vanillaMin < blockEnd && vanillaMax > blockStart) {
                blockStart = vanillaMax + 1.0f;
            }
        }

        float cursor = blockStart;
        for (String childId : children) {
            int size = subtreeSize(childId, childrenMap);
            float childY = cursor + (size - 1) / 2.0f;
            positions.put(childId, new float[]{childX, childY});
            placeChildren(childId, childX, childY, childrenMap, customIds, tree);
            cursor += size;
        }
    }

    private int subtreeSize(String nodeId, Map<String, List<String>> childrenMap) {
        List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());
        if (children.isEmpty()) return 1;
        int size = 0;
        for (String child : children) {
            size += subtreeSize(child, childrenMap);
        }
        return Math.max(size, 1);
    }

    private float[] computeSubtreeBounds(String nodeId, Map<String, List<String>> childrenMap) {
        float[] pos = positions.get(nodeId);
        float min = pos != null ? pos[1] : 0f;
        float max = pos != null ? pos[1] : 0f;

        for (String child : childrenMap.getOrDefault(nodeId, Collections.emptyList())) {
            float[] childBounds = computeSubtreeBounds(child, childrenMap);
            min = Math.min(min, childBounds[0]);
            max = Math.max(max, childBounds[1]);
        }
        return new float[]{min, max};
    }

    private float getVanillaSubtreeMaxY(String parentId, AdvancementTree tree, Set<String> customIds) {
        AdvancementNode node = tree.get(ResourceLocation.parse(parentId));
        if (node == null) return Float.NEGATIVE_INFINITY;
        return vanillaDescendantMaxY(node, customIds);
    }

    private float getVanillaSubtreeMinY(String parentId, AdvancementTree tree, Set<String> customIds) {
        AdvancementNode node = tree.get(ResourceLocation.parse(parentId));
        if (node == null) return Float.POSITIVE_INFINITY;
        return vanillaDescendantMinY(node, customIds);
    }

    private float vanillaDescendantMaxY(AdvancementNode node, Set<String> customIds) {
        float max = Float.NEGATIVE_INFINITY;
        for (AdvancementNode child : node.children()) {
            String childId = child.holder().id().toString();
            if (customIds.contains(childId)) continue;
            if (child.holder().value().display().isPresent()) {
                max = Math.max(max, child.holder().value().display().get().getY());
            }
            max = Math.max(max, vanillaDescendantMaxY(child, customIds));
        }
        return max;
    }

    private float vanillaDescendantMinY(AdvancementNode node, Set<String> customIds) {
        float min = Float.POSITIVE_INFINITY;
        for (AdvancementNode child : node.children()) {
            String childId = child.holder().id().toString();
            if (customIds.contains(childId)) continue;
            if (child.holder().value().display().isPresent()) {
                min = Math.min(min, child.holder().value().display().get().getY());
            }
            min = Math.min(min, vanillaDescendantMinY(child, customIds));
        }
        return min;
    }

    public Map<String, float[]> getPositions() { return positions; }
    public Map<String, float[]> getVanillaShifts() { return vanillaShifts; }
}