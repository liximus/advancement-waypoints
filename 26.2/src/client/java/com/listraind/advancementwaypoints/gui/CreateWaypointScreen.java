package com.listraind.advancementwaypoints.gui;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public class CreateWaypointScreen extends WaypointFormScreen {

    
    private final boolean tab;
    private boolean initialized = false;

    public CreateWaypointScreen(boolean tab) {
        super(Component.translatable(tab ? "advwp.create.tab.title" : "advwp.create.title"));
        this.tab = tab;
        
        selectedParentId = tab ? null : WaypointStorage.getLastParent();
    }

    @Override
    protected boolean isTabMode() {
        return tab;
    }

    @Override
    protected boolean showResetParentButton() {
        
        return false;
    }

    @Override
    protected boolean hideFieldsUntilParentSelected() {
        
        
        return !tab;
    }

    @Override
    protected void saveState() {
        if (nameField != null) savedName = nameField.getValue();
        if (descField != null) savedDesc = descField.getValue();
        if (hadParentBefore) {
            for (CoordRow cr : coordRows) {
                if (cr.bx != null) cr.sx = cr.bx.getValue();
                if (cr.by != null) cr.sy = cr.by.getValue();
                if (cr.bz != null) cr.sz = cr.bz.getValue();
            }
        }
    }

    @Override
    protected void init() {
        if (!initialized) {
            if (!tab && selectedParentId != null && coordRows.isEmpty()) {
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
    public void onParentSelected(Identifier newParent) {
        saveState();
        selectedParentId = newParent;
        hadParentBefore = true;
        minecraft.gui.setScreen(new CreateWaypointScreen(tab));
        if (coordRows.isEmpty()) {
            CoordRow cr = new CoordRow(currentDim());
            if (minecraft != null && minecraft.player != null) {
                cr.sx = String.valueOf((int) minecraft.player.getX());
                cr.sy = String.valueOf((int) minecraft.player.getY());
                cr.sz = String.valueOf((int) minecraft.player.getZ());
            }
            coordRows.add(cr);
        }
    }

    @Override
    protected void initActions(int cx, int y) {
        
        boolean canCreate = tab || selectedParentId != null;

        Button createButton = Button.builder(Component.translatable("advwp.button.create_save"), b -> {
            String name = colorCodes(nameField.getValue().trim());
            if (name.isEmpty()) name = tab ? "tab" : "waypoint";

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
            entry.addProperty("background", Objects.requireNonNullElse(bg, "stone"));

            WaypointStorage.saveOrUpdateWaypoint(entry);
            WaypointStorage.setLastParent(Identifier.parse(id));
            minecraft.gui.setScreen(null);
        }).bounds(cx - 50, y, 100, BUTTON_HEIGHT).build();
        createButton.active = canCreate;
        addRenderableWidget(createButton);
    }
}
