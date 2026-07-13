package com.listraind.advancementwaypoints.gui;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import betteradvancements.fabric.config.ConfigFileHandler;

import java.util.List;

public class EditWaypointScreen extends WaypointFormScreen {

    private final String originalId;

    public EditWaypointScreen(JsonObject data) {
        super(Component.translatable("advwp.edit.title"));
        this.originalId = ConfigIO.str(data, "id", "");
        this.isVanilla = !originalId.startsWith("advwaypoints:");

        savedName = ConfigIO.str(data, "title", "").replace('§', '&');
        savedBackground = ConfigIO.nullable(data, "background");
        if (savedBackground == null) savedBackground = "";

        String iconStr = ConfigIO.str(data, "icon", "minecraft:grass_block");
        try {
            selectedIcon = BuiltInRegistries.ITEM.getValue(Identifier.parse(iconStr));
        } catch (Exception e) {
            selectedIcon = Items.GRASS_BLOCK;
        }

        String parentStr = ConfigIO.nullable(data, "parent");
        if (parentStr != null && !parentStr.isEmpty()) {
            selectedParentId = Identifier.parse(parentStr);
            hadParentBefore = true;
        }

        String desc = ConfigIO.str(data, "description", "");
        List<CoordParser.DimCoords> parsed = CoordParser.parseAllCoords(desc);
        for (CoordParser.DimCoords dc : parsed) {
            for (String[] c : dc.coords()) {
                coordRows.add(new CoordRow(dc.dim(), c[0], c[1], c[2]));
            }
        }
        savedDesc = CoordParser.extractExtra(desc);
    }

    @Override
    protected void init() {
        super.init();
        if (isVanilla) {
            nameField.active = false;
            descField.active = false;
            iconButton.active = false;
            parentButton.active = false;
            if (bgButton != null) bgButton.active = false;
        }
    }

    @Override
    protected void initActions(int cx, int y) {
        if (isVanilla) {
            addRenderableWidget(Button.builder(Component.translatable("advwp.button.read_only"), b -> {
            }).bounds(cx - 105, y, 210, BUTTON_HEIGHT).build());
            return;
        }

        addRenderableWidget(Button.builder(Component.translatable("advwp.button.save"), b -> {
            String name = colorCodes(nameField.getValue().trim());
            if (name.isEmpty()) name = "waypoint";
            String bg = getBackgroundValue();

            JsonObject entry = new JsonObject();
            entry.addProperty("id", originalId);
            entry.addProperty("icon", iconId());
            entry.addProperty("title", name);
            entry.addProperty("description", buildFinalDescription());
            entry.addProperty("frame", "task");
            entry.addProperty("parent", selectedParentId != null ? selectedParentId.toString() : "");
            if (bg != null) entry.addProperty("background", bg);

            WaypointStorage.saveOrUpdateWaypoint(entry);
            minecraft.gui.setScreen(null);
        }).bounds(cx - 105, y, 100, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("advwp.button.delete"), b -> {
            WaypointStorage.deleteWaypoint(originalId);
            minecraft.gui.setScreen(null);
        }).bounds(cx + 5, y, 100, BUTTON_HEIGHT).build());
    }
}