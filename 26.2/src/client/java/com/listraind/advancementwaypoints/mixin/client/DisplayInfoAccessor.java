package com.listraind.advancementwaypoints.mixin.client;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayInfo.class)
public interface DisplayInfoAccessor {
    @Mutable
    @Accessor("description")
    void advWp_setDescription(Component d);

    @Mutable
    @Accessor("title")
    void advWp_setTitle(Component t);

    @Mutable
    @Accessor("icon")
    void advWp_setIcon(ItemStackTemplate i);
}