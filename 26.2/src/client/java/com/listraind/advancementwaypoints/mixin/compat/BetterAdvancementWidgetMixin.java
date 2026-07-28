package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import com.listraind.advancementwaypoints.gui.base.WidgetFrameHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {BetterAdvancementWidget.class},
   remap = false
)
public class BetterAdvancementWidgetMixin implements IBetterAdvancementWidget {
   @Shadow(
      remap = false
   )
   private AdvancementNode advancementNode;
   @Shadow(
      remap = false
   )
   private DisplayInfo displayInfo;
   @Shadow
   public int x;
   @Shadow
   public int y;
   @Shadow
   private BetterDisplayInfo betterDisplayInfo;

   @Redirect(
      method = {"draw"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
),
      remap = true
   )
   private void redirectBlitSprite(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
      guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.displayInfo), x, y, width, height, color);
   }

   @Redirect(
      method = {"drawHover"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
),
      remap = true
   )
   private void redirectBlitSpriteHover(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
      guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.displayInfo), x, y, width, height, color);
   }

   public void advWp_updatePosition() {
      this.x = this.betterDisplayInfo.getPosX() != null ? this.betterDisplayInfo.getPosX() : Mth.floor(this.displayInfo.getX() * 32.0F);
      this.y = this.betterDisplayInfo.getPosY() != null ? this.betterDisplayInfo.getPosY() : Mth.floor(this.displayInfo.getY() * 27.0F);
   }
}
