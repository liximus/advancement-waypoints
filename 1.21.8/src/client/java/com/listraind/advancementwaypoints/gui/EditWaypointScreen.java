package com.listraind.advancementwaypoints.gui;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.config.WaypointManager;
import com.listraind.advancementwaypoints.config.WaypointManager.CoordsPerDimension;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

public class EditWaypointScreen extends BaseWaypointFormScreen {

    private final String originalId;
    private List<WaypointManager.CoordsPerDimension> allOriginalCoords;

    public EditWaypointScreen(JsonObject waypointJson) {
        super(Component.literal("Редактирование вейпоинта"));
        this.originalId = waypointJson.has("id") ? waypointJson.get("id").getAsString() : "";
        this.isVanilla = !this.originalId.contains("advwaypoints");
        loadFromJson(waypointJson);
    }

    private void loadFromJson(JsonObject o) {
        savedName = o.has("title") ? o.get("title").getAsString() : "";
        savedName = savedName.replace('§', '&');
        selectedIcon = WaypointManager.getIconFromJson(o);
        selectedParentId = WaypointManager.getParentFromJson(o);

        String desc = o.has("description") ? o.get("description").getAsString() : "";
        parseDescription(desc);
    }

    private void parseDescription(String desc) {
        allOriginalCoords = WaypointManager.parseAllCoordsFromDescription(desc);

        String[][] parsed = WaypointManager.parseCoordsFromDescription(desc);
        activeCoords.clear();
        for (int i = 0; i < 4; i++) {
            if (parsed[i] != null && parsed[i].length >= 3 && (!parsed[i][0].isEmpty() || !parsed[i][1].isEmpty() || !parsed[i][2].isEmpty())) {
                activeCoords.add(new CoordData(i, parsed[i][0], parsed[i][1], parsed[i][2]));
            }
        }
        
        for (CoordsPerDimension cpd : allOriginalCoords) {
            for (int j = 1; j < cpd.coords.size(); j++) {
                String[] c = cpd.coords.get(j);
                activeCoords.add(new CoordData(cpd.dimIndex, c[0], c[1], c[2]));
            }
        }
        
        savedDescription = WaypointManager.parseExtraFromDescription(desc);
    }

    @Override
    protected void initActionButtons(int centerX, int buttonsY) {
        addRenderableWidget(Button.builder(Component.literal("§aСохранить"), button -> {
            saveWaypoint();
            minecraft.setScreen(null);
        }).bounds(centerX - 105, buttonsY, 100, BUTTON_HEIGHT).build());

        Button delBtn = addRenderableWidget(Button.builder(Component.literal("§cУдалить"), button -> {
            deleteWaypoint();
            minecraft.setScreen(null);
        }).bounds(centerX + 5, buttonsY, 100, BUTTON_HEIGHT).build());
        delBtn.active = !isVanilla;
    }

    private void saveWaypoint() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) name = "waypoint";
        name = translateColorCodes(name);

        String description = WaypointManager.buildDescription(getStandardCoordFields(), savedDescription, allOriginalCoords);
        String iconId = getIconId();

        WaypointManager.saveWaypoint(originalId, name, description, iconId, selectedParentId);
    }

    private void deleteWaypoint() {
        WaypointManager.deleteWaypoint(originalId);
    }
}