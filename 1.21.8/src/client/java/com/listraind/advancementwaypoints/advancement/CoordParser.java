package com.listraind.advancementwaypoints.advancement;

import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordParser {

    private static final Pattern COORD_PATTERN = Pattern.compile(
            "X:\\s*(-?\\d+)\\s*Y:\\s*(-?\\d+)\\s*Z:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE
    );

    private static final String[][] KEYWORDS = {
            {"верх", "обычн", "overworld"},
            {"ад", "незер", "нижн", "nether"},
            {"край", "энд", "end"},
    };

    private static final Navigator.Dimension[] DIMS = {
            Navigator.Dimension.OVERWORLD,
            Navigator.Dimension.NETHER,
            Navigator.Dimension.END,
    };

    public static final String[] DIM_LABELS = {"§2Верхний мир", "§4Крыша ада", "§cАд", "§eЭнд"};

    private static final Pattern COORD_BLOCK_PATTERN = Pattern.compile(
            "(?:§[0-9a-f])?(Верхний мир|Крыша ада|Ад|Энд):\\s*\\n((?:\\s*§6X:-?\\d+\\s*Y:-?\\d+\\s*Z:-?\\d+\\s*\\n?)+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SINGLE_COORD = Pattern.compile(
            "§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)", Pattern.CASE_INSENSITIVE
    );

    public static Map<Navigator.Dimension, List<BlockPos>> parseForNavigation(String text) {
        Map<Navigator.Dimension, List<BlockPos>> result = new EnumMap<>(Navigator.Dimension.class);
        for (Navigator.Dimension dim : Navigator.Dimension.values()) result.put(dim, new ArrayList<>());

        String lower = text.toLowerCase();
        List<int[]> matches = new ArrayList<>();

        for (int d = 0; d < KEYWORDS.length; d++) {
            for (String kw : KEYWORDS[d]) {
                int idx = 0;
                while ((idx = lower.indexOf(kw, idx)) != -1) {
                    matches.add(new int[]{idx, d});
                    idx += kw.length();
                }
            }
        }
        matches.sort(Comparator.comparingInt(a -> a[0]));

        for (int i = 0; i < matches.size(); i++) {
            int start = matches.get(i)[0];
            int end = (i + 1 < matches.size()) ? matches.get(i + 1)[0] : text.length();
            Matcher m = COORD_PATTERN.matcher(text.substring(start, end));
            while (m.find()) {
                result.get(DIMS[matches.get(i)[1]]).add(new BlockPos(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                ));
            }
        }
        return result;
    }

    public static List<DimCoords> parseAllCoords(String desc) {
        List<List<String[]>> dimCoords = new ArrayList<>();
        for (int i = 0; i < 4; i++) dimCoords.add(new ArrayList<>());

        Matcher block = COORD_BLOCK_PATTERN.matcher(desc);
        while (block.find()) {
            int dimIdx = dimIndex(block.group(1));
            if (dimIdx == -1) continue;
            Matcher c = SINGLE_COORD.matcher(block.group(2));
            while (c.find()) {
                dimCoords.get(dimIdx).add(new String[]{c.group(1), c.group(2), c.group(3)});
            }
        }

        List<DimCoords> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!dimCoords.get(i).isEmpty()) result.add(new DimCoords(i, dimCoords.get(i)));
        }
        return result;
    }

    public static String extractExtra(String desc) {
        return COORD_BLOCK_PATTERN.matcher(desc).replaceAll("").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static String buildDescription(List<DimCoords> coords, String extra) {
        StringBuilder sb = new StringBuilder();
        if (extra != null && !extra.isEmpty()) sb.append("§f").append(extra);

        for (DimCoords dc : coords) {
            if (dc.coords().isEmpty()) continue;
            for (String[] c : dc.coords()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(DIM_LABELS[dc.dim()]).append(":\n");
                sb.append("§6X:").append(c[0]).append(" Y:").append(c[1]).append(" Z:").append(c[2]);
            }
        }
        return sb.toString();
    }

    private static int dimIndex(String name) {
        return switch (name.toLowerCase()) {
            case "верхний мир" -> 0;
            case "крыша ада" -> 1;
            case "ад" -> 2;
            case "энд" -> 3;
            default -> -1;
        };
    }

    public record DimCoords(int dim, List<String[]> coords) {
        public DimCoords(int dim) { this(dim, new ArrayList<>()); }
    }
}