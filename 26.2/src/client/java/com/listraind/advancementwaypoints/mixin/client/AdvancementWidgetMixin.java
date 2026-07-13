package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin {

    @Shadow
    private AdvancementNode advancementNode;

    @Shadow
    private DisplayInfo display;

    @Unique
    private static final Identifier TASK_SELECTED = Identifier.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/task_frame_selected");
    @Unique
    private static final Identifier GOAL_SELECTED = Identifier.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/goal_frame_selected");
    @Unique
    private static final Identifier CHALLENGE_SELECTED = Identifier.fromNamespaceAndPath(
            AdvancementWaypoints.MOD_ID, "advancements/challenge_frame_selected");

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void redirectBlitSpriteDraw(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline,
                                        Identifier sprite, int x, int y, int width, int height) {
        guiGraphics.blitSprite(pipeline, resolveSprite(sprite), x, y, width, height);
    }

    @Redirect(
            method = "extractHover",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 3
            )
    )
    private void redirectBlitSpriteHover(GuiGraphicsExtractor guiGraphics, RenderPipeline pipeline,
                                         Identifier sprite, int x, int y, int width, int height) {
        guiGraphics.blitSprite(pipeline, resolveSprite(sprite), x, y, width, height);
    }

    @Unique
    private Identifier resolveSprite(Identifier original) {
        Identifier currentId = Navigator.getInstance().getCurrentId();
        if (currentId == null || !currentId.equals(advancementNode.holder().id())) return original;
        return switch (display.getType()) {
            case TASK -> TASK_SELECTED;
            case GOAL -> GOAL_SELECTED;
            case CHALLENGE -> CHALLENGE_SELECTED;
        };
    }
}