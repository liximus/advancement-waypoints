package com.listraind.advancementwaypoints.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public class ConfigIO {
   private static final Path BASE = Path.of("config", "advancement_waypoints");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   public static final int FORMAT_VERSION = 1;

   public static Path worldDir() {
      return BASE.resolve("worlds").resolve(worldHash());
   }

   private static JsonArray parseEntriesArray(String raw) {
      JsonElement el = JsonParser.parseString(raw);
      if (el.isJsonArray()) {
         return el.getAsJsonArray();
      } else {
         if (el.isJsonObject()) {
            JsonObject root = el.getAsJsonObject();
            if (root.has("entries") && root.get("entries").isJsonArray()) {
               return root.getAsJsonArray("entries");
            }
         }

         return new JsonArray();
      }
   }

   public static List<JsonObject> readArray(Path path) {
      List<JsonObject> result = new ArrayList();
      if (!Files.exists(path, new LinkOption[0])) {
         return result;
      } else {
         try {
            for(JsonElement el : parseEntriesArray(Files.readString(path, StandardCharsets.UTF_8))) {
               result.add(el.getAsJsonObject());
            }
         } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Read error: {}", path, e);
         }

         return result;
      }
   }

   public static List<JsonObject> readAllInFolder(Path folder) {
      List<JsonObject> result = new ArrayList();
      if (Files.exists(folder, new LinkOption[0]) && Files.isDirectory(folder, new LinkOption[0])) {
         try {
            Stream<Path> stream = Files.list(folder);

            try {
               stream.filter((p) -> p.toString().endsWith(".json")).sorted().forEach((p) -> {
                  try {
                     for(JsonElement el : parseEntriesArray(Files.readString(p, StandardCharsets.UTF_8))) {
                        result.add(el.getAsJsonObject());
                     }
                  } catch (Exception e) {
                     AdvancementWaypoints.LOGGER.error("Read error: {}", p, e);
                  }

               });
            } catch (Throwable var6) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Read folder error: {}", folder, e);
         }

         return result;
      } else {
         return result;
      }
   }

   public static void writeArray(Path path, List<JsonObject> list) {
      try {
         Path parent = path.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         JsonArray arr = new JsonArray();

         for(JsonObject obj : list) {
            JsonObject clean = obj.deepCopy();
            clean.remove("x");
            clean.remove("y");
            arr.add(clean);
         }

         JsonObject root = new JsonObject();
         root.addProperty("version", 1);
         root.add("entries", arr);
         Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
         Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);

         try {
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var7) {
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
         ServerData server = mc.getCurrentServer();
         raw = "server:" + server.name + "|" + server.ip;
      } else if (mc.getSingleplayerServer() != null) {
         IntegratedServer var10000 = mc.getSingleplayerServer();
         raw = "single:" + var10000.getWorldPath(LevelResource.ROOT).toString();
      } else {
         raw = "unknown";
      }

      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
         StringBuilder sb = new StringBuilder();

         for(int i = 0; i < 16; ++i) {
            sb.append(String.format("%02x", digest[i]));
         }

         return sb.toString();
      } catch (Exception var5) {
         return String.valueOf(raw.hashCode());
      }
   }
}
