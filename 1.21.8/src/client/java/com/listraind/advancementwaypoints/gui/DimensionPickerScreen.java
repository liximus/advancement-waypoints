package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DimensionPickerScreen extends Screen {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private static final int[] COLORS = {0x00AA00, 0xAA0000, 0xFF5555, 0xAA00AA};

    private final WaypointFormScreen parent;
    private int panelX, panelY, panelW, panelH;

    public DimensionPickerScreen(WaypointFormScreen parent) {
        super(Component.literal("Выберите измерение"));
        this.parent = parent;
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
            String label = CoordParser.DIM_LABELS[i].replaceAll("§[0-9a-f]", "");
            int color = COLORS[i];
            addRenderableWidget(Button.builder(
                    Component.literal(label).withStyle(s -> s.withColor(color)),
                    b -> { parent.addDimRow(dim); minecraft.setScreen(parent); }
            ).bounds(panelX + pad, panelY + pad + 20 + i * (bh + gap), bw, bh).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, panelX, panelY + 20, 0f, 0f, panelW, panelH, panelW, panelH);
        super.render(g, mx, my, d);
        g.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}