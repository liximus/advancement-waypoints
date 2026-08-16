package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.compat.BetterAdvancementsHelper;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.handler.AdvancementScreenHandler;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

@Mixin(targets = "betteradvancements.common.gui.BetterAdvancementsScreen")
public abstract class BetterAdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom, IBetterAdvancementsScreen {

    @Shadow(remap = false) private BetterAdvancementTab selectedTab;
    @Shadow(remap = false) protected static float zoom;
    @Shadow(remap = false) private int internalWidth;
    @Shadow(remap = false) private int internalHeight;
    @Shadow(remap = false) private static int tabPage;
    @Shadow(remap = false) private Map<AdvancementHolder, BetterAdvancementTab> tabs;

    @Unique private final AdvancementScreenHandler advWp_delegate = new AdvancementScreenHandler();
    @Unique private AdvancementScreenHandler.ButtonState advWp_press = AdvancementScreenHandler.ButtonState.NONE;

    protected BetterAdvancementsScreenMixin(Component component) { super(component); }

    @Inject(method = "onAddAdvancementRoot", at = @At("HEAD"), cancellable = true)
    private void onAddRoot(net.minecraft.advancements.AdvancementNode root, CallbackInfo ci) {
        if (WaypointStorage.isTabHidden(root.holder().id().toString())) ci.cancel();
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        advWp_delegate.setAdvancementsLastScreen(advWp_delegate.getParentScreen());
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor g, int mx, int my, float pt, CallbackInfo ci) {
        advWp_delegate.renderContextMenu(g, mx, my, pt);
    }

    @Inject(method = "onAdvancementsCleared", at = @At("HEAD"))
    private void onAdvCleared(CallbackInfo ci) {
        BetterAdvancementTab.scrollHistory.clear();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onPress(MouseButtonEvent event, boolean unknown, CallbackInfoReturnable<Boolean> cir) {
        if (advWp_delegate.handleContextMenuClick(event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
            return;
        }
        advWp_press = new AdvancementScreenHandler.ButtonState(event.x(), event.y(), event.button());

        if (event.button() == 1 && BetterAdvancementsHelper.isTabHeaderClicked(tabs, this.width, this.height, internalWidth, internalHeight, tabPage, event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        advWp_handleRelease(event.x(), event.y(), event.button());
        return super.mouseReleased(event);
    }

    @Unique
    private void advWp_handleRelease(double mx, double my, int btn) {
        if (!advWp_press.isClick(mx, my, btn)) {
            advWp_press = AdvancementScreenHandler.ButtonState.NONE;
            return;
        }
        advWp_press = AdvancementScreenHandler.ButtonState.NONE;

        AdvancementHolder targetHolder = BetterAdvancementsHelper.findHoveredHolder(
                selectedTab, zoom, this.width, this.height, internalWidth, internalHeight, mx, my
        );

        if (targetHolder != null) {
            targetHolder.value().display().ifPresent(d -> {
                Identifier id = targetHolder.id();
                String idStr = id.toString();
                if (btn == 0) {
                    if (WaypointStorage.isBranchHidden(idStr)) {
                        WaypointStorage.setBranchHidden(idStr, false);
                    } else if (com.listraind.advancementwaypoints.config.ModConfig.getInstance().isEnableNavigation()) {
                        Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                        advWp_delegate.handleLeftClick(id, targets);
                    }
                } else if (btn == 1) {
                    Map<Navigator.Dimension, List<BlockPos>> parsed = CoordParser.parseForNavigation(d.getDescription().getString());
                    advWp_delegate.showContextMenu(mx, my, id, parsed);
                }
            });
        } else if (btn == 1) {
            BetterAdvancementTab clickedTab = BetterAdvancementsHelper.findClickedTab(tabs, this.width, this.height, internalWidth, internalHeight, tabPage, mx, my);
            if (clickedTab != null) {
                String rootId = (clickedTab.getRootNode() != null) ? clickedTab.getRootNode().holder().id().toString() : null;
                advWp_delegate.showTabContextMenu(mx, my, rootId);
            }
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        advWp_delegate.resetSelectMode();
        if (advWp_delegate.getParentScreen() != null) {
            minecraft.gui.setScreen(advWp_delegate.getParentScreen());
        }
    }

    @Override public void advWaypoint_setSelectMode(Consumer<Identifier> cb) { advWp_delegate.setSelectMode(cb); }
    @Override public void advWaypoint_setScreenToOpen(Screen s) { advWp_delegate.setScreenToOpen(s); }
    @Override public void advWaypoint_setParentScreen(Screen screen) { advWp_delegate.setParentScreen(screen); }
    @Override public boolean advWaypoint_isMouseOverContextMenu(double mx, double my) { return advWp_delegate.isMouseOverContextMenu(mx, my); }

    @Override
    public void advWp_recalculateAll() {
        for (BetterAdvancementTab tab : tabs.values()) {
            if (tab instanceof IBetterAdvancementTab iTab) {
                iTab.advWp_recalculate();
            }
        }
    }
}
