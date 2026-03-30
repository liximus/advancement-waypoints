package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.Command;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointManager {

    private static final String[] DIM_LABELS = {"§2Верхний мир", "§4Крыша ада", "§4Ад", "§eЭнд"};

    private static final Pattern[] SECTION_PATTERNS = {
            Pattern.compile("§2Верхний мир:\\s*\\n\\s*§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("§4Крыша ада:\\s*\\n\\s*§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("§4(?<!Крыша )Ад:\\s*\\n\\s*§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("§eЭнд:\\s*\\n\\s*§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)", Pattern.CASE_INSENSITIVE),
    };

    private static final Pattern ALL_COORD_BLOCKS = Pattern.compile(
            "(?:§[24e])?(Верхний мир|Крыша ада|Ад|Энд):\\s*\\n((?:\\s*§6X:-?\\d+\\s*Y:-?\\d+\\s*Z:-?\\d+\\s*\\n?)+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SINGLE_COORD = Pattern.compile(
            "§6X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    public static class CoordsPerDimension {
        public final int dimIndex;
        public final List<String[]> coords;
        
        public CoordsPerDimension(int dimIndex) {
            this.dimIndex = dimIndex;
            this.coords = new ArrayList<>();
        }
        
        public void add(String x, String y, String z) {
            coords.add(new String[]{x, y, z});
        }
    }

    public static List<CoordsPerDimension> parseAllCoordsFromDescription(String desc) {
        List<CoordsPerDimension> result = new ArrayList<>();
        List<List<String[]>> dimCoords = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            dimCoords.add(new ArrayList<>());
        }

        Matcher entryMatcher = ALL_COORD_BLOCKS.matcher(desc);
        while (entryMatcher.find()) {
            String dimName = entryMatcher.group(1);
            String coordsBlock = entryMatcher.group(2);

            int dimIndex = getDimensionIndex(dimName);
            if (dimIndex == -1) continue;

            Matcher coordMatcher = SINGLE_COORD.matcher(coordsBlock);
            while (coordMatcher.find()) {
                dimCoords.get(dimIndex).add(new String[]{
                        coordMatcher.group(1),
                        coordMatcher.group(2),
                        coordMatcher.group(3)
                });
            }
        }

        for (int i = 0; i < 4; i++) {
            if (!dimCoords.get(i).isEmpty()) {
                CoordsPerDimension data = new CoordsPerDimension(i);
                for (String[] c : dimCoords.get(i)) {
                    data.add(c[0], c[1], c[2]);
                }
                result.add(data);
            }
        }

        return result;
    }

    private static int getDimensionIndex(String dimName) {
        if (dimName.equalsIgnoreCase("Верхний мир")) return 0;
        if (dimName.equalsIgnoreCase("Крыша ада")) return 1;
        if (dimName.equalsIgnoreCase("Ад")) return 2;
        if (dimName.equalsIgnoreCase("Энд")) return 3;
        return -1;
    }

    public static void generateAndSave(String name, EditBox[][] coordFields, String iconId,
                                       ResourceLocation parentId, String desc) {
        if (name.isEmpty()) name = "waypoint";
        String id = "advwaypoints:" + randId();
        String description = buildDescription(coordFields, desc);

        List<JsonObject> existing = ConfigManager.loadCustomAdvancements();
        JsonObject found = null;
        for (JsonObject o : existing)
            if (o.get("id").getAsString().equals(id)) { found = o; break; }

        if (found != null) {
            found.addProperty("title", name);
            found.addProperty("description", description);
            found.addProperty("icon", iconId);
            if (parentId != null) {
                found.addProperty("parent", parentId.toString());
            } else {
                found.addProperty("parent", "");
            }
            found.remove("x");
            found.remove("y");
        } else {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id);
            entry.addProperty("icon", iconId);
            entry.addProperty("title", name);
            entry.addProperty("description", description);
            entry.addProperty("frame", "task");
            entry.addProperty("parent", parentId != null ? parentId.toString() : "");
            existing.add(entry);
        }

        ConfigManager.saveCustomAdvancements(existing);
        ConfigManager.setLastParent(ResourceLocation.parse(id));
        Command.reloadAdvancements();
    }

    public static void saveWaypoint(String originalId, String name, String description, String iconId, ResourceLocation selectedParentId) {
        if (name.isEmpty()) name = "waypoint";

        List<JsonObject> all = ConfigManager.loadCustomAdvancements();

        JsonObject found = null;
        for (JsonObject o : all) {
            if (o.has("id") && o.get("id").getAsString().equals(originalId)) {
                found = o;
                break;
            }
        }

        if (found == null) {
            found = new JsonObject();
            found.addProperty("id", originalId);
            all.add(found);
        }

        found.addProperty("title", name);
        found.addProperty("description", description);
        found.addProperty("icon", iconId);
        if (selectedParentId != null) {
            found.addProperty("parent", selectedParentId.toString());
        } else {
            found.addProperty("parent", "");
        }
        found.remove("x");
        found.remove("y");

        ConfigManager.saveCustomAdvancements(all);
        Command.reloadAdvancements();
    }

    public static void deleteWaypoint(String originalId) {
        List<JsonObject> all = ConfigManager.loadCustomAdvancements();
        all.removeIf(o -> o.has("id") && o.get("id").getAsString().equals(originalId));
        ConfigManager.saveCustomAdvancements(all);
        Command.reloadAdvancements();
    }

    public static JsonObject getWaypointData(ResourceLocation id) {
        List<JsonObject> customList = ConfigManager.loadCustomAdvancements();
        for (JsonObject obj : customList) {
            if (obj.has("id") && obj.get("id").getAsString().equals(id.toString())) {
                return obj;
            }
        }

        JsonObject fakeObj = new JsonObject();
        fakeObj.addProperty("id", id.toString());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            AdvancementNode node = mc.player.connection.getAdvancements().getTree().get(id);
            if (node != null && node.holder().value().display().isPresent()) {
                var display = node.holder().value().display().get();
                fakeObj.addProperty("title", display.getTitle().getString());
                fakeObj.addProperty("description", display.getDescription().getString());

                ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(display.getIcon().getItem());
                fakeObj.addProperty("icon", iconId != null ? iconId.toString() : "minecraft:stone");

                if (node.parent() != null) {
                    fakeObj.addProperty("parent", node.parent().holder().id().toString());
                }
            }
        }
        return fakeObj;
    }

    public static String buildDescription(EditBox[][] coordFields, String extra, List<CoordsPerDimension> allOriginalCoords) {
        StringBuilder sb = new StringBuilder();
        if (!extra.isEmpty()) {
            sb.append(extra);
        }

        boolean[] hasNewCoords = new boolean[4];
        for (int i = 0; i < 4; i++) {
            String x = coordFields[i][0].getValue().trim();
            hasNewCoords[i] = !x.isEmpty();
        }

        for (int i = 0; i < 4; i++) {
            CoordsPerDimension original = null;
            for (CoordsPerDimension cpd : allOriginalCoords) {
                if (cpd.dimIndex == i) {
                    original = cpd;
                    break;
                }
            }

            if (hasNewCoords[i]) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(DIM_LABELS[i]).append(":\n");

                String x = coordFields[i][0].getValue().trim();
                String y = coordFields[i][1].getValue().trim();
                String z = coordFields[i][2].getValue().trim();
                
                String[] xParts = x.split(";;");
                String[] yParts = y.split(";;");
                String[] zParts = z.split(";;");
                
                for (int j = 0; j < xParts.length; j++) {
                    if (j > 0) sb.append("\n");
                    String xVal = xParts[j].trim().isEmpty() ? "0" : xParts[j].trim();
                    String yVal = j < yParts.length && !yParts[j].trim().isEmpty() ? yParts[j].trim() : "0";
                    String zVal = j < zParts.length && !zParts[j].trim().isEmpty() ? zParts[j].trim() : "0";
                    sb.append("§6X:").append(xVal)
                      .append(" Y:").append(yVal)
                      .append(" Z:").append(zVal);
                }

                if (original != null && original.coords.size() > xParts.length) {
                    for (int j = xParts.length; j < original.coords.size(); j++) {
                        String[] c = original.coords.get(j);
                        sb.append("\n§6X:").append(c[0]).append(" Y:").append(c[1]).append(" Z:").append(c[2]);
                    }
                }
            } else if (original != null) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(DIM_LABELS[i]).append(":\n");
                for (int j = 0; j < original.coords.size(); j++) {
                    String[] c = original.coords.get(j);
                    if (j > 0) sb.append("\n");
                    sb.append("§6X:").append(c[0]).append(" Y:").append(c[1]).append(" Z:").append(c[2]);
                }
            }
        }

        return sb.toString();
    }

    public static String buildDescription(EditBox[][] coordFields, String extra) {
        return buildDescription(coordFields, extra, new ArrayList<>());
    }


    private static void appendCoords(StringBuilder sb, String label, EditBox[] f) {
        String x = f[0].getValue().trim(), y = f[1].getValue().trim(), z = f[2].getValue().trim();
        if (x.isEmpty() && y.isEmpty() && z.isEmpty()) return;
        if (sb.length() > 0) sb.append("\n\n");
        sb.append(label).append(":\n§6X:").append(x).append(" Y:").append(y).append(" Z:").append(z);
    }


    public static String[][] parseCoordsFromDescription(String desc) {
        String[][] parsed = new String[4][3];
        for (int i = 0; i < 4; i++) parsed[i] = new String[]{"", "", ""};

        for (int i = 0; i < SECTION_PATTERNS.length; i++) {
            Matcher m = SECTION_PATTERNS[i].matcher(desc);
            if (m.find()) {
                parsed[i][0] = m.group(1);
                parsed[i][1] = m.group(2);
                parsed[i][2] = m.group(3);
            }
        }
        return parsed;
    }

    public static String parseExtraFromDescription(String desc) {
        String extra = ALL_COORD_BLOCKS.matcher(desc).replaceAll("").trim();
        extra = extra.replaceAll("\\n{3,}", "\n\n").trim();
        return extra;
    }

    public static Item getIconFromJson(JsonObject o) {
        String iconStr = o.has("icon") ? o.get("icon").getAsString() : "minecraft:grass_block";
        try {
            return BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(iconStr));
        } catch (Exception e) {
            return Items.GRASS_BLOCK;
        }
    }

    public static ResourceLocation getParentFromJson(JsonObject o) {
        if (o.has("parent")) {
            String parentStr = o.get("parent").getAsString();
            if (parentStr != null && !parentStr.isEmpty()) {
                return ResourceLocation.parse(parentStr);
            }
        }
        return null;
    }

    private static String randId() {
        return "wp_" + (System.currentTimeMillis() % 10000000);
    }
}
