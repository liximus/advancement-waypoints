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


    public static Map<ArrowModule.Dimension, List<BlockPos>> parseAdvancement(String text) {
        Map<ArrowModule.Dimension, List<BlockPos>> result = new EnumMap<>(ArrowModule.Dimension.class);
        for (ArrowModule.Dimension dim : ArrowModule.Dimension.values()) {
            result.put(dim, new ArrayList<>());
        }
        
        String lower = text.toLowerCase();

        List<KeywordMatch> allMatches = new ArrayList<>();
        for (int d = 0; d < KEYWORDS.length; d++) {
            for (String kw : KEYWORDS[d]) {
                int idx = 0;
                while ((idx = lower.indexOf(kw, idx)) != -1) {
                    allMatches.add(new KeywordMatch(idx, DIMS[d]));
                    idx += kw.length();
                }
            }
        }

        allMatches.sort(Comparator.comparingInt(m -> m.position));

        for (int i = 0; i < allMatches.size(); i++) {
            int start = allMatches.get(i).getKey();
            int end = (i + 1 < allMatches.size()) ? allMatches.get(i + 1).getKey() : text.length();

            String sectionText = text.substring(start, end);
            Matcher m = COORD_PATTERN.matcher(sectionText);

            ArrowModule.Dimension dim = allMatches.get(i).getValue();
            List<BlockPos> coords = result.get(dim);
            while (m.find()) {
                coords.add(new BlockPos(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                ));
            }
        }

        return result;
    }

    private static class KeywordMatch implements Map.Entry<Integer, ArrowModule.Dimension> {
        final int position;
        final ArrowModule.Dimension dimension;

        KeywordMatch(int position, ArrowModule.Dimension dimension) {
            this.position = position;
            this.dimension = dimension;
        }

        @Override
        public Integer getKey() { return position; }

        @Override
        public ArrowModule.Dimension getValue() { return dimension; }

        @Override
        public ArrowModule.Dimension setValue(ArrowModule.Dimension value) { throw new UnsupportedOperationException(); }
    }
}
