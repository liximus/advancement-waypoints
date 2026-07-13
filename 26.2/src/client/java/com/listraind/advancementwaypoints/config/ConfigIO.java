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
    public static final int FORMAT_VERSION = 1;

    public static Path worldDir() {
        return BASE.resolve("worlds").resolve(worldHash());
    }

    private static JsonArray parseEntriesArray(String raw) {
        JsonElement el = JsonParser.parseString(raw);
        if (el.isJsonArray()) return el.getAsJsonArray();
        if (el.isJsonObject()) {
            JsonObject root = el.getAsJsonObject();
            if (root.has("entries") && root.get("entries").isJsonArray()) return root.getAsJsonArray("entries");
        }
        return new JsonArray();
    }

    public static List<JsonObject> readArray(Path path) {
        List<JsonObject> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        try {
            JsonArray arr = parseEntriesArray(Files.readString(path, StandardCharsets.UTF_8));
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
                            JsonArray arr = parseEntriesArray(Files.readString(p, StandardCharsets.UTF_8));
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
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            JsonArray arr = new JsonArray();
            for (JsonObject obj : list) {
                JsonObject clean = obj.deepCopy();
                clean.remove("x");
                clean.remove("y");
                arr.add(clean);
            }
            JsonObject root = new JsonObject();
            root.addProperty("version", FORMAT_VERSION);
            root.add("entries", arr);
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Write error: {}", path, e);
        }
    }

    public static String str(JsonObject o, String key, String defaultValue) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : defaultValue;
    }

    public static String nullable(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static String worldHash() {
        Minecraft mc = Minecraft.getInstance();
        String raw;
        if (mc.getCurrentServer() != null) {
            var server = mc.getCurrentServer();
            raw = "server:" + server.name + "|" + server.ip;
        } else if (mc.getSingleplayerServer() != null) {
            raw = "single:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toString();
        } else {
            raw = "unknown";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }
}