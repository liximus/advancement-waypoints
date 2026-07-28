package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.config.ModConfig;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class MixinLocatorBarDistance {

    @Shadow
    private Pair<?, ContextualBar> contextualInfoBar;

    @Shadow
    private Minecraft minecraft;

    @Inject(
        method = "extractHotbarAndDecorations",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"
        ),
        cancellable = true
    )
    private void advwp_suppressXpLevelWhenLocatorActive(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnableNavigation()) return;
        if (this.contextualInfoBar != null && this.contextualInfoBar.getSecond() instanceof LocatorBar) {
            if (config.getHudMode() == ModConfig.HudMode.LOCATOR && config.isShowDistanceOnLocator()) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "extractHotbarAndDecorations",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void advwp_renderDistanceOnLocatorBar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnableNavigation()) return;
        if (this.contextualInfoBar != null && this.contextualInfoBar.getSecond() instanceof LocatorBar) {
            if (config.getHudMode() == ModConfig.HudMode.LOCATOR && config.isShowDistanceOnLocator()) {
                advwp_drawDistance(g, this.minecraft);
            }
        }
    }

    @Unique
    private static void advwp_drawDistance(GuiGraphicsExtractor g, Minecraft mc) {
        if (mc.player == null) return;

        Navigator nav = Navigator.getInstance();
        if (!nav.hasAnyTarget()) return;

        Player player = mc.player;
        Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
        if (dim == null) return;

        List<BlockPos> posList = nav.getTargetsForDimension(dim);
        if (posList == null || posList.size() != 1) return;

        BlockPos target = posList.get(0);
        if (target == null) return;

        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        String text = String.format("%.0f m", dist);

        Font font = mc.font;
        int textWidth = font.width(text);
        int x = (g.guiWidth() - textWidth) / 2;
        int y = g.guiHeight() - 24 - 9 - 2;

        g.text(font, text, x + 1, y, 0xFF000000, false);
        g.text(font, text, x - 1, y, 0xFF000000, false);
        g.text(font, text, x, y + 1, 0xFF000000, false);
        g.text(font, text, x, y - 1, 0xFF000000, false);
        g.text(font, text, x, y, 0xFF80FF20, false);
    }
}
