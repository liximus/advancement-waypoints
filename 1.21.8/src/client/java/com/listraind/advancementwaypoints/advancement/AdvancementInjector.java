package com.listraind.advancementwaypoints.advancement;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;

import java.util.*;

public class AdvancementInjector {

    public record LoadResult(List<WaypointData> advancements, Map<String, float[]> vanillaOverrides) {}

    public static LoadResult load(net.minecraft.advancements.AdvancementTree tree) {
        List<JsonObject> custom = WaypointStorage.loadWaypoints();
        if (custom.isEmpty()) return new LoadResult(List.of(), Map.of());

        LayoutCalculator calc = new LayoutCalculator();
        calc.calculate(custom, tree);

        Map<String, float[]> pos = calc.getPositions();
        List<WaypointData> result = new ArrayList<>();

        for (JsonObject o : custom) {
            String id = ConfigIO.str(o, "id", "");
            float[] p = pos.getOrDefault(id, new float[]{0f, 0f});

            String parent = ConfigIO.nullable(o, "parent");
            String bg = ConfigIO.nullable(o, "background");

            boolean isRoot = parent == null || parent.isEmpty();
            if (isRoot && (bg == null || bg.isEmpty())) {
                bg = "minecraft:torch";
            }

            AdvancementWaypoints.LOGGER.info(bg);

            result.add(new WaypointData(
                    id,
                    ConfigIO.str(o, "icon", "minecraft:paper"),
                    ConfigIO.str(o, "title", "???"),
                    ConfigIO.str(o, "description", ""),
                    ConfigIO.str(o, "frame", "task"),
                    bg,
                    parent,
                    p[0], p[1]
            ));
        }
        return new LoadResult(result, calc.getVanillaShifts());
    }
}