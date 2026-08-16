package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import com.listraind.advancementwaypoints.advancement.TextureHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AdvancementTab.class})
public class AdvancementTabMixin {
   @Inject(
      method = {"extractContents"},
      at = {@At("HEAD")}
   )
   private void onExtractContents(GuiGraphicsExtractor g, int x, int y, CallbackInfo ci) {
      AdvancementTabCapture.set(x, y);
   }

   @Redirect(
      method = {"extractContents"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
      )
   )
   private void advWp_redirectTabBackgroundBlit(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
      TextureHelper.blitBackground(g, pipeline, texture, x, y, width, height);
   }
}
