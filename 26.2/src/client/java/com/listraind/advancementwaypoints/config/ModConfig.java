package com.listraind.advancementwaypoints.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.modmenu.YaclConfigScreen;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.Screen;

public class ModConfig {
   private static final Path CONFIG_PATH = Path.of("config", "advancement_waypoints", "config.json");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final ModConfig INSTANCE = new ModConfig();
   private boolean enableChatScanner = true;

   private ModConfig() {
      this.load();
   }

   public static ModConfig getInstance() {
      return INSTANCE;
   }

   public boolean isEnableChatScanner() {
      return this.enableChatScanner;
   }

   public void setEnableChatScanner(boolean enableChatScanner) {
      this.enableChatScanner = enableChatScanner;
   }

   public void load() {
      if (Files.exists(CONFIG_PATH, new LinkOption[0])) {
         try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("enableChatScanner")) {
               this.enableChatScanner = obj.get("enableChatScanner").getAsBoolean();
            }
         } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Failed to load mod config", e);
         }

      }
   }

   public void save() {
      try {
         Path parent = CONFIG_PATH.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         JsonObject obj = new JsonObject();
         obj.addProperty("enableChatScanner", this.enableChatScanner);
         Files.writeString(CONFIG_PATH, GSON.toJson(obj), StandardCharsets.UTF_8);
      } catch (Exception e) {
         AdvancementWaypoints.LOGGER.error("Failed to save mod config", e);
      }

   }

   public static Screen createConfigScreen(Screen parent) {
      return YaclConfigScreen.create(parent);
   }
}
