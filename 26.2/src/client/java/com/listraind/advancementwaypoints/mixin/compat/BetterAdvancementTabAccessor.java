package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BetterAdvancementTab.class})
public interface BetterAdvancementTabAccessor {
   @Accessor
   Map<AdvancementHolder, BetterAdvancementWidget> getWidgets();

   @Accessor
   int getScrollX();

   @Accessor
   int getScrollY();
}
