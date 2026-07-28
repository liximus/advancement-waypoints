package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.config.ModConfig;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.listraind.advancementwaypoints.navigator.WaypointLocatorMode;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public class MixinLocatorBarItem {

    @Shadow @Final
    private Minecraft minecraft;

    @Unique
    private static final Identifier PORTAL_TEXTURE =
        Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/portal.png");

    @Unique
    private static final Identifier ARROW_UP_SPRITE =
        Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_up");

    @Unique
    private static final Identifier ARROW_DOWN_SPRITE =
        Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_down");

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void advwp_renderPortalIconAlwaysAtCenter(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnableNavigation() || config.getHudMode() != ModConfig.HudMode.LOCATOR) return;

        Navigator nav = Navigator.getInstance();
        if (!nav.hasAnyTarget()) return;

        if (this.minecraft.player == null) return;

        Navigator.Dimension dim = Navigator.Dimension.from(this.minecraft.player.level().dimension());
        if (dim == null) return;

        List<BlockPos> posList = nav.getTargetsForDimension(dim);
        if (posList == null || posList.isEmpty()) {
            LocatorBar self = (LocatorBar) (Object) this;
            int top = self.top(this.minecraft.getWindow());

            int portalSize = 9;
            int centerX = (graphics.guiWidth() - portalSize) / 2;
            int centerY = top - 2;

            float scale = (float) portalSize / 16.0f;

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) centerX, (float) centerY);
            graphics.pose().scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, PORTAL_TEXTURE, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.pose().popMatrix();
        }
    }

    @Inject(
        method = "lambda$extractRenderState$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
        ),
        cancellable = true
    )
    private void advwp_renderCustomItemOrPortalOnLocatorBar(
        Entity cameraEntity,
        Level level,
        PartialTickSupplier partialTicks,
        GuiGraphicsExtractor graphics,
        int top,
        TrackedWaypoint waypoint,
        CallbackInfo ci
    ) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnableNavigation() || config.getHudMode() != ModConfig.HudMode.LOCATOR) return;

        if (waypoint.id() == null) return;
        boolean isOurWaypoint = waypoint.id().map(
            uuid -> WaypointLocatorMode.isModWaypoint(uuid),
            name -> false
        );

        if (!isOurWaypoint) return;

        Navigator nav = Navigator.getInstance();
        Minecraft mc = Minecraft.getInstance();
        Navigator.Dimension dim = mc.player != null ? Navigator.Dimension.from(mc.player.level().dimension()) : null;
        List<BlockPos> posList = dim != null ? nav.getTargetsForDimension(dim) : null;

        if (posList == null || posList.isEmpty()) {
            ci.cancel();
            return;
        }

        double angle = waypoint.yawAngleToCamera(level, new TrackedWaypoint.Camera() {
            @Override public float yaw() { return cameraEntity.getYRot(); }
            @Override public net.minecraft.world.phys.Vec3 position() { return cameraEntity.getEyePosition(); }
        }, partialTicks);

        int screenMiddle = graphics.guiWidth() / 2;
        double normAngle = angle * 173.0 / 2.0 / 60.0;
        int dotOffset = net.minecraft.util.Mth.floor(normAngle);
        int dotX = screenMiddle + dotOffset;
        int dotY = top - 2;

        double dist = Math.sqrt(waypoint.distanceSquared(cameraEntity));
        double maxDist = 1500.0;
        double clampedDist = Math.min(dist, maxDist);
        double progress = Math.pow(clampedDist / maxDist, 0.5244);
        float scale = (float) (0.85f - progress * 0.50f);
        int itemSize = Math.round(16 * scale);
        int itemY = dotY - (itemSize - 9) / 2;

        if (config.isShowItemOnLocator()) {
            ItemStack icon = WaypointLocatorMode.getCurrentTargetIcon();
            if (icon != null && !icon.isEmpty()) {
                ci.cancel();

                int itemX = dotX - itemSize / 2;

                graphics.pose().pushMatrix();
                graphics.pose().translate((float) itemX, (float) itemY);
                graphics.pose().scale(scale, scale);
                graphics.item(icon, 0, 0);
                graphics.pose().popMatrix();

                TrackedWaypoint.Projector projector = (TrackedWaypoint.Projector) mc.gameRenderer;
                TrackedWaypoint.PitchDirection pitch = waypoint.pitchDirectionToCamera(level, projector, partialTicks);

                if (pitch != TrackedWaypoint.PitchDirection.NONE) {
                    Identifier arrowSprite = pitch == TrackedWaypoint.PitchDirection.DOWN ? ARROW_DOWN_SPRITE : ARROW_UP_SPRITE;
                    int arrowX = dotX - 3;

                    int arrowY;
                    if (pitch == TrackedWaypoint.PitchDirection.DOWN) {
                        arrowY = Math.max(top + 6, itemY + itemSize + 1);
                    } else {
                        arrowY = Math.min(top - 6, itemY - 5);
                    }

                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, arrowX, arrowY, 7, 5);
                }
            }
        }
    }
}
