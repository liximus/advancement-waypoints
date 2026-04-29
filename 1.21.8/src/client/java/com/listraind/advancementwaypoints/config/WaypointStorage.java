package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointStorage {

    private static ResourceLocation lastParent = null;

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
        try (var stream = Files.list(waypointsFolder())) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> {
                        List<JsonObject> contents = ConfigIO.readArray(p);
                        return contents.stream().anyMatch(o -> id.equals(ConfigIO.str(o, "id", "")));
                    })
                    .findFirst().orElse(null);
        } catch (Exception e) {
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
        String id = data.get("id").getAsString();

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
        Path file = findFileContaining(id);
        if (file == null) return;

        List<JsonObject> fileContents = ConfigIO.readArray(file);
        fileContents.removeIf(o -> id.equals(ConfigIO.str(o, "id", "")));

        if (fileContents.isEmpty()) {
            try { Files.deleteIfExists(file); } catch (Exception ignored) {}
        } else {
            ConfigIO.writeArray(file, fileContents);
        }

        AdvancementWaypointsClient.reloadAdvancements();
    }

    public static JsonObject getWaypointOrVanilla(ResourceLocation id) {
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
                ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(d.getIcon().getItem());
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
        String id = data.get("id").getAsString();

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

    public static ResourceLocation getLastParent() { return lastParent; }
    public static void setLastParent(ResourceLocation p) { lastParent = p; }
}