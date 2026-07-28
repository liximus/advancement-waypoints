package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.dialogs.MainMenuScreen;
import com.listraind.advancementwaypoints.gui.handler.AdvancementScreenHandler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = AdvancementsScreen.class, priority = 500)
public abstract class AdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {

    @Shadow private AdvancementTab selectedTab;
    @Final @Shadow private Screen lastScreen;
    @Final @Shadow private Map<AdvancementHolder, AdvancementTab> tabs;

    @Unique private final AdvancementScreenHandler advWp_delegate = new AdvancementScreenHandler();
    @Unique protected Button modButton;

    protected AdvancementsScreenMixin(Component t) { super(t); }

    @Inject(method = "onAddAdvancementRoot", at = @At("HEAD"), cancellable = true)
    private void onAddRoot(AdvancementNode root, CallbackInfo ci) {
        if (WaypointStorage.isTabHidden(root.holder().id().toString())) ci.cancel();
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        advWp_delegate.setAdvancementsLastScreen(lastScreen);
        modButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    setFocused(null);
                    minecraft.gui.setScreen(new MainMenuScreen(this));
                }).bounds(0, 0, 20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("advwp.button.modbutton.tooltip")))
                .build());
        modButton.visible = false;
        advWp_delegate.syncModButton(modButton, this.width, this.height);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor g, int mx, int my, float pt, CallbackInfo ci) {
        if (modButton != null && modButton.visible) {
            advWp_delegate.syncModButton(modButton, this.width, this.height);
            modButton.extractRenderState(g, mx, my, pt);
            int ix = modButton.getX() + (modButton.getWidth() - 16) / 2;
            int iy = modButton.getY() + (modButton.getHeight() - 16) / 2;
            g.blit(RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/logo.png"),
                    ix, iy, 0f, 0f, 16, 16, 16, 16);
        }
        advWp_delegate.renderContextMenu(g, mx, my, pt);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onPress(MouseButtonEvent event, boolean unknown, CallbackInfoReturnable<Boolean> cir) {
        if (advWp_delegate.handleMouseClick(this, event.x(), event.y(), event.button(), tabs, selectedTab)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        advWp_delegate.handleMouseRelease(this, event.x(), event.y(), event.button(), tabs, selectedTab);
    }

    @Override public void advWaypoint_setSelectMode(Consumer<Identifier> cb) { advWp_delegate.setSelectMode(cb); }
    @Override public void advWaypoint_setScreenToOpen(Screen s) { advWp_delegate.setScreenToOpen(s); }
    @Override public void advWaypoint_setParentScreen(Screen screen) { advWp_delegate.setParentScreen(screen); }
    @Override public boolean advWaypoint_isMouseOverContextMenu(double mx, double my) { return advWp_delegate.isMouseOverContextMenu(mx, my); }

    @Override
    public void onClose() {
        advWp_delegate.resetSelectMode();
        Screen target = advWp_delegate.getParentScreen() != null ? advWp_delegate.getParentScreen() : lastScreen;
        minecraft.gui.setScreen(target);
    }
}
