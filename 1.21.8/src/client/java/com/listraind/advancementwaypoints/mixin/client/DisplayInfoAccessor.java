package com.listraind.advancementwaypoints.mixin.client;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayInfo.class)
public interface DisplayInfoAccessor {
    @Mutable
    @Accessor("description")
    void pepe_setDescription(Component description);

    @Mutable
    @Accessor("title")
    void pepe_setTitle(Component title);

    @Mutable
    @Accessor("icon")
    void pepe_setIcon(ItemStack icon);
}