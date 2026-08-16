package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = {BetterAdvancementTab.class}, remap = false)
public interface BetterAdvancementTabAccessor {
    @Accessor
    Map<AdvancementHolder, BetterAdvancementWidget> getWidgets();

    @Accessor
    int getScrollX();

    @Accessor
    void setScrollX(int scrollX);

    @Accessor
    int getScrollY();

    @Accessor
    void setScrollY(int scrollY);

    @Accessor
    boolean isCentered();

    @Accessor
    void setCentered(boolean centered);

    @Accessor
    int getMinX();

    @Accessor
    void setMinX(int minX);

    @Accessor
    int getMaxX();

    @Accessor
    void setMaxX(int maxX);

    @Accessor
    int getMinY();

    @Accessor
    void setMinY(int minY);

    @Accessor
    int getMaxY();

    @Accessor
    void setMaxY(int maxY);
}
