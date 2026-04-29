package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = BetterAdvancementTab.class, remap = false)
public abstract class BetterAdvancementTabMixin implements IBetterAdvancementTab {

    @Shadow
    protected Map<AdvancementHolder, BetterAdvancementWidget> widgets;
    @Shadow private int minX, maxX, minY, maxY;
    @Shadow private boolean centered;

    @Override
    public void advWp_recalculate() {
        for (BetterAdvancementWidget w : widgets.values()) {
            if (w instanceof IBetterAdvancementWidget iw) {
                iw.advWp_updatePosition();
            }
        }

        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;

        for (BetterAdvancementWidget w : widgets.values()) {
            int left = w.getX();
            int right = left + 28;
            int top = w.getY();
            int bottom = top + 27;
            this.minX = Math.min(this.minX, left);
            this.maxX = Math.max(this.maxX, right);
            this.minY = Math.min(this.minY, top);
            this.maxY = Math.max(this.maxY, bottom);
        }

        this.centered = false;
    }
}