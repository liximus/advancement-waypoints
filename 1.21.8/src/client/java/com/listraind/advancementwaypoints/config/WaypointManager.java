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
            "(?:§[24e])?(Верхний мир|Крыша ада|Ад|Энд):\\s*\\n\\s*§6X:-?\\d+\\s*Y:-?\\d+\\s*Z:-?\\d+",
            Pattern.CASE_INSENSITIVE
    );

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

    public static String buildDescription(EditBox[][] coordFields, String extra) {
        StringBuilder sb = new StringBuilder();
        if (!extra.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(extra);
        }
        for (int i = 0; i < 4; i++) appendCoords(sb, DIM_LABELS[i], coordFields[i]);
        return sb.toString();
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
