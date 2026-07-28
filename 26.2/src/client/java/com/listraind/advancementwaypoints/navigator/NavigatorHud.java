package com.listraind.advancementwaypoints.navigator;

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
      Navigator nav = Navigator.getInstance();
      if (nav.hasAnyTarget()) {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
            if (dim != null) {
               BlockPos target = nav.getNearest(dim, player.blockPosition());
               int sw = mc.getWindow().getGuiScaledWidth();
               int sh = mc.getWindow().getGuiScaledHeight();
               if (target != null) {
                  this.renderArrow(g, mc, player, target, sw, sh);
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
      int cx = sw - 10 - ew / 2;
      int var10000 = sh - 10;
      Objects.requireNonNull(mc.font);
      int ty = var10000 - 9;
      int cy = ty - 6 - 8;
      g.fill(cx - 8 - 4, cy - 8 - 4, cx + 8 + 4, cy + 8 + 4, Integer.MIN_VALUE);
      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)cy);
      g.pose().rotate(rot);
      g.pose().translate(-8.0F, -8.0F);
      g.blit(RenderPipelines.GUI_TEXTURED, ARROW, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      g.pose().popMatrix();
      g.text(mc.font, text, cx - tw / 2, ty, -1, true);
   }

   private void renderPortal(GuiGraphicsExtractor g, int sw, int sh) {
      int cx = sw - 10 - 8;
      int cy = sh - 10 - 8;
      g.fill(cx - 8 - 4, cy - 8 - 4, cx + 8 + 4, cy + 8 + 4, Integer.MIN_VALUE);
      g.blit(RenderPipelines.GUI_TEXTURED, PORTAL, cx - 8, cy - 8, 0.0F, 0.0F, 16, 16, 16, 16);
   }
}
