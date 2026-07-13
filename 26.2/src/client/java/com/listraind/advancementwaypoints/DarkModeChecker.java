package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.DimensionPickerScreen;
import com.listraind.advancementwaypoints.gui.ItemPickerScreen;
import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.gui.WaypointFormScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;

public class DarkModeChecker {

    public static boolean isDarkModeEnabled() {
        for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
            if (pack.getTitle().getString().toLowerCase().contains("dark")
                    || pack.getId().toLowerCase().contains("dark")) {
                return true;
            }
        }
        return false;
    }

    public static void setModDarkMode() {
        boolean darkMode = isDarkModeEnabled();
        WaypointFormScreen.setDarkMode(darkMode);
        ItemPickerScreen.setDarkMode(darkMode);
        DimensionPickerScreen.setDarkMode(darkMode);
        MainMenuScreen.setDarkMode(darkMode);
    }
}