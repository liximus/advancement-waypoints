package com.listraind.advancementwaypoints.mixin.client;

import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AdvancementTab.class})
public interface AdvancementTabAccessor {
   @Accessor
   Map<AdvancementHolder, ?> getWidgets();

   @Accessor
   double getScrollX();

   @Accessor
   double getScrollY();
}
