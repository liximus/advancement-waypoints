package com.listraind.advancementwaypoints.mixin.client;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AdvancementWidget.class)
public interface AdvancementWidgetAccessor {
    @Accessor
    AdvancementNode getAdvancementNode();

    @Invoker
    boolean invokeIsMouseOver(int scrollX, int scrollY, int mouseX, int mouseY);
}