package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.DimensionPickerScreen;
import com.listraind.advancementwaypoints.gui.ItemPickerScreen;
import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.gui.WaypointFormScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.Pack;

import java.util.Collection;

public class DarkModeChecker {
    public static boolean isDarkModeEnabled() {
        Minecraft mc = Minecraft.getInstance();
        PackRepository packRepo = mc.getResourcePackRepository();

        Collection<Pack> selectedPacks = packRepo.getSelectedPacks();

        for (Pack pack : selectedPacks) {
            String id = pack.getId();
            String title = pack.getTitle().getString();

            if (title.toLowerCase().contains("dark") || id.toLowerCase().contains("dark")) {
                return true;
            }
        }
        return false;
    }

    public static void setModDarkMode() {
        boolean dm = isDarkModeEnabled();
        WaypointFormScreen.setDarkMode(dm);
        ItemPickerScreen.setDarkMode(dm);
        DimensionPickerScreen.setDarkMode(dm);
        MainMenuScreen.setDarkMode(dm);
    }

}