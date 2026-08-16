package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementWidget;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(
        value = {BetterAdvancementTab.class},
        remap = false
)
public abstract class BetterAdvancementTabMixin implements IBetterAdvancementTab {

    @Shadow private AdvancementNode rootNode;
    @Shadow protected Map<AdvancementHolder, BetterAdvancementWidget> widgets;
    @Shadow protected int scrollX;
    @Shadow protected int scrollY;
    @Shadow private int minX;
    @Shadow private int maxX;
    @Shadow private int minY;
    @Shadow private int maxY;
    @Shadow private boolean centered;

    @Inject(
            method = "drawContents",
            at = @At("HEAD")
    )
    private void onDrawContents(GuiGraphicsExtractor guiGraphics, int left, int top, int width, int height, float zoom, CallbackInfo ci) {
        AdvancementTabCapture.set(left, top);
        if (!this.centered) {
            this.scrollX = (width - (this.maxX + this.minX)) / 2;
            this.scrollY = (height - (this.maxY + this.minY)) / 2;
            this.centered = true;
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "drawContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
            ),
            remap = true
    )
    private void advWp_redirectBetterTabBackgroundBlit(GuiGraphicsExtractor g, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, net.minecraft.resources.Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        com.listraind.advancementwaypoints.advancement.TextureHelper.blitBackground(g, pipeline, texture, x, y, width, height);
    }

    @Inject(
            method = "addWidget",
            at = @At("RETURN")
    )
    private void onAddWidget(BetterAdvancementWidget widget, AdvancementHolder holder, CallbackInfo ci) {
        this.centered = false;
    }

    @Inject(
            method = "loadScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onLoadScroll(int width, int height, CallbackInfo ci) {
        advWp_recalculate();
        Pair<Integer, Integer> scroll = BetterAdvancementTab.scrollHistory.get(this.rootNode.holder());
        if (scroll != null && (this.maxX - this.minX > width || this.maxY - this.minY > height)) {
            this.scrollX = scroll.getFirst();
            this.scrollY = scroll.getSecond();
            if (this.maxX - this.minX > width) {
                this.scrollX = Mth.clamp(this.scrollX, -(this.maxX - width), -this.minX);
            }
            if (this.maxY - this.minY > height) {
                this.scrollY = Mth.clamp(this.scrollY, -(this.maxY - height), -this.minY);
            }
            this.centered = true;
        } else {
            this.centered = false;
        }
        ci.cancel();
    }

    @Inject(
            method = "scroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onScroll(double deltaX, double deltaY, int width, int height, CallbackInfo ci) {
        if (this.maxX - this.minX > width) {
            this.scrollX = (int) Math.round(Mth.clamp((double) this.scrollX + deltaX, (double) (-(this.maxX - width)), (double) (-this.minX)));
        }
        if (this.maxY - this.minY > height) {
            this.scrollY = (int) Math.round(Mth.clamp((double) this.scrollY + deltaY, (double) (-(this.maxY - height)), (double) (-this.minY)));
        }
        ci.cancel();
    }

    @Inject(
            method = {"storeScroll"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void advWp_guardStoreScroll(CallbackInfo ci) {
        if (!this.centered) {
            ci.cancel();
        }
    }

    @Override
    public void advWp_recalculate() {
        if (this.widgets != null && !this.widgets.isEmpty()) {
            for (BetterAdvancementWidget w : this.widgets.values()) {
                if (w instanceof IBetterAdvancementWidget iw) {
                    iw.advWp_resetHierarchy();
                    iw.advWp_updatePosition();
                }
            }

            for (BetterAdvancementWidget w : this.widgets.values()) {
                if (w instanceof IBetterAdvancementWidget iw) {
                    iw.advWp_rebuildHierarchy();
                }
            }

            int newMinX = Integer.MAX_VALUE;
            int newMaxX = Integer.MIN_VALUE;
            int newMinY = Integer.MAX_VALUE;
            int newMaxY = Integer.MIN_VALUE;

            for (BetterAdvancementWidget w : this.widgets.values()) {
                if (w.getAdvancement() != null && WaypointStorage.isNodeHidden(w.getAdvancement())) {
                    continue;
                }
                int left = w.getX();
                int right = left + 28;
                int top = w.getY();
                int bottom = top + 27;
                newMinX = Math.min(newMinX, left);
                newMaxX = Math.max(newMaxX, right);
                newMinY = Math.min(newMinY, top);
                newMaxY = Math.max(newMaxY, bottom);
            }

            if (newMinX != Integer.MAX_VALUE) {
                this.minX = newMinX;
                this.maxX = newMaxX;
                this.minY = newMinY;
                this.maxY = newMaxY;
            } else {
                this.minX = 0;
                this.maxX = 28;
                this.minY = 0;
                this.maxY = 27;
            }
        }
    }
}
