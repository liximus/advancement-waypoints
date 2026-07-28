package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {BetterAdvancementTab.class},
   remap = false
)
public abstract class BetterAdvancementTabMixin implements IBetterAdvancementTab {
   @Shadow
   protected Map<AdvancementHolder, BetterAdvancementWidget> widgets;
   @Shadow
   private int minX;
   @Shadow
   private int maxX;
   @Shadow
   private int minY;
   @Shadow
   private int maxY;
   @Shadow
   private boolean centered;

   @Inject(
      method = {"storeScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void advWp_guardStoreScroll(CallbackInfo ci) {
      if (!this.centered) {
         ci.cancel();
      }

   }

   public void advWp_recalculate() {
      if (this.widgets != null && !this.widgets.isEmpty()) {
         for(BetterAdvancementWidget w : this.widgets.values()) {
            if (w instanceof IBetterAdvancementWidget) {
               IBetterAdvancementWidget iw = (IBetterAdvancementWidget)w;
               iw.advWp_updatePosition();
            }
         }

         int newMinX = Integer.MAX_VALUE;
         int newMaxX = Integer.MIN_VALUE;
         int newMinY = Integer.MAX_VALUE;
         int newMaxY = Integer.MIN_VALUE;

         for(BetterAdvancementWidget w : this.widgets.values()) {
            int left = w.getX();
            int right = left + 28;
            int top = w.getY();
            int bottom = top + 27;
            newMinX = Math.min(newMinX, left);
            newMaxX = Math.max(newMaxX, right);
            newMinY = Math.min(newMinY, top);
            newMaxY = Math.max(newMaxY, bottom);
         }

         if (newMinX != Integer.MAX_VALUE) {
            this.minX = newMinX;
            this.maxX = newMaxX;
            this.minY = newMinY;
            this.maxY = newMaxY;
            this.centered = false;
         }
      }
   }
}
