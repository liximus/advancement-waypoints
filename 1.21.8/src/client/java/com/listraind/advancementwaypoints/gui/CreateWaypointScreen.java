package com.listraind.advancementwaypoints.gui;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CreateWaypointScreen extends WaypointFormScreen {

    private boolean initialized = false;

    public CreateWaypointScreen() {
        super(Component.literal("Создание вейпоинта"));
        selectedParentId = WaypointStorage.getLastParent();
    }

    @Override
    protected void init() {
        if (!initialized) {
            if (selectedParentId != null && coordRows.isEmpty()) {
                CoordRow cr = new CoordRow(currentDim());
                if (minecraft != null && minecraft.player != null) {
                    cr.sx = String.valueOf((int) minecraft.player.getX());
                    cr.sy = String.valueOf((int) minecraft.player.getY());
                    cr.sz = String.valueOf((int) minecraft.player.getZ());
                }
                coordRows.add(cr);
            }
            initialized = true;
        }
        super.init();
    }

    @Override
    protected void initActions(int cx, int y) {
        addRenderableWidget(Button.builder(Component.literal("§aСоздать"), b -> {
            String name = colorCodes(nameField.getValue().trim());
            if (name.isEmpty()) name = "waypoint";

            String id = "advwaypoints:wp_" + (System.currentTimeMillis() % 10000000);
            String desc = buildFinalDescription();
            String bg = getBackgroundValue();

            JsonObject entry = new JsonObject();
            entry.addProperty("id", id);
            entry.addProperty("icon", iconId());
            entry.addProperty("title", name);
            entry.addProperty("description", desc);
            entry.addProperty("frame", "task");
            entry.addProperty("parent", selectedParentId != null ? selectedParentId.toString() : "");
            if (bg != null) entry.addProperty("background", bg);

            WaypointStorage.saveOrUpdateWaypoint(entry);
            WaypointStorage.setLastParent(ResourceLocation.parse(id));
            minecraft.setScreen(null);
        }).bounds(cx - 50, y, 100, BH).build());
    }
}