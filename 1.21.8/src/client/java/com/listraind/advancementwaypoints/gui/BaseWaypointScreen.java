package com.listraind.advancementwaypoints.gui;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancementMixinHelpers.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BaseWaypointScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 120;

    public BaseWaypointScreen() {
        super(Component.literal("Меню вейпоинтов"));
    }

    @Override
    protected void init() {
        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;
        int btnW = PANEL_W - 40;
        int btnX = cx + 20;

        addRenderableWidget(Button.builder(Component.literal("Создать вейпоинт"), b ->
                minecraft.setScreen(new AddWaypointScreen())
        ).bounds(btnX, cy + 30, btnW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Редактировать вейпоинт"), b -> {
            AdvancementsScreen advScreen = new AdvancementsScreen(
                    minecraft.player.connection.getAdvancements(),
                    this
            );
            ((IAdvancementScreenCustom) advScreen).advWaypoint_setSelectModeStringToWrite(id -> {
                JsonObject target = WaypointManager.getWaypointData(id);
                ((IAdvancementScreenCustom) advScreen).advWaypoint_setScreenToOpen(new EditWaypointScreen(target));
            });
            minecraft.setScreen(advScreen);
        }).bounds(btnX, cy + 60, btnW, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, cx, cy, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, cy + 10, 0xFF222222, false);
    }
}