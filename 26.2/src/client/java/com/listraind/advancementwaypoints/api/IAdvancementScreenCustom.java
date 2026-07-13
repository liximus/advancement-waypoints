package com.listraind.advancementwaypoints.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;


public interface IAdvancementScreenCustom {
    void advWaypoint_setSelectMode(Consumer<Identifier> cb);

    void advWaypoint_setScreenToOpen(Screen s);

    void advWaypoint_setParentScreen(Screen s);
}