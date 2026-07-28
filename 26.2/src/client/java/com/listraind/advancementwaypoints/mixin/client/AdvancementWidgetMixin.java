package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.WidgetFrameHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {AdvancementWidget.class},
   priority = 500
)
public class AdvancementWidgetMixin {
   @Shadow
   private AdvancementNode advancementNode;
   @Shadow
   private DisplayInfo display;

   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void suppressRenderIfHidden(GuiGraphicsExtractor g, int x, int y, CallbackInfo ci) {
      if (WaypointStorage.isNodeHidden(this.advancementNode)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"extractConnectivity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void suppressConnectivityIfHidden(GuiGraphicsExtractor g, int x, int y, boolean drawLines, CallbackInfo ci) {
      if (WaypointStorage.isNodeHidden(this.advancementNode)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"extractHover"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void suppressHoverWhenOverContextMenu(GuiGraphicsExtractor g, int originX, int originY, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
      if (WaypointStorage.isNodeHidden(this.advancementNode)) {
         ci.cancel();
      } else {
         Minecraft mc = Minecraft.getInstance();
         Screen var10 = mc.gui.screen();
         if (var10 instanceof IAdvancementScreenCustom) {
            IAdvancementScreenCustom custom = (IAdvancementScreenCustom)var10;
            double realMx = mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getWidth();
            double realMy = mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getHeight();
            if (custom.advWaypoint_isMouseOverContextMenu(realMx, realMy)) {
               ci.cancel();
            }
         }

      }
   }

   @Redirect(
      method = {"extractRenderState"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
)
   )
   private void redirectBlitSpriteDraw(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
      guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.display), x, y, width, height);
   }

   @Redirect(
      method = {"extractHover"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
   ordinal = 3
)
   )
   private void redirectBlitSpriteHover(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
      guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.display), x, y, width, height);
   }
}
