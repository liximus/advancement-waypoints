package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {

    @Shadow private AdvancementTab selectedTab;
    @Final @Shadow private Screen lastScreen;

    @Unique private int lastWinX, lastWinY;
    @Unique private boolean selectMode;
    @Unique private Consumer<ResourceLocation> selectCallback;
    @Unique private Screen screenToOpen;

    protected AdvancementsScreenMixin(Component t) { super(t); }

    @Inject(method = "renderWindow", at = @At("HEAD"))
    private void capturePos(GuiGraphics g, int x, int y, CallbackInfo ci) {
        lastWinX = x;
        lastWinY = y;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onClick(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        if (btn != 0 || selectedTab == null) return;

        AdvancementTabAccessor tab = (AdvancementTabAccessor) selectedTab;
        int relX = (int) mx - (lastWinX + 9);
        int relY = (int) my - (lastWinY + 18);
        int sx = Mth.floor(tab.getScrollX());
        int sy = Mth.floor(tab.getScrollY());

        for (AdvancementWidget w : tab.getWidgets().values()) {
            AdvancementWidgetAccessor wa = (AdvancementWidgetAccessor) w;
            if (!wa.invokeIsMouseOver(sx, sy, relX, relY)) continue;

            AdvancementNode node = wa.getAdvancementNode();
            node.holder().value().display().ifPresent(d -> {
                ResourceLocation id = node.holder().id();
                if (selectMode) {
                    selectCallback.accept(id);
                    minecraft.setScreen(screenToOpen != null ? screenToOpen : lastScreen);
                } else {
                    Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                    Navigator nav = Navigator.getInstance();
                    nav.setTargets(Navigator.Dimension.OVERWORLD, targets.get(Navigator.Dimension.OVERWORLD));
                    nav.setTargets(Navigator.Dimension.NETHER, targets.get(Navigator.Dimension.NETHER));
                    nav.setTargets(Navigator.Dimension.END, targets.get(Navigator.Dimension.END));
                    Minecraft.getInstance().setScreen(null);
                }
            });
            break;
        }
    }

    @Override
    public void advWaypoint_setSelectMode(Consumer<ResourceLocation> cb) {
        selectMode = true;
        selectCallback = cb;
    }

    @Override
    public void advWaypoint_setScreenToOpen(Screen s) {
        screenToOpen = s;
    }

    @Override
    public void onClose() {
        selectMode = false;
        minecraft.setScreen(lastScreen);
    }
}