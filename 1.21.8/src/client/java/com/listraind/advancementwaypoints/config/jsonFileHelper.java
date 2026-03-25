package com.listraind.advancementwaypoints.config;

import com.google.gson.*;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient.ParsedAdvancement;

public class jsonFileHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

    public static List<JsonObject> readArray(Path path) {
        List<JsonObject> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        try {
            JsonArray arr = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) result.add(el.getAsJsonObject());
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Read error: {}", path, e);
        }
        return result;
    }

    public static ParsedAdvancement fromJson(JsonObject o) {
        return new ParsedAdvancement(
                str(o, "id", ""), str(o, "icon", "minecraft:paper"),
                str(o, "title", "???"), str(o, "description", ""),
                str(o, "frame", "task"),
                nullable(o, "background"), nullable(o, "parent"),
                o.has("x") ? o.get("x").getAsFloat() : 0f,
                o.has("y") ? o.get("y").getAsFloat() : 0f
        );
    }

    public static List<ParsedAdvancement> fromJsonList(List<JsonObject> list) {
        List<ParsedAdvancement> result = new ArrayList<>();
        for (JsonObject o : list) result.add(fromJson(o));
        return result;
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) ? o.get(key).getAsString() : def;
    }

    private static String nullable(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}