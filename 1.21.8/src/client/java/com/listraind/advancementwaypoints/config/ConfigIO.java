package com.listraind.advancementwaypoints.config;

import com.google.gson.*;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ConfigIO {

    private static final Path BASE = Path.of("config", "advancement_waypoints");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path worldDir() {
        return BASE.resolve("worlds").resolve(worldHash());
    }

    public static List<JsonObject> readArray(Path path) {
        List<JsonObject> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        try {
            JsonArray arr = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) result.add(el.getAsJsonObject());
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Read error: {}", path, e);
        }
        return result;
    }

    public static List<JsonObject> readAllInFolder(Path folder) {
        List<JsonObject> result = new ArrayList<>();
        if (!Files.exists(folder) || !Files.isDirectory(folder)) return result;
        try (var stream = Files.list(folder)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          JsonArray arr = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonArray();
                          for (JsonElement el : arr) result.add(el.getAsJsonObject());
                      } catch (Exception e) {
                          AdvancementWaypoints.LOGGER.error("Read error: {}", p, e);
                      }
                  });
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Read folder error: {}", folder, e);
        }
        return result;
    }

    public static void writeArray(Path path, List<JsonObject> list) {
        try {
            Files.createDirectories(path.getParent());
            JsonArray arr = new JsonArray();
            for (JsonObject obj : list) {
                JsonObject clean = obj.deepCopy();
                clean.remove("x");
                clean.remove("y");
                arr.add(clean);
            }
            Files.writeString(path, GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Write error: {}", path, e);
        }
    }

    public static String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    public static String nullable(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static String worldHash() {
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