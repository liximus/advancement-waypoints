package com.listraind.advancementwaypoints.mixin.client;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(AdvancementTab.class)
public interface AdvancementTabAccessor {
    @Accessor Map<AdvancementNode, AdvancementWidget> getWidgets();
    @Accessor double getScrollX();
    @Accessor double getScrollY();
}