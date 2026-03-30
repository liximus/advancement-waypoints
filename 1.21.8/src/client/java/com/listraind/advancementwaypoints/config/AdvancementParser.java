package com.listraind.advancementwaypoints.config;

import com.listraind.advancementwaypoints.navigator.ArrowModule;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancementParser {
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "X:\\s*(-?\\d+)\\s*Y:\\s*(-?\\d+)\\s*Z:\\s*(-?\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[][] KEYWORDS = {
            {"верх", "обычн", "overworld"},          // OVERWORLD
            {"ад", "незер", "нижн", "nether"},       // NETHER
            {"край", "энд", "end"},                  // END
    };

    private static final ArrowModule.Dimension[] DIMS = {
            ArrowModule.Dimension.OVERWORLD,
            ArrowModule.Dimension.NETHER,
            ArrowModule.Dimension.END,
    };


    public static Map<ArrowModule.Dimension, BlockPos> parseAdvancement(String text) {
        Map<ArrowModule.Dimension, BlockPos> result = new EnumMap<>(ArrowModule.Dimension.class);
        String lower = text.toLowerCase();

        TreeMap<Integer, ArrowModule.Dimension> sections = new TreeMap<>();

        for (int d = 0; d < KEYWORDS.length; d++) {
            for (String kw : KEYWORDS[d]) {
                int idx = lower.indexOf(kw);
                if (idx != -1) {
                    sections.putIfAbsent(idx, DIMS[d]);
                }
            }
        }

        List<Map.Entry<Integer, ArrowModule.Dimension>> entries = new ArrayList<>(sections.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            int start = entries.get(i).getKey();
            int end = (i + 1 < entries.size()) ? entries.get(i + 1).getKey() : text.length();

            Matcher m = COORD_PATTERN.matcher(text.substring(start, end));
            if (m.find()) {
                result.put(entries.get(i).getValue(), new BlockPos(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                ));
            }
        }

        return result;
    }
}
