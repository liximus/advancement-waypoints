package com.listraind.advancementwaypoints.advancementMixinHelpers;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public interface IAdvancementScreenCustom {
    void advWaypoint_setSelectModeStringToWrite(Consumer<ResourceLocation> idToSelect);
    void advWaypoint_setScreenToOpen(Screen screen);
}
