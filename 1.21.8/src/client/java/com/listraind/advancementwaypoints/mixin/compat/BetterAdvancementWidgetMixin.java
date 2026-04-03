package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(value = BetterAdvancementWidget.class, remap = false)
public class BetterAdvancementWidgetMixin {

    @Shadow(remap = false)
    private AdvancementNode advancementNode;

    @Shadow(remap = false)
    private DisplayInfo displayInfo;

    @Unique
    private static final ResourceLocation TASK_SELECTED = ResourceLocation.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/task_frame_selected");
    @Unique
    private static final ResourceLocation GOAL_SELECTED = ResourceLocation.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/goal_frame_selected");
    @Unique
    private static final ResourceLocation CHALLENGE_SELECTED = ResourceLocation.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/challenge_frame_selected");

    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V"
            ),
            remap = true
    )
    private void redirectBlitSprite(GuiGraphics guiGraphics, RenderPipeline pipeline,
                                    ResourceLocation sprite, int x, int y, int width, int height, int color) {
        ResourceLocation currentId = Navigator.getInstance().getCurrentId();
        ResourceLocation finalSprite = sprite;

        if (currentId != null && currentId.equals(advancementNode.holder().id())) {
            switch (displayInfo.getType()) {
                case TASK -> finalSprite = TASK_SELECTED;
                case GOAL -> finalSprite = GOAL_SELECTED;
                case CHALLENGE -> finalSprite = CHALLENGE_SELECTED;
            }
        }

        guiGraphics.blitSprite(pipeline, finalSprite, x, y, width, height, color);
    }

    @Redirect(
            method = "drawHover",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V"
            ),
            remap = true
    )
    private void redirectBlitSpriteHover(GuiGraphics guiGraphics, RenderPipeline pipeline,
                                         ResourceLocation sprite, int x, int y, int width, int height, int color) {
        ResourceLocation currentId = Navigator.getInstance().getCurrentId();
        ResourceLocation finalSprite = sprite;

        if (currentId != null && currentId.equals(advancementNode.holder().id())) {
            switch (displayInfo.getType()) {
                case TASK -> finalSprite = TASK_SELECTED;
                case GOAL -> finalSprite = GOAL_SELECTED;
                case CHALLENGE -> finalSprite = CHALLENGE_SELECTED;
            }
        }

        guiGraphics.blitSprite(pipeline, finalSprite, x, y, width, height, color);
    }
}