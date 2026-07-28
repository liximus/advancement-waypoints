package com.listraind.advancementwaypoints.advancement;

import com.listraind.advancementwaypoints.navigator.Navigator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CoordParser {
   private static final Pattern COORD_PATTERN = Pattern.compile("X:\\s*(-?\\d+)\\s*Y:\\s*(-?\\d+)\\s*Z:\\s*(-?\\d+)", 2);
   private static final String[][] DIM_KEYWORDS = new String[][]{{"overworld", "верх", "обычн"}, {"nether", "ад", "незер", "нижн"}, {"end", "край", "энд"}};
   private static final Navigator.Dimension[] KEYWORD_DIMS;
   private static final Pattern DIM_BLOCK_PATTERN;
   private static final Pattern SINGLE_COORD;
   private static final Map<String, Integer> BLOCK_DIM_INDEX;
   private static final int[] SLOT_TO_NAV;
   public static final String[] DIM_LABEL_KEYS;
   private static final String[] DIM_LABEL_COLORS;
   private static final String[] DIM_LABELS_FALLBACK;
   private static final Navigator.Dimension[] NAV_DIMS;

   public static @Nullable Map<Navigator.Dimension, List<BlockPos>> parseForNavigation(String text) {
      if (text != null && !text.isEmpty()) {
         Map<Navigator.Dimension, List<BlockPos>> result = new EnumMap(Navigator.Dimension.class);

         for(Navigator.Dimension dim : NAV_DIMS) {
            result.put(dim, new ArrayList());
         }

         parseBlockFormat(text, result);
         if (result.values().stream().noneMatch((list) -> !list.isEmpty())) {
            String lower = text.toLowerCase();
            parseKeywordFormat(lower, text, result);
         }

         boolean hasAny = result.values().stream().anyMatch((list) -> !list.isEmpty());
         return hasAny ? result : null;
      } else {
         return null;
      }
   }

   private static void parseBlockFormat(String text, Map<Navigator.Dimension, List<BlockPos>> result) {
      Matcher block = DIM_BLOCK_PATTERN.matcher(text);

      while(block.find()) {
         String dimKey = block.group(1).toLowerCase();
         Integer slot = (Integer)BLOCK_DIM_INDEX.get(dimKey);
         if (slot != null) {
            Navigator.Dimension navDim = NAV_DIMS[SLOT_TO_NAV[slot]];
            Matcher c = SINGLE_COORD.matcher(block.group(2));

            while(c.find()) {
               ((List)result.get(navDim)).add(new BlockPos(Integer.parseInt(c.group(1)), Integer.parseInt(c.group(2)), Integer.parseInt(c.group(3))));
            }
         }
      }

   }

   private static void parseKeywordFormat(String lower, String text, Map<Navigator.Dimension, List<BlockPos>> result) {
      List<int[]> matches = new ArrayList();

      for(int d = 0; d < DIM_KEYWORDS.length; ++d) {
         int idx;
         for(String kw : DIM_KEYWORDS[d]) {
            for(idx = 0; (idx = lower.indexOf(kw, idx)) != -1; idx += kw.length()) {
               matches.add(new int[]{idx, d});
            }
         }
      }

      matches.sort(Comparator.comparingInt((a) -> a[0]));

      for(int i = 0; i < matches.size(); ++i) {
         int start = ((int[])matches.get(i))[0];
         int end = i + 1 < matches.size() ? ((int[])matches.get(i + 1))[0] : text.length();
         Matcher m = COORD_PATTERN.matcher(text.substring(start, end));

         while(m.find()) {
            ((List)result.get(KEYWORD_DIMS[((int[])matches.get(i))[1]])).add(new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
         }
      }

   }

   public static List<DimCoords> parseAllCoords(String desc) {
      List<List<String[]>> dimCoords = new ArrayList();

      for(int i = 0; i < 4; ++i) {
         dimCoords.add(new ArrayList());
      }

      Matcher block = DIM_BLOCK_PATTERN.matcher(desc);

      while(block.find()) {
         String dimKey = block.group(1).toLowerCase();
         Integer dimIdx = (Integer)BLOCK_DIM_INDEX.get(dimKey);
         if (dimIdx != null) {
            Matcher c = SINGLE_COORD.matcher(block.group(2));

            while(c.find()) {
               ((List)dimCoords.get(dimIdx)).add(new String[]{c.group(1), c.group(2), c.group(3)});
            }
         }
      }

      List<DimCoords> result = new ArrayList();

      for(int i = 0; i < 4; ++i) {
         if (!((List)dimCoords.get(i)).isEmpty()) {
            result.add(new DimCoords(i, (List)dimCoords.get(i)));
         }
      }

      return result;
   }

   public static String extractExtra(String desc) {
      String result;
      for(result = DIM_BLOCK_PATTERN.matcher(desc).replaceAll("").replaceAll("\\n{3,}", "\n\n").trim(); result.startsWith("§f"); result = result.substring(2)) {
      }

      return result;
   }

   public static String buildDescription(List<DimCoords> coords, String extra) {
      StringBuilder sb = new StringBuilder();
      if (extra != null && !extra.isEmpty()) {
         String cleanExtra;
         for(cleanExtra = extra; cleanExtra.startsWith("§f"); cleanExtra = cleanExtra.substring(2)) {
         }

         if (!cleanExtra.isEmpty()) {
            sb.append("§f").append(cleanExtra);
         }
      }

      for(DimCoords dc : coords) {
         if (!dc.coords().isEmpty()) {
            for(String[] c : dc.coords()) {
               if (sb.length() > 0) {
                  sb.append("\n\n");
               }

               sb.append(localizedDimLabel(dc.dim())).append(":\n");
               sb.append("§6X:").append(c[0]).append(" Y:").append(c[1]).append(" Z:").append(c[2]);
            }
         }
      }

      return sb.toString();
   }

   private static String localizedDimLabel(int slot) {
      String color = DIM_LABEL_COLORS[slot];

      try {
         String label = Component.translatable(DIM_LABEL_KEYS[slot]).getString();
         return !label.startsWith(color) && !label.startsWith("§") ? color + label : label;
      } catch (Exception var3) {
         return DIM_LABELS_FALLBACK[slot];
      }
   }

   static {
      KEYWORD_DIMS = new Navigator.Dimension[]{Navigator.Dimension.OVERWORLD, Navigator.Dimension.NETHER, Navigator.Dimension.END};
      DIM_BLOCK_PATTERN = Pattern.compile("(?:§[0-9a-f])?(overworld|nether|nether roof|end|Верхний мир|Крыша ада|Ад|Энд):\\s*\\n((?:\\s*§?6?X:-?\\d+\\s*Y:-?\\d+\\s*Z:-?\\d+\\s*\\n?)+)", 2);
      SINGLE_COORD = Pattern.compile("§?6?X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)");
      BLOCK_DIM_INDEX = new LinkedHashMap();
      BLOCK_DIM_INDEX.put("overworld", 0);
      BLOCK_DIM_INDEX.put("nether roof", 1);
      BLOCK_DIM_INDEX.put("nether", 2);
      BLOCK_DIM_INDEX.put("end", 3);
      BLOCK_DIM_INDEX.put("верхний мир", 0);
      BLOCK_DIM_INDEX.put("крыша ада", 1);
      BLOCK_DIM_INDEX.put("ад", 2);
      BLOCK_DIM_INDEX.put("энд", 3);
      SLOT_TO_NAV = new int[]{0, 1, 1, 2};
      DIM_LABEL_KEYS = new String[]{"advwp.dim.overworld", "advwp.dim.nether_roof", "advwp.dim.nether", "advwp.dim.end"};
      DIM_LABEL_COLORS = new String[]{"§2", "§4", "§c", "§e"};
      DIM_LABELS_FALLBACK = new String[]{"§2Верхний мир", "§4Крыша ада", "§cАд", "§eЭнд"};
      NAV_DIMS = Navigator.Dimension.values();
   }

   public static record DimCoords(int dim, List<String[]> coords) {
      public DimCoords(int dim) {
         this(dim, new ArrayList());
      }
   }
}
