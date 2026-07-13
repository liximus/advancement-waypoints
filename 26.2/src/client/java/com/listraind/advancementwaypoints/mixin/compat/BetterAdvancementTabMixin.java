package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = BetterAdvancementTab.class, remap = false)
public abstract class BetterAdvancementTabMixin implements IBetterAdvancementTab {

    @Shadow
    protected Map<AdvancementHolder, BetterAdvancementWidget> widgets;
    @Shadow
    private int minX, maxX, minY, maxY;
    @Shadow
    private boolean centered;

    @Inject(method = "storeScroll", at = @At("HEAD"), cancellable = true)
    private void advWp_guardStoreScroll(CallbackInfo ci) {
        if (!centered) ci.cancel();
    }

    @Override
    public void advWp_recalculate() {
        if (widgets == null || widgets.isEmpty()) return;

        for (BetterAdvancementWidget w : widgets.values()) {
            if (w instanceof IBetterAdvancementWidget iw) {
                iw.advWp_updatePosition();
            }
        }

        int newMinX = Integer.MAX_VALUE;
        int newMaxX = Integer.MIN_VALUE;
        int newMinY = Integer.MAX_VALUE;
        int newMaxY = Integer.MIN_VALUE;

        for (BetterAdvancementWidget w : widgets.values()) {
            int left = w.getX();
            int right = left + 28;
            int top = w.getY();
            int bottom = top + 27;
            newMinX = Math.min(newMinX, left);
            newMaxX = Math.max(newMaxX, right);
            newMinY = Math.min(newMinY, top);
            newMaxY = Math.max(newMaxY, bottom);
        }

        if (newMinX == Integer.MAX_VALUE) return;

        this.minX = newMinX;
        this.maxX = newMaxX;
        this.minY = newMinY;
        this.maxY = newMaxY;

        this.centered = false;
    }
}