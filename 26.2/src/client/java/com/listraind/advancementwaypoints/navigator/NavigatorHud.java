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
         if (ModConfig.getInstance().getHudMode() == ModConfig.HudMode.LOCATOR) return;

         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
            if (dim != null) {
               BlockPos target = nav.getNearest(dim, player.blockPosition());
               int sw = mc.getWindow().getGuiScaledWidth();
               int sh = mc.getWindow().getGuiScaledHeight();
               if (target != null) {
                  if (ModConfig.getInstance().getHudMode() == ModConfig.HudMode.COMPASS) {
                     this.renderCompass(g, mc, player, target, sw, sh);
                  } else {
                     this.renderArrow(g, mc, player, target, sw, sh);
                  }
               } else {
                  this.renderPortal(g, sw, sh);
               }

            }
         }
      }
   }

   private void renderArrow(GuiGraphicsExtractor g, Minecraft mc, Player player, BlockPos target, int sw, int sh) {
      double dx = (double)target.getX() + (double)0.5F - player.getX();
      double dz = (double)target.getZ() + (double)0.5F - player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      String text = String.format("%.0f m", dist);
      double angle = Math.toDegrees(Math.atan2(dz, -dx)) - (double)90.0F;
      float rot = (float)(-Math.toRadians(angle + (double)player.getYRot()));
      int tw = mc.font.width(text);
      int ew = Math.max(16, tw);

      int cx, ty, cy;
      int margin = MARGIN;
      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - ew / 2;
            cy = margin + 8 + 4;
            ty = cy + 8 + 6;
         }
         case TOP_LEFT -> {
            cx = margin + ew / 2;
            cy = margin + 8 + 4;
            ty = cy + 8 + 6;
         }
         case BOTTOM_LEFT -> {
            cx = margin + ew / 2;
            ty = sh - margin - 9;
            cy = ty - 6 - 8;
         }
         default -> {
            cx = sw - margin - ew / 2;
            ty = sh - margin - 9;
            cy = ty - 6 - 8;
         }
      }

      g.fill(cx - 8 - 4, cy - 8 - 4, cx + 8 + 4, cy + 8 + 4, Integer.MIN_VALUE);
      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().rotate(rot);
      g.pose().translate(-8.0F, -8.0F);
      g.blit(RenderPipelines.GUI_TEXTURED, ARROW, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      g.pose().popMatrix();
      g.text(mc.font, text, cx - tw / 2, ty, -1, true);
   }

   private void renderCompass(GuiGraphicsExtractor g, Minecraft mc, Player player, BlockPos target, int sw, int sh) {
      double dx = (double)target.getX() + 0.5D - player.getX();
      double dz = (double)target.getZ() + 0.5D - player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      String text = String.format("%.0f m", dist);
      double angle = Math.toDegrees(Math.atan2(dz, -dx)) - 90.0D;
      double relAngle = 180.0D - (angle + (double)player.getYRot());
      relAngle = (relAngle % 360.0D + 360.0D) % 360.0D;
      int frame = (int) Math.round(relAngle / 11.25D) % 32;

      int compassSize = 24;
      int hs = compassSize / 2;
      int tw = mc.font.width(text);
      int ew = Math.max(compassSize, tw);

      int cx, ty, cy;
      int margin = MARGIN;
      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - ew / 2;
            cy = margin + hs + 4;
            ty = cy + hs + 6;
         }
         case TOP_LEFT -> {
            cx = margin + ew / 2;
            cy = margin + hs + 4;
            ty = cy + hs + 6;
         }
         case BOTTOM_LEFT -> {
            cx = margin + ew / 2;
            ty = sh - margin - 9;
            cy = ty - 6 - hs;
         }
         default -> {
            cx = sw - margin - ew / 2;
            ty = sh - margin - 9;
            cy = ty - 6 - hs;
         }
      }

      g.fill(cx - hs - 4, cy - hs - 4, cx + hs + 4, cy + hs + 4, Integer.MIN_VALUE);

      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().scale(1.5F, 1.5F);
      g.pose().translate(-8.0F, -8.0F);
      g.blit(RenderPipelines.GUI_TEXTURED, COMPASS_TEXTURES[frame], 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      g.pose().popMatrix();

      g.text(mc.font, text, cx - tw / 2, ty, -1, true);
   }

   private void renderPortal(GuiGraphicsExtractor g, int sw, int sh) {
      int cx, cy;
      int margin = MARGIN;
      ModConfig.HudPosition pos = ModConfig.getInstance().getHudPosition();
      switch (pos) {
         case TOP_RIGHT -> {
            cx = sw - margin - 8;
            cy = margin + 8 + 4;
         }
         case TOP_LEFT -> {
            cx = margin + 8;
            cy = margin + 8 + 4;
         }
         case BOTTOM_LEFT -> {
            cx = margin + 8;
            cy = sh - margin - 8 - 9;
         }
         default -> {
            cx = sw - margin - 8;
            cy = sh - margin - 8 - 9;
         }
      }

      g.fill(cx - 8 - 4, cy - 8 - 4, cx + 8 + 4, cy + 8 + 4, Integer.MIN_VALUE);
      g.blit(RenderPipelines.GUI_TEXTURED, PORTAL, cx - 8, cy - 8, 0.0F, 0.0F, 16, 16, 16, 16);
   }
}
