package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.DarkModeChecker;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DimensionPickerScreen extends Screen {

    protected static Identifier BG = DarkModeChecker.isDarkModeEnabled() ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png") : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static final int[] COLORS = {0x00AA00, 0xAA0000, 0xFF5555, 0xAA00AA};

    private final WaypointFormScreen parent;
    private int panelX, panelY, panelW, panelH;

    public DimensionPickerScreen(WaypointFormScreen parent) {
        super(Component.translatable("advwp.picker.dimension.title"));
        this.parent = parent;
    }

    public static void setDarkMode(boolean darkMode) {
        BG = darkMode ? Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png") : Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    }

    @Override
    protected void init() {
        int bw = 160, bh = 20, gap = 4, pad = 5;
        panelW = bw + pad * 2;
        panelH = 4 * bh + 3 * gap + pad * 2;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        for (int i = 0; i < 4; i++) {
            int dim = i;
            int color = COLORS[i];
            addRenderableWidget(Button.builder(
                    Component.translatable(CoordParser.DIM_LABEL_KEYS[i]).withStyle(s -> s.withColor(color)),
                    b -> {
                        parent.addDimRow(dim);
                        minecraft.gui.setScreen(parent);
                    }
            ).bounds(panelX + pad, panelY + pad + 20 + i * (bh + gap), bw, bh).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, panelX, panelY + 20, 0f, 0f, panelW, panelH, panelW, panelH);
        super.extractRenderState(g, mx, my, d);
        g.centeredText(font, title, panelX + panelW / 2, panelY + 26, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}