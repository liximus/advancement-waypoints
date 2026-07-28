package com.listraind.advancementwaypoints.mixin.compat;

import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {AdvancementWidget.class},
   priority = 100
)
public abstract class PlaneAdvancementWidgetMixin {
   @Shadow
   private AdvancementNode advancementNode;

   @Inject(
      method = {"planeAdvancements$renderLines"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void suppressPlaneLinesIfHidden(GuiGraphicsExtractor g, int x, int y, boolean isRoot, CallbackInfo ci) {
      if (WaypointStorage.isNodeHidden(this.advancementNode)) {
         ci.cancel();
      }

   }
}
