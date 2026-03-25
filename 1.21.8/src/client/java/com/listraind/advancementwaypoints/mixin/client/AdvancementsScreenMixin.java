package com.listraind.advancementwaypoints.mixin.client;


import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancementMixinHelpers.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.navigator.ArrowModule;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.listraind.advancementwaypoints.config.AdvancementParser.parseAdvancement;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {

    @Shadow private AdvancementTab selectedTab;

    // Сохраняем реальные координаты окна из последнего рендера
    @Unique private int lastWindowX;
    @Unique private int lastWindowY;

    @Unique private boolean isSelectMode;

    @Final
    @Shadow private Screen lastScreen;

    @Unique private Consumer<ResourceLocation> idToSelect;



    protected AdvancementsScreenMixin(ClientAdvancements clientAdvancements, Component title) {
        super(title);
    }

    @Inject(method = "renderWindow", at = @At("HEAD"))
    private void captureWindowPosition(GuiGraphics guiGraphics, int x, int y,
                                       CallbackInfo ci) {
        this.lastWindowX = x;
        this.lastWindowY = y;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onAdvancementClicked(double mouseX, double mouseY, int button,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || this.selectedTab == null) return;

        AdvancementTabAccessor tab = (AdvancementTabAccessor) this.selectedTab;

        int contentLeft = this.lastWindowX + 9;
        int contentTop = this.lastWindowY + 18;

        int relMouseX = (int) mouseX - contentLeft;
        int relMouseY = (int) mouseY - contentTop;

        int scrollX = Mth.floor(tab.getScrollX());
        int scrollY = Mth.floor(tab.getScrollY());

        for (AdvancementWidget widget : tab.getWidgets().values()) {
            AdvancementWidgetAccessor wa = (AdvancementWidgetAccessor) widget;



            if (wa.invokeIsMouseOver(scrollX, scrollY, relMouseX, relMouseY)) {
                AdvancementNode node = wa.getAdvancementNode();

                node.holder().value().display().ifPresent(display -> {
                    Component description = display.getDescription();
                    Component title = display.getTitle();
                    ResourceLocation id = node.holder().id();
                    if(!isSelectMode) {
                        AdvancementWaypoints.LOGGER.info(description.getString());
                        startNavigator(description.getString());
                        Minecraft.getInstance().setScreen(null);
                    }else{
                        this.idToSelect.accept(id);
                        assert this.minecraft != null;
                        this.minecraft.setScreen(this.lastScreen);
                    }
                });

                break;
            }
        }
    }






    @Unique
    private static void startNavigator(String text) {
        Map<ArrowModule.Dimension, BlockPos> targets = parseAdvancement(text);

        ArrowModule arrow = ArrowModule.getInstance();
        arrow.setTarget(ArrowModule.Dimension.OVERWORLD, targets.get(ArrowModule.Dimension.OVERWORLD));
        arrow.setTarget(ArrowModule.Dimension.NETHER,    targets.get(ArrowModule.Dimension.NETHER));
        arrow.setTarget(ArrowModule.Dimension.END,        targets.get(ArrowModule.Dimension.END));
    }


    @Unique
    @Override
    public void advWaypoint_setSelectModeStringToWrite(Consumer<ResourceLocation> idToSelect) {
        isSelectMode = true;
        this.idToSelect = idToSelect;
    }


    @Override
    public void onClose() {
        isSelectMode = false;
        assert this.minecraft != null;
        this.minecraft.setScreen(this.lastScreen);
    }



}