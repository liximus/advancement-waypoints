package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.DarkModeChecker;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MainMenuScreen extends Screen {

    protected static Identifier BG = DarkModeChecker.isDarkModeEnabled()
            ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png")
            : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static final int W = 200, H = 120;

    private final Screen parent;

    public MainMenuScreen() {
        this(null);
    }

    public MainMenuScreen(Screen parent) {
        super(Component.translatable("advwp.menu.title"));
        this.parent = parent;
    }

    public static void setDarkMode(boolean darkMode) {
        BG = darkMode
                ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png")
                : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    }

    @Override
    protected void init() {
        int cx = (width - W) / 2;
        int cy = (height - H) / 2;
        int buttonWidth = W - 40;
        int buttonLeft = cx + 20;

        addRenderableWidget(Button.builder(Component.translatable("advwp.menu.create_waypoint"), b ->
                minecraft.gui.setScreen(new CreateWaypointScreen(false))
        ).bounds(buttonLeft, cy + 25, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("advwp.menu.create_tab"), b ->
                minecraft.gui.setScreen(new CreateWaypointScreen(true))
        ).bounds(buttonLeft, cy + 55, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("advwp.menu.edit"), b -> {
            if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) return;
            minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this));

            if (minecraft.gui.screen() instanceof IAdvancementScreenCustom customScreen) {
                customScreen.advWaypoint_setParentScreen(this);
                customScreen.advWaypoint_setSelectMode(id -> {
                    var data = WaypointStorage.getWaypointOrVanilla(id);
                    customScreen.advWaypoint_setScreenToOpen(new EditWaypointScreen(data));
                });
            }
        }).bounds(buttonLeft, cy + 85, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        int cx = (width - W) / 2;
        int cy = (height - H) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, BG, cx, cy, 0, 0, W, H, W, H);
        super.extractRenderState(g, mx, my, d);
        g.text(font, title, width / 2 - font.width(title) / 2, cy + 10, 0xFF222222, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }
}