package com.listraind.advancementwaypoints.navigator;

import com.listraind.advancementwaypoints.config.ModConfig;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class NavigatorHud {
   private static final Identifier ARROW = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/arrow.png");
   private static final Identifier PORTAL = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/portal.png");
   private static final Identifier[] COMPASS_TEXTURES = new Identifier[32];
   static {
      for (int i = 0; i < 32; i++) {
         COMPASS_TEXTURES[i] = Identifier.fromNamespaceAndPath("minecraft", String.format("textures/item/compass_%02d.png", i));
      }
   }

   private static final int SIZE = 16;
   private static final int MARGIN = 10;
   private static NavigatorHud INSTANCE;

   private NavigatorHud() {
   }

   public static NavigatorHud getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NavigatorHud();
      }

      return INSTANCE;
   }

   void register() {
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("advancement-waypoints", "nav_hud"), this::extractRenderState);
   }

   private void extractRenderState(GuiGraphicsExtractor g, DeltaTracker dt) {
      if (!ModConfig.getInstance().isEnableNavigation()) return;
      Navigator nav = Navigator.getInstance();
      if (nav.hasAnyTarget()) {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
            if (dim != null) {
               BlockPos target = nav.getNearest(dim, player.blockPosition());
               float alpha = nav.updateProximityAndGetAlpha(player, target);
               if (!nav.hasAnyTarget()) return;
               if (ModConfig.getInstance().getHudMode() == ModConfig.HudMode.LOCATOR) return;

               int sw = mc.getWindow().getGuiScaledWidth();
               int sh = mc.getWindow().getGuiScaledHeight();
               if (target != null) {
                  if (ModConfig.getInstance().getHudMode() == ModConfig.HudMode.COMPASS) {
                     this.renderCompass(g, mc, player, target, sw, sh, alpha);
                  } else {
                     this.renderArrow(g, mc, player, target, sw, sh, alpha);
                  }
               } else {
                  this.renderPortal(g, mc, player, sw, sh);
               }

            }
         }
      }
   }

   private static int getLeftHotbarBoundary(Player player, int sw) {
      boolean isRightHanded = player == null || player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
      boolean hasOffhand = player != null && !player.getOffhandItem().isEmpty();
      return sw / 2 - 91 - (isRightHanded && hasOffhand ? 29 : 0);
   }

   private static int getRightHotbarBoundary(Player player, int sw) {
      boolean isRightHanded = player == null || player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
      boolean hasOffhand = player != null && !player.getOffhandItem().isEmpty();
      return sw / 2 + 91 + (!isRightHanded && hasOffhand ? 29 : 0);
   }

   private void renderArrow(GuiGraphicsExtractor g, Minecraft mc, Player player, BlockPos target, int sw, int sh, float alpha) {
      double dx = (double)target.getX() + 0.5D - player.getX();
      double dz = (double)target.getZ() + 0.5D - player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      String text = String.format("%.0f m", dist);
      double angle = Math.toDegrees(Math.atan2(dz, -dx)) - 90.0D;
      float rot = (float)(-Math.toRadians(angle + (double)player.getYRot()));
      int tw = mc.font.width(text);

      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      boolean isHotbar = (pos == ModConfig.HudPosition.HOTBAR_LEFT || pos == ModConfig.HudPosition.HOTBAR_RIGHT);
      float scale = ModConfig.getInstance().getHudScaleFactor();
      int margin = ModConfig.getInstance().getHudOffset();

      int boxHalf = isHotbar ? 11 : 12;
      int halfW = Math.max(boxHalf, tw / 2);
      int topExtent = isHotbar ? 21 : 12;
      int bottomExtent = isHotbar ? 11 : 23;

      int scaledHalfW = (int) Math.ceil(halfW * scale);
      int scaledTop = (int) Math.ceil(topExtent * scale);
      int scaledBottom = (int) Math.ceil(bottomExtent * scale);

      int cx, cy;
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - scaledHalfW;
            cy = margin + scaledTop;
         }
         case TOP_LEFT -> {
            cx = margin + scaledHalfW;
            cy = margin + scaledTop;
         }
         case BOTTOM_LEFT -> {
            cx = margin + scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
         case HOTBAR_LEFT -> {
            cx = getLeftHotbarBoundary(player, sw) - margin - scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         case HOTBAR_RIGHT -> {
            cx = getRightHotbarBoundary(player, sw) + margin + scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         default -> {
            cx = sw - margin - scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
      }

      net.minecraft.client.gui.screens.Screen currentScreen = mc.gui.screen();
      boolean isScreenOpen = currentScreen != null
            && !(currentScreen instanceof net.minecraft.client.gui.screens.PauseScreen)
            && !(currentScreen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen);
      float dimFactor = isScreenOpen ? 0.25F : 1.0F;
      int iconAlpha = (int)(255 * alpha);
      int rgb = (int)(255 * dimFactor);
      int iconColor = (iconAlpha << 24) | (rgb << 16) | (rgb << 8) | rgb;
      int boxColor = Integer.MIN_VALUE;
      int textColor = isScreenOpen ? 0xFF505050 : -1;

      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().scale(scale, scale);

      g.fill(-boxHalf, -boxHalf, boxHalf, boxHalf, boxColor);

      g.pose().pushMatrix();
      g.pose().rotate(rot);
      g.blit(RenderPipelines.GUI_TEXTURED, ARROW, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16, iconColor);
      g.pose().popMatrix();

      int textY = isHotbar ? -21 : 14;
      g.text(mc.font, text, -tw / 2, textY, textColor, true);

      g.pose().popMatrix();
   }

   private void renderCompass(GuiGraphicsExtractor g, Minecraft mc, Player player, BlockPos target, int sw, int sh, float alpha) {
      double dx = (double)target.getX() + 0.5D - player.getX();
      double dz = (double)target.getZ() + 0.5D - player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      String text = String.format("%.0f m", dist);
      double angle = Math.toDegrees(Math.atan2(dz, -dx)) - 90.0D;
      double relAngle = 180.0D - (angle + (double)player.getYRot());
      relAngle = (relAngle % 360.0D + 360.0D) % 360.0D;
      int frame = (int) Math.round(relAngle / 11.25D) % 32;

      int tw = mc.font.width(text);
      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      boolean isHotbar = (pos == ModConfig.HudPosition.HOTBAR_LEFT || pos == ModConfig.HudPosition.HOTBAR_RIGHT);
      float scale = ModConfig.getInstance().getHudScaleFactor();
      int margin = ModConfig.getInstance().getHudOffset();

      int boxHalf = isHotbar ? 11 : 16;
      int halfW = Math.max(boxHalf, tw / 2);
      int topExtent = isHotbar ? 21 : 16;
      int bottomExtent = isHotbar ? 11 : 27;

      int scaledHalfW = (int) Math.ceil(halfW * scale);
      int scaledTop = (int) Math.ceil(topExtent * scale);
      int scaledBottom = (int) Math.ceil(bottomExtent * scale);

      int cx, cy;
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - scaledHalfW;
            cy = margin + scaledTop;
         }
         case TOP_LEFT -> {
            cx = margin + scaledHalfW;
            cy = margin + scaledTop;
         }
         case BOTTOM_LEFT -> {
            cx = margin + scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
         case HOTBAR_LEFT -> {
            cx = getLeftHotbarBoundary(player, sw) - margin - scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         case HOTBAR_RIGHT -> {
            cx = getRightHotbarBoundary(player, sw) + margin + scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         default -> {
            cx = sw - margin - scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
      }

      net.minecraft.client.gui.screens.Screen currentScreen = mc.gui.screen();
      boolean isScreenOpen = currentScreen != null
            && !(currentScreen instanceof net.minecraft.client.gui.screens.PauseScreen)
            && !(currentScreen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen);
      float dimFactor = isScreenOpen ? 0.25F : 1.0F;
      int iconAlpha = (int)(255 * alpha);
      int rgb = (int)(255 * dimFactor);
      int iconColor = (iconAlpha << 24) | (rgb << 16) | (rgb << 8) | rgb;
      int boxColor = Integer.MIN_VALUE;
      int textColor = isScreenOpen ? 0xFF505050 : -1;

      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().scale(scale, scale);

      g.fill(-boxHalf, -boxHalf, boxHalf, boxHalf, boxColor);

      g.pose().pushMatrix();
      if (!isHotbar) {
         g.pose().scale(1.5F, 1.5F);
      }
      g.blit(RenderPipelines.GUI_TEXTURED, COMPASS_TEXTURES[frame], -8, -8, 0.0F, 0.0F, 16, 16, 16, 16, iconColor);
      g.pose().popMatrix();

      int textY = isHotbar ? -21 : 18;
      g.text(mc.font, text, -tw / 2, textY, textColor, true);

      g.pose().popMatrix();
   }

   private void renderPortal(GuiGraphicsExtractor g, Minecraft mc, Player player, int sw, int sh) {
      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      boolean isHotbar = (pos == ModConfig.HudPosition.HOTBAR_LEFT || pos == ModConfig.HudPosition.HOTBAR_RIGHT);
      float scale = ModConfig.getInstance().getHudScaleFactor();
      int margin = ModConfig.getInstance().getHudOffset();

      int boxHalf = isHotbar ? 11 : 12;
      int halfW = boxHalf;
      int topExtent = boxHalf;
      int bottomExtent = boxHalf;

      int scaledHalfW = (int) Math.ceil(halfW * scale);
      int scaledTop = (int) Math.ceil(topExtent * scale);
      int scaledBottom = (int) Math.ceil(bottomExtent * scale);

      int cx, cy;
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - scaledHalfW;
            cy = margin + scaledTop;
         }
         case TOP_LEFT -> {
            cx = margin + scaledHalfW;
            cy = margin + scaledTop;
         }
         case BOTTOM_LEFT -> {
            cx = margin + scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
         case HOTBAR_LEFT -> {
            cx = getLeftHotbarBoundary(player, sw) - margin - scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         case HOTBAR_RIGHT -> {
            cx = getRightHotbarBoundary(player, sw) + margin + scaledHalfW;
            cy = sh - (int) Math.ceil(11 * scale);
         }
         default -> {
            cx = sw - margin - scaledHalfW;
            cy = sh - margin - scaledBottom;
         }
      }

      net.minecraft.client.gui.screens.Screen currentScreen = mc.gui.screen();
      boolean isScreenOpen = currentScreen != null
            && !(currentScreen instanceof net.minecraft.client.gui.screens.PauseScreen)
            && !(currentScreen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen);
      float dimFactor = isScreenOpen ? 0.25F : 1.0F;
      int rgb = (int)(255 * dimFactor);
      int portalColor = (255 << 24) | (rgb << 16) | (rgb << 8) | rgb;
      int boxColor = Integer.MIN_VALUE;

      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().scale(scale, scale);

      g.fill(-boxHalf, -boxHalf, boxHalf, boxHalf, boxColor);
      g.blit(RenderPipelines.GUI_TEXTURED, PORTAL, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16, portalColor);

      g.pose().popMatrix();
   }
}
