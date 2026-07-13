package com.listraind.advancementwaypoints.navigator;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class NavigatorHud {

    private static final Identifier ARROW = Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/arrow.png");
    private static final Identifier PORTAL = Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/portal.png");
    private static final int SIZE = 16, MARGIN = 10;

    private static NavigatorHud INSTANCE;

    private NavigatorHud() {
    }

    public static NavigatorHud getInstance() {
        if (INSTANCE == null) INSTANCE = new NavigatorHud();
        return INSTANCE;
    }

    void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "nav_hud"), this::extractRenderState);
    }

    private void extractRenderState(GuiGraphicsExtractor g, DeltaTracker dt) {
        Navigator nav = Navigator.getInstance();
        if (!nav.hasAnyTarget()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
        if (dim == null) return;

        BlockPos target = nav.getNearest(dim, player.blockPosition());
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (target != null) {
            renderArrow(g, mc, player, target, sw, sh);
        } else {
            renderPortal(g, sw, sh);
        }
    }

    private void renderArrow(GuiGraphicsExtractor g, Minecraft mc, Player player, BlockPos target, int sw, int sh) {
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        String text = String.format("%.0f m", dist);

        double angle = Math.toDegrees(Math.atan2(dz, -dx)) - 90.0;
        float rot = (float) -Math.toRadians(angle + player.getYRot());

        int tw = mc.font.width(text);
        int ew = Math.max(SIZE, tw);
        int cx = sw - MARGIN - ew / 2;
        int ty = sh - MARGIN - mc.font.lineHeight;
        int cy = ty - 6 - SIZE / 2;

        g.fill(cx - SIZE / 2 - 4, cy - SIZE / 2 - 4, cx + SIZE / 2 + 4, cy + SIZE / 2 + 4, 0x80000000);

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(rot);
        g.pose().translate(-SIZE / 2.0f, -SIZE / 2.0f);
        g.blit(RenderPipelines.GUI_TEXTURED, ARROW, 0, 0, 0f, 0f, SIZE, SIZE, SIZE, SIZE);
        g.pose().popMatrix();

        g.text(mc.font, text, cx - tw / 2, ty, 0xFFFFFFFF, true);
    }

    private void renderPortal(GuiGraphicsExtractor g, int sw, int sh) {
        int cx = sw - MARGIN - SIZE / 2;
        int cy = sh - MARGIN - SIZE / 2;
        g.fill(cx - SIZE / 2 - 4, cy - SIZE / 2 - 4, cx + SIZE / 2 + 4, cy + SIZE / 2 + 4, 0x80000000);
        g.blit(RenderPipelines.GUI_TEXTURED, PORTAL, cx - SIZE / 2, cy - SIZE / 2, 0f, 0f, SIZE, SIZE, SIZE, SIZE);
    }
}