package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void onExtractContents(GuiGraphicsExtractor g, int x, int y, CallbackInfo ci) {
        AdvancementTabCapture.set(x, y);
    }
}
