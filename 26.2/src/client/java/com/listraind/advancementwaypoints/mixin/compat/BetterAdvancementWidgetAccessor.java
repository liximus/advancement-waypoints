package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = {BetterAdvancementWidget.class}, remap = false)
public interface BetterAdvancementWidgetAccessor {
    @Accessor
    AdvancementNode getAdvancementNode();

    @Accessor
    DisplayInfo getDisplayInfo();
}
