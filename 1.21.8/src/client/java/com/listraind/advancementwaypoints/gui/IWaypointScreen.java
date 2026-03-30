package com.listraind.advancementwaypoints.gui;

import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public interface IWaypointScreen {
    void openItemPicker(Consumer<Item> onItemSelected);
    void closeItemPicker();
}
