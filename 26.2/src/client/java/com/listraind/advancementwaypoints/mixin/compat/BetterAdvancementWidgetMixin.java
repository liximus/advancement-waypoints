package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.WidgetFrameHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = {BetterAdvancementWidget.class}, remap = false)
public abstract class BetterAdvancementWidgetMixin implements IBetterAdvancementWidget {

    @Shadow private AdvancementNode advancementNode;
    @Shadow private DisplayInfo displayInfo;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected BetterDisplayInfo betterDisplayInfo;
    @Shadow private BetterAdvancementWidget parent;
    @Shadow private List<BetterAdvancementWidget> children;

    @Shadow public abstract void attachToParent();

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void suppressRenderIfHidden(GuiGraphicsExtractor guiGraphics, int scrollX, int scrollY, CallbackInfo ci) {
        if (WaypointStorage.isNodeHidden(this.advancementNode)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawConnectivity", at = @At("HEAD"), cancellable = true)
    private void suppressConnectivityIfHidden(GuiGraphicsExtractor guiGraphics, int scrollX, int scrollY, boolean drawInside, CallbackInfo ci) {
        if (WaypointStorage.isNodeHidden(this.advancementNode)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawHover", at = @At("HEAD"), cancellable = true)
    private void suppressHoverIfHiddenOrContextMenu(GuiGraphicsExtractor guiGraphics, int scrollX, int scrollY, float fade, int left, int top, float zoom, CallbackInfo ci) {
        if (WaypointStorage.isNodeHidden(this.advancementNode)) {
            ci.cancel();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (screen instanceof IAdvancementScreenCustom custom) {
            double realMx = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
            double realMy = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();
            if (custom.advWaypoint_isMouseOverContextMenu(realMx, realMy)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void suppressMouseOverIfHiddenOrContextMenu(double scrollX, double scrollY, double mouseX, double mouseY, float zoom, CallbackInfoReturnable<Boolean> cir) {
        if (WaypointStorage.isNodeHidden(this.advancementNode)) {
            cir.setReturnValue(false);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (screen instanceof IAdvancementScreenCustom custom) {
            double realMx = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
            double realMy = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();
            if (custom.advWaypoint_isMouseOverContextMenu(realMx, realMy)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Redirect(
            method = {"draw"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
            ),
            remap = true
    )
    private void redirectBlitSprite(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
        guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.displayInfo), x, y, width, height, color);
    }

    @Redirect(
            method = {"drawHover"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
            ),
            remap = true
    )
    private void redirectBlitSpriteHover(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
        guiGraphics.blitSprite(pipeline, WidgetFrameHelper.resolveSprite(sprite, this.advancementNode, this.displayInfo), x, y, width, height, color);
    }

    @Override
    public void advWp_updatePosition() {
        this.x = this.betterDisplayInfo.getPosX() != null ? this.betterDisplayInfo.getPosX() : Mth.floor(this.displayInfo.getX() * 32.0F);
        this.y = this.betterDisplayInfo.getPosY() != null ? this.betterDisplayInfo.getPosY() : Mth.floor(this.displayInfo.getY() * 27.0F);
    }

    @Override
    public void advWp_resetHierarchy() {
        this.parent = null;
        if (this.children != null) {
            this.children.clear();
        }
    }

    @Override
    public void advWp_rebuildHierarchy() {
        this.attachToParent();
    }
}
