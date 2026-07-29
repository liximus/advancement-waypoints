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
   public enum HudMode implements dev.isxander.yacl3.api.NameableEnum {
      ARROW("advwp.config.hud_mode.arrow"),
      COMPASS("advwp.config.hud_mode.compass"),
      LOCATOR("advwp.config.hud_mode.locator");

      private final String translationKey;

      HudMode(String translationKey) {
         this.translationKey = translationKey;
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
         return net.minecraft.network.chat.Component.translatable(this.translationKey);
      }
   }

   public enum HudPosition implements dev.isxander.yacl3.api.NameableEnum {
      BOTTOM_RIGHT("advwp.config.hud_position.bottom_right"),
      TOP_RIGHT("advwp.config.hud_position.top_right"),
      TOP_LEFT("advwp.config.hud_position.top_left"),
      BOTTOM_LEFT("advwp.config.hud_position.bottom_left");

      private final String translationKey;

      HudPosition(String translationKey) {
         this.translationKey = translationKey;
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
         return net.minecraft.network.chat.Component.translatable(this.translationKey);
      }
   }

   public enum PulseSpeed implements dev.isxander.yacl3.api.NameableEnum {
      SLOW("advwp.config.pulse_speed.slow", 0.005D),
      MEDIUM("advwp.config.pulse_speed.medium", 0.010D),
      FAST("advwp.config.pulse_speed.fast", 0.020D);

      private final String translationKey;
      private final double multiplier;

      PulseSpeed(String translationKey, double multiplier) {
         this.translationKey = translationKey;
         this.multiplier = multiplier;
      }

      public double getMultiplier() {
         return this.multiplier;
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
         return net.minecraft.network.chat.Component.translatable(this.translationKey);
      }
   }

   private static final Path CONFIG_PATH = Path.of("config", "advancement_waypoints", "config.json");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final ModConfig INSTANCE = new ModConfig();
   private boolean enableChatScanner = true;
   private boolean enableNavigation = true;
   private boolean allowAttachToAnyNode = false;
   private HudMode hudMode = HudMode.COMPASS;
   private HudPosition hudPosition = HudPosition.BOTTOM_RIGHT;
   private boolean showDistanceOnLocator = true;
   private boolean showItemOnLocator = true;
   private int autoDisableRadius = 32;
   private int autoDisableTime = 7;
   private boolean enableProximityPulse = true;
   private PulseSpeed pulseSpeed = PulseSpeed.MEDIUM;

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

   public boolean isEnableNavigation() {
      return this.enableNavigation;
   }

   public void setEnableNavigation(boolean enableNavigation) {
      this.enableNavigation = enableNavigation;
   }

   public boolean isAllowAttachToAnyNode() {
      return this.allowAttachToAnyNode;
   }

   public void setAllowAttachToAnyNode(boolean allowAttachToAnyNode) {
      this.allowAttachToAnyNode = allowAttachToAnyNode;
   }

   public HudMode getHudMode() {
      return this.hudMode;
   }

   public void setHudMode(HudMode hudMode) {
      this.hudMode = hudMode != null ? hudMode : HudMode.ARROW;
   }

   public HudPosition getHudPosition() {
      return this.hudPosition;
   }

   public void setHudPosition(HudPosition hudPosition) {
      this.hudPosition = hudPosition != null ? hudPosition : HudPosition.BOTTOM_RIGHT;
   }

   public boolean isShowDistanceOnLocator() {
      return this.showDistanceOnLocator;
   }

   public void setShowDistanceOnLocator(boolean showDistanceOnLocator) {
      this.showDistanceOnLocator = showDistanceOnLocator;
   }

   public boolean isShowItemOnLocator() {
      return this.showItemOnLocator;
   }

   public void setShowItemOnLocator(boolean showItemOnLocator) {
      this.showItemOnLocator = showItemOnLocator;
   }

   public int getAutoDisableRadius() {
      return this.autoDisableRadius;
   }

   public void setAutoDisableRadius(int autoDisableRadius) {
      this.autoDisableRadius = Math.max(0, autoDisableRadius);
   }

   public int getAutoDisableTime() {
      return this.autoDisableTime;
   }

   public void setAutoDisableTime(int autoDisableTime) {
      this.autoDisableTime = Math.max(1, autoDisableTime);
   }

   public boolean isEnableProximityPulse() {
      return this.enableProximityPulse;
   }

   public void setEnableProximityPulse(boolean enableProximityPulse) {
      this.enableProximityPulse = enableProximityPulse;
   }

   public PulseSpeed getPulseSpeed() {
      return this.pulseSpeed;
   }

   public void setPulseSpeed(PulseSpeed pulseSpeed) {
      this.pulseSpeed = pulseSpeed != null ? pulseSpeed : PulseSpeed.MEDIUM;
   }

   public void load() {
      if (Files.exists(CONFIG_PATH, new LinkOption[0])) {
         try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("enableChatScanner")) {
               this.enableChatScanner = obj.get("enableChatScanner").getAsBoolean();
            }
            if (obj.has("enableNavigation")) {
               this.enableNavigation = obj.get("enableNavigation").getAsBoolean();
            }
            if (obj.has("allowAttachToAnyNode")) {
               this.allowAttachToAnyNode = obj.get("allowAttachToAnyNode").getAsBoolean();
            }
            if (obj.has("hudMode")) {
               try {
                  this.hudMode = HudMode.valueOf(obj.get("hudMode").getAsString());
               } catch (Exception ignored) {
               }
            }
            if (obj.has("hudPosition")) {
               try {
                  this.hudPosition = HudPosition.valueOf(obj.get("hudPosition").getAsString());
               } catch (Exception ignored) {
               }
            }
            if (obj.has("showDistanceOnLocator")) {
               this.showDistanceOnLocator = obj.get("showDistanceOnLocator").getAsBoolean();
            }
            if (obj.has("showItemOnLocator")) {
               this.showItemOnLocator = obj.get("showItemOnLocator").getAsBoolean();
            }
            if (obj.has("autoDisableRadius")) {
               this.autoDisableRadius = obj.get("autoDisableRadius").getAsInt();
            }
            if (obj.has("autoDisableTime")) {
               this.autoDisableTime = obj.get("autoDisableTime").getAsInt();
            }
            if (obj.has("enableProximityPulse")) {
               this.enableProximityPulse = obj.get("enableProximityPulse").getAsBoolean();
            }
            if (obj.has("pulseSpeed")) {
               try {
                  this.pulseSpeed = PulseSpeed.valueOf(obj.get("pulseSpeed").getAsString());
               } catch (Exception ignored) {
               }
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
         obj.addProperty("enableNavigation", this.enableNavigation);
         obj.addProperty("allowAttachToAnyNode", this.allowAttachToAnyNode);
         obj.addProperty("hudMode", this.hudMode.name());
         obj.addProperty("hudPosition", this.hudPosition.name());
         obj.addProperty("showDistanceOnLocator", this.showDistanceOnLocator);
         obj.addProperty("showItemOnLocator", this.showItemOnLocator);
         obj.addProperty("autoDisableRadius", this.autoDisableRadius);
         obj.addProperty("autoDisableTime", this.autoDisableTime);
         obj.addProperty("enableProximityPulse", this.enableProximityPulse);
         obj.addProperty("pulseSpeed", this.pulseSpeed.name());
         Files.writeString(CONFIG_PATH, GSON.toJson(obj), StandardCharsets.UTF_8);
      } catch (Exception e) {
         AdvancementWaypoints.LOGGER.error("Failed to save mod config", e);
      }

   }

   public static Screen createConfigScreen(Screen parent) {
      return YaclConfigScreen.create(parent);
   }
}
