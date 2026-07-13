package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WaypointStorage {

    private static Identifier lastParent = null;

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
        if (!Files.exists(waypointsFolder())) return null;
        List<Path> jsonFiles;
        try (var stream = Files.list(waypointsFolder())) {
            jsonFiles = stream.filter(p -> p.toString().endsWith(".json"))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return null;
        }
        for (Path p : jsonFiles) {
            List<JsonObject> contents = ConfigIO.readArray(p);
            if (contents.stream().anyMatch(o -> id.equals(ConfigIO.str(o, "id", "")))) {
                return p;
            }
        }
        return null;
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
        if (id.isEmpty()) return;

        Path file = findFileContaining(id);
        if (file == null) file = mainFile();

        List<JsonObject> fileContents = Files.exists(file) ? ConfigIO.readArray(file) : new ArrayList<>();

        JsonObject existing = null;
        for (JsonObject o : fileContents) {
            if (id.equals(ConfigIO.str(o, "id", ""))) {
                existing = o;
                break;
            }
        }

        if (existing != null) {
            for (var entry : data.entrySet()) existing.add(entry.getKey(), entry.getValue());
        } else {
            fileContents.add(data);
        }

        ConfigIO.writeArray(file, fileContents);
        AdvancementWaypointsClient.reloadAdvancements();
    }

    public static void deleteWaypoint(String id) {
        Set<String> toDelete = collectDescendants(id);
        toDelete.add(id);

        Set<Path> touchedFiles = new HashSet<>();
        for (String victimId : toDelete) {
            Path file = findFileContaining(victimId);
            if (file != null) touchedFiles.add(file);
        }

        for (Path file : touchedFiles) {
            List<JsonObject> fileContents = ConfigIO.readArray(file);
            fileContents.removeIf(o -> toDelete.contains(ConfigIO.str(o, "id", "")));

            if (fileContents.isEmpty()) {
                try {
                    Files.deleteIfExists(file);
                } catch (Exception ignored) {
                }
            } else {
                ConfigIO.writeArray(file, fileContents);
            }
        }

        AdvancementWaypointsClient.reloadAdvancements();
    }

    private static Set<String> collectDescendants(String rootId) {
        List<JsonObject> all = loadWaypoints();
        Map<String, List<String>> childrenByParent = new HashMap<>();
        for (JsonObject o : all) {
            String parent = ConfigIO.nullable(o, "parent");
            if (parent == null || parent.isEmpty()) continue;
            String childId = ConfigIO.str(o, "id", "");
            if (childId.isEmpty()) continue;
            childrenByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(childId);
        }

        Set<String> descendants = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> children = childrenByParent.get(current);
            if (children == null) continue;
            for (String child : children) {
                if (descendants.add(child)) queue.add(child);
            }
        }
        return descendants;
    }

    public static JsonObject getWaypointOrVanilla(Identifier id) {
        for (JsonObject obj : loadWaypoints()) {
            if (id.toString().equals(ConfigIO.str(obj, "id", ""))) return obj;
        }

        JsonObject result = new JsonObject();
        result.addProperty("id", id.toString());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            AdvancementNode node = mc.player.connection.getAdvancements().getTree().get(id);
            if (node != null && node.holder().value().display().isPresent()) {
                var d = node.holder().value().display().get();
                result.addProperty("title", d.getTitle().getString());
                result.addProperty("description", d.getDescription().getString());
                Identifier iconId = BuiltInRegistries.ITEM.getKey(d.getIcon().item().value());
                result.addProperty("icon", iconId != null ? iconId.toString() : "minecraft:stone");
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
        if (id.isEmpty()) return;

        JsonObject existing = null;
        for (JsonObject o : overrides) {
            if (id.equals(ConfigIO.str(o, "id", ""))) {
                existing = o;
                break;
            }
        }

        if (existing != null) {
            for (var entry : data.entrySet()) existing.add(entry.getKey(), entry.getValue());
        } else {
            overrides.add(data);
        }
        saveOverrides(overrides);
    }

    public static Identifier getLastParent() {
        return lastParent;
    }

    public static void setLastParent(Identifier p) {
        lastParent = p;
    }
}