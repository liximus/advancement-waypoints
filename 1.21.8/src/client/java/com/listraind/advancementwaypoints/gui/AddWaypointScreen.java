package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.config.ConfigManager;
import com.listraind.advancementwaypoints.config.WaypointManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class AddWaypointScreen extends BaseWaypointFormScreen {

    private boolean hasInitializedDefaultCoords = false;

    public AddWaypointScreen() {
        super(Component.literal("Создание вейпоинта"));
        selectedParentId = ConfigManager.getLastParent();
    }

    @Override
    protected void init() {
        if (!hasInitializedDefaultCoords && activeCoords.isEmpty()) {
            CoordData cd = new CoordData(getCurrentDimIndex());
            if (minecraft != null && minecraft.player != null) {
                cd.sx = String.valueOf((int) minecraft.player.getX());
                cd.sy = String.valueOf((int) minecraft.player.getY());
                cd.sz = String.valueOf((int) minecraft.player.getZ());
            }
            activeCoords.add(cd);
            hasInitializedDefaultCoords = true;
        }
        super.init();
        if (selectedParentId != null && parentButton != null) {
            parentButton.setMessage(Component.literal("Parent: " + (selectedParent.isEmpty() ? "нет" : selectedParent)));
        }
    }

    @Override
    protected void initActionButtons(int centerX, int buttonsY) {
        addRenderableWidget(Button.builder(Component.literal("§aСоздать"), button -> {
            String name = translateColorCodes(nameField.getValue().trim());
            WaypointManager.generateAndSave(
                    name, getStandardCoordFields(), getIconId(),
                    selectedParentId, descriptionField.getValue().trim()
            );
            assert minecraft != null;
            minecraft.setScreen(null);
        }).bounds(centerX - 50, buttonsY, 100, BUTTON_HEIGHT).build());
    }
}