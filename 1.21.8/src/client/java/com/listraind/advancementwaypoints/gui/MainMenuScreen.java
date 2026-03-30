package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MainMenuScreen extends Screen {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static final int W = 200, H = 90;

    public MainMenuScreen() {
        super(Component.literal("Меню вейпоинтов"));
    }

    @Override
    protected void init() {
        int cx = (width - W) / 2;
        int cy = (height - H) / 2;
        int bw = W - 40;
        int bx = cx + 20;

        addRenderableWidget(Button.builder(Component.literal("Создать вейпоинт"), b ->
                minecraft.setScreen(new CreateWaypointScreen())
        ).bounds(bx, cy + 25, bw, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Редактировать вейпоинт"), b -> {
            AdvancementsScreen adv = new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this);
            ((IAdvancementScreenCustom) adv).advWaypoint_setSelectMode(id -> {
                var data = WaypointStorage.getWaypointOrVanilla(id);
                ((IAdvancementScreenCustom) adv).advWaypoint_setScreenToOpen(new EditWaypointScreen(data));
            });
            minecraft.setScreen(adv);
        }).bounds(bx, cy + 55, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        int cx = (width - W) / 2;
        int cy = (height - H) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, BG, cx, cy, 0, 0, W, H, W, H);
        super.render(g, mx, my, d);
        g.drawString(font, title, width / 2 - font.width(title) / 2, cy + 10, 0xFF222222, false);
    }
}