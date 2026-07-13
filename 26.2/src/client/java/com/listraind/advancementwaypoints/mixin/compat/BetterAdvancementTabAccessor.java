package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(betteradvancements.common.gui.BetterAdvancementTab.class)
public interface BetterAdvancementTabAccessor {
    @Accessor
    Map<AdvancementHolder, BetterAdvancementWidget> getWidgets();

    @Accessor
    int getScrollX();

    @Accessor
    int getScrollY();
}