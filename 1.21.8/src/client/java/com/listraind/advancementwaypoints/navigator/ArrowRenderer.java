package com.listraind.advancementwaypoints.navigator;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ArrowRenderer {

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/arrow.png");
    private static final ResourceLocation PORTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/portal.png");

    private static final int SIZE = 16;
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_BOTTOM = 10;
    private static ArrowRenderer INSTANCE;

    private ArrowRenderer() {}

    public static ArrowRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ArrowRenderer();
        }
        return INSTANCE;
    }

    void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.DEBUG,
                ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "nav_arr"), this::render);
    }

    private void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ArrowModule module = ArrowModule.getInstance();
        if (!module.hasAnyTarget()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ArrowModule.Dimension currentDim = ArrowModule.Dimension.from(player.level().dimension());
        if (currentDim == null) return;

        BlockPos target = module.getTarget(currentDim);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int bgPadding = 4;

        if (target != null) {
            renderArrow(graphics, mc, player, target);
        } else {
            renderPortal(graphics, mc, screenW, screenH, bgPadding);
        }
    }

    private void renderArrow(GuiGraphics graphics, Minecraft mc, Player player,
                             BlockPos target) {
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);
        String distText = String.format("%.0f m", distance);

        double angleToTarget = Math.toDegrees(Math.atan2(dz, -dx)) - 90.0;
        float rotationRadians = (float) -Math.toRadians(angleToTarget + player.getYRot());

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int textWidth = mc.font.width(distText);

        int elementWidth = Math.max(SIZE, textWidth);
        int centerX = screenW - MARGIN_RIGHT - elementWidth / 2;
        int textY = screenH - MARGIN_BOTTOM - mc.font.lineHeight;
        int centerY = textY - 6 - SIZE / 2;

        int bgPadding = 4;
        graphics.fill(
                centerX - SIZE / 2 - bgPadding,
                centerY - SIZE / 2 - bgPadding,
                centerX + SIZE / 2 + bgPadding,
                centerY + SIZE / 2 + bgPadding,
                0x80000000
        );

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) centerX, (float) centerY);
        graphics.pose().rotate(rotationRadians);
        graphics.pose().translate(-SIZE / 2.0f, -SIZE / 2.0f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, 0, 0, 0f, 0f, SIZE, SIZE, SIZE, SIZE);
        graphics.pose().popMatrix();

        graphics.drawString(mc.font, distText, centerX - textWidth / 2, textY, 0xFFFFFFFF, true);
    }

    private void renderPortal(GuiGraphics graphics, Minecraft mc, int screenW, int screenH, int bgPadding) {
        int centerX = screenW - MARGIN_RIGHT - SIZE / 2;
        int centerY = screenH - MARGIN_BOTTOM - SIZE / 2;

        graphics.fill(
                centerX - SIZE / 2 - bgPadding,
                centerY - SIZE / 2 - bgPadding,
                centerX + SIZE / 2 + bgPadding,
                centerY + SIZE / 2 + bgPadding,
                0x80000000
        );

        graphics.blit(RenderPipelines.GUI_TEXTURED, PORTAL_TEXTURE,
                centerX - SIZE / 2, centerY - SIZE / 2,
                0f, 0f, SIZE, SIZE, SIZE, SIZE);
    }
}