//package com.listraind.advancementwaypoints;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.listraind.advancementwaypoints.advancementMixinHelpers.ICustomAdvancementApplier;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientAdvancements;
//import net.minecraft.network.chat.Component;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URI;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//public class Server {
//    private static final Logger LOG = LoggerFactory.getLogger("PepeCoordAchievements");
//    public static final String API_URL = "http://localhost:5000/advancement";
//    public static final List<AdvancementWaypointsClient.ParsedAdvancement> CACHED = new CopyOnWriteArrayList<>();
//    public static volatile boolean loaded = false;
//
//    private static final Path CACHE_FILE = Path.of("config", "pepecoord_achievements_cache.json");
//
//    public static void fetchAsync() {
//        CompletableFuture.runAsync(() -> {
//            try {
//                String json = httpGet(API_URL);
//                List<AdvancementWaypointsClient.ParsedAdvancement> parsed = parse(json);
//
//                Minecraft mc = Minecraft.getInstance();
//                mc.execute(() -> {
//                    try {
//                        CACHED.clear();
//                        CACHED.addAll(parsed);
//                        loaded = true;
//                        saveCache(json);
//                        LOG.info("[PepeCoord] Загружено {} ачивок", parsed.size());
//
//                        if (mc.getConnection() != null) {
//                            ClientAdvancements adv = mc.getConnection().getAdvancements();
//
//                            ((ICustomAdvancementApplier) adv).advWaypoint_injectCustomAdvancements();
//
//                            if (mc.player != null) {
//                                mc.player.displayClientMessage(
//                                        Component.literal("§aДостижения успешно загружены."), false);
//                            }
//                        } else if (mc.player != null) {
//                            mc.player.displayClientMessage(
//                                    Component.literal("§eПроизошла ошибка при применении изменений. Перезайдите в мир."), false);
//                        }
//                    } catch (Exception ex) {
//                        LOG.error("Ошибка при применении ачивок в главном потоке", ex);
//                    }
//                });
//
//            } catch (Exception e) {
//                LOG.error("[PepeCoord] Ошибка загрузки с сервера, пытаемся загрузить из кэша", e);
//                loadFromCache();
//            }
//        });
//    }
//
//    private static void loadFromCache() {
//        CompletableFuture.runAsync(() -> {
//            try {
//                if (Files.exists(CACHE_FILE)) {
//                    String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
//                    List<AdvancementWaypointsClient.ParsedAdvancement> parsed = parse(json);
//                    CACHED.clear();
//                    CACHED.addAll(parsed);
//                    loaded = true;
//                    LOG.info("[PepeCoord] Загружено {} ачивок из кэша", parsed.size());
//
//                    Minecraft mc = Minecraft.getInstance();
//                    if (mc.player != null) {
//                        mc.execute(() -> mc.player.displayClientMessage(
//                                Component.literal("§e[PepeCoord] Возникла ошибка при подключении к серверу, попробуйте позже"), false));
//                    }
//                } else {
//                    LOG.warn("[PepeCoord] Кэш не найден");
//                }
//            } catch (Exception e) {
//                LOG.error("[PepeCoord] Ошибка загрузки из кэша", e);
//            }
//        });
//    }
//
//    private static void saveCache(String json) {
//        try {
//            Files.createDirectories(CACHE_FILE.getParent());
//            Files.writeString(CACHE_FILE, json, StandardCharsets.UTF_8);
//            LOG.info("[PepeCoord] Кэш сохранён");
//        } catch (IOException e) {
//            LOG.error("[PepeCoord] Ошибка сохранения кэша", e);
//        }
//    }
//
//    private static String httpGet(String url) throws IOException {
//        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
//        c.setRequestMethod("GET");
//        c.setConnectTimeout(10_000);
//        c.setReadTimeout(10_000);
//        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
//        try (var r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
//            StringBuilder sb = new StringBuilder();
//            String l;
//            while ((l = r.readLine()) != null) sb.append(l);
//            return sb.toString();
//        } finally {
//            c.disconnect();
//        }
//    }
//
//    private static List<AdvancementWaypointsClient.ParsedAdvancement> parse(String json) {
//        List<AdvancementWaypointsClient.ParsedAdvancement> list = new ArrayList<>();
//        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
//
//        for (JsonElement el : arr) {
//            JsonObject o = el.getAsJsonObject();
//
//            String id = o.get("id").getAsString();
//            String icon = o.has("icon") ? o.get("icon").getAsString() : "minecraft:paper";
//            String title = o.has("title") ? o.get("title").getAsString() : "???";
//            String desc = o.has("description") ? o.get("description").getAsString() : "";
//            String frame = o.has("frame") ? o.get("frame").getAsString() : "task";
//            String bg = o.has("background") && !o.get("background").isJsonNull()
//                    ? o.get("background").getAsString() : null;
//            String parent = o.has("parent") && !o.get("parent").isJsonNull()
//                    ? o.get("parent").getAsString() : null;
//
//            float x = o.has("x") ? o.get("x").getAsFloat() : 0f;
//            float y = o.has("y") ? o.get("y").getAsFloat() : 0f;
//
//            list.add(new AdvancementWaypointsClient.ParsedAdvancement(id, icon, title, desc, frame, bg, parent, x, y));
//
//        }
//        return list;
//    }
//}