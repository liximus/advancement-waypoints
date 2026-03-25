package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient.ParsedAdvancement;

import java.util.*;

import net.minecraft.advancements.AdvancementTree;

public class AdvancementLoader {

    public static class LoadResult {
        public final List<ParsedAdvancement> advancements;
        public final Map<String, float[]> vanillaOverrides;

        public LoadResult(List<ParsedAdvancement> adv, Map<String, float[]> vo) {
            this.advancements = adv;
            this.vanillaOverrides = vo;
        }
    }

    public static LoadResult loadAll(AdvancementTree tree) {
        List<JsonObject> custom = ConfigManager.loadCustomAdvancements();
        if (custom.isEmpty()) return new LoadResult(List.of(), Map.of());

        LayoutCalculator calc = new LayoutCalculator();
        calc.calculate(custom, tree);

        Map<String, float[]> positions = calc.getComputedPositions();
        List<ParsedAdvancement> result = new ArrayList<>();

        for (JsonObject o : custom) {
            String id = o.has("id") ? o.get("id").getAsString() : "";
            float[] pos = positions.getOrDefault(id, new float[]{0f, 0f});

            result.add(new ParsedAdvancement(
                    id,
                    o.has("icon") ? o.get("icon").getAsString() : "minecraft:paper",
                    o.has("title") ? o.get("title").getAsString() : "???",
                    o.has("description") ? o.get("description").getAsString() : "",
                    o.has("frame") ? o.get("frame").getAsString() : "task",
                    o.has("background") && !o.get("background").isJsonNull() ? o.get("background").getAsString() : null,
                    o.has("parent") && !o.get("parent").isJsonNull() ? o.get("parent").getAsString() : null,
                    pos[0], pos[1]
            ));
        }

        return new LoadResult(result, calc.getVanillaOverrides());
    }
}