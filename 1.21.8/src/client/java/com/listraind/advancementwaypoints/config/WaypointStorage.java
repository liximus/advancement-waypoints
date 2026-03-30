package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;

public class WaypointStorage {

    private static ResourceLocation lastParent = null;

    private static Path waypointsPath() {
        return ConfigIO.worldDir().resolve("custom_advancements.json");
    }

    private static Path overridesPath() {
        return ConfigIO.worldDir().resolve("overrides.json");
    }

    public static List<JsonObject> loadWaypoints() {
        return ConfigIO.readArray(waypointsPath());
    }

    public static void saveWaypoints(List<JsonObject> list) {
        ConfigIO.writeArray(waypointsPath(), list);
    }

    public static List<JsonObject> loadOverrides() {
        return ConfigIO.readArray(overridesPath());
    }

    public static void saveOverrides(List<JsonObject> list) {
        ConfigIO.writeArray(overridesPath(), list);
    }

    public static void saveOrUpdateWaypoint(JsonObject data) {
        List<JsonObject> all = loadWaypoints();
        String id = data.get("id").getAsString();

        JsonObject existing = null;
        for (JsonObject o : all) {
            if (id.equals(ConfigIO.str(o, "id", ""))) {
                existing = o;
                break;
            }
        }

        if (existing != null) {
            for (var entry : data.entrySet()) {
                existing.add(entry.getKey(), entry.getValue());
            }
        } else {
            all.add(data);
        }

        saveWaypoints(all);
        AdvancementWaypointsClient.reloadAdvancements();
    }

    public static void deleteWaypoint(String id) {
        List<JsonObject> all = loadWaypoints();
        all.removeIf(o -> id.equals(ConfigIO.str(o, "id", "")));
        saveWaypoints(all);
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