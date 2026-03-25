package com.listraind.advancementwaypoints.config;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.Command;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

public class jsonGenerator {

    private static final String[] DIM_LABELS = {"Верхний мир", "Крыша ада", "Ад", "Энд"};

    public static void generateAndSave(String name, EditBox[][] coordFields, String iconId,
                                       ResourceLocation parentId, String desc) {
        if (name.isEmpty()) name = "waypoint";
        String id = "advwaypoints:" + toId(name);
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
        Command.reloadAdvancements();
    }

    private static String buildDescription(EditBox[][] coordFields, String extra) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) appendCoords(sb, DIM_LABELS[i], coordFields[i]);
        if (!extra.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(extra);
        }
        return sb.toString();
    }

    private static void appendCoords(StringBuilder sb, String label, EditBox[] f) {
        String x = f[0].getValue().trim(), y = f[1].getValue().trim(), z = f[2].getValue().trim();
        if (x.isEmpty() && y.isEmpty() && z.isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(label).append(":\n  X:").append(x).append(" Y:").append(y).append(" Z:").append(z);
    }

    private static String toId(String name) {
        String id = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return id.isEmpty() ? "wp_" + (System.currentTimeMillis() % 100000) : id;
    }
}