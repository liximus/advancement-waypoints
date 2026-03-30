package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

public class ConfigManager {
    private static final Path BASE = Path.of("config", "advancement_waypoints");

    private static ResourceLocation lastParent = null;

    private static Path worldDir() {
        return BASE.resolve("worlds").resolve(getWorldHash());
    }

    public static List<JsonObject> loadCustomAdvancements() {
        return jsonFileHelper.readArray(worldDir().resolve("custom_advancements.json"));
    }

    public static void saveCustomAdvancements(List<JsonObject> list) {
        jsonFileHelper.writeArray(worldDir().resolve("custom_advancements.json"), list);
    }

    public static List<JsonObject> loadOverrides() {
        return jsonFileHelper.readArray(worldDir().resolve("overrides.json"));
    }

    public static void saveOverrides(List<JsonObject> list) {
        jsonFileHelper.writeArray(worldDir().resolve("overrides.json"), list);
    }

    public static JsonObject loadOverride(String id) {
        List<JsonObject> overrides = loadOverrides();
        for (JsonObject o : overrides) {
            if (o.has("id") && o.get("id").getAsString().equals(id)) {
                return o;
            }
        }
        return null;
    }

    public static void saveOverride(JsonObject overrideData) {
        List<JsonObject> overrides = loadOverrides();
        String id = overrideData.get("id").getAsString();
        JsonObject existing = null;
        for (JsonObject o : overrides) {
            if (o.has("id") && o.get("id").getAsString().equals(id)) {
                existing = o;
                break;
            }
        }
        if (existing != null) {
            for (var entry : overrideData.entrySet()) {
                existing.add(entry.getKey(), entry.getValue());
            }
        } else {
            overrides.add(overrideData);
        }
        saveOverrides(overrides);
    }

    public static void removeOverride(String id) {
        List<JsonObject> overrides = loadOverrides();
        overrides.removeIf(o -> o.has("id") && o.get("id").getAsString().equals(id));
        saveOverrides(overrides);
    }

    public static ResourceLocation getLastParent() {
        return lastParent;
    }

    public static void setLastParent(ResourceLocation parentId) {
        lastParent = parentId;
    }

    private static String getWorldHash() {
        Minecraft mc = Minecraft.getInstance();
        String raw;
        if (mc.getCurrentServer() != null) raw = mc.getCurrentServer().ip;
        else if (mc.getSingleplayerServer() != null)
            raw = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toString();
        else raw = "unknown";
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }
}