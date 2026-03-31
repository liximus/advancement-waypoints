package com.listraind.advancementwaypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.Pack;

import java.util.Collection;

public class DarkModeChecker {
    public static boolean isPackEnabled(String name) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository packRepo = mc.getResourcePackRepository();

        Collection<Pack> selectedPacks = packRepo.getSelectedPacks();

        for (Pack pack : selectedPacks) {
            // ID (имя файла/папки)
            String id = pack.getId();
            // Отображаемое имя
            String title = pack.getTitle().getString();

            if (title.contains(name) || id.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static void printAllPacks() {
        Minecraft mc = Minecraft.getInstance();
        PackRepository packRepo = mc.getResourcePackRepository();

        for (Pack pack : packRepo.getSelectedPacks()) {
            System.out.println("ID: " + pack.getId());
            System.out.println("Title: " + pack.getTitle().getString());
            System.out.println("Description: " + pack.getDescription().getString());
            System.out.println("---");
        }
    }
}