package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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

@Mixin(value = AdvancementsScreen.class, priority = 500)
public abstract class AdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {

    @Shadow
    private AdvancementTab selectedTab;
    @Final
    @Shadow
    private Screen lastScreen;

    @Unique
    private int lastWinX, lastWinY;
    @Unique
    private boolean selectMode;
    @Unique
    private Consumer<ResourceLocation> selectCallback;
    @Unique
    private Screen screenToOpen;
    @Unique
    private Screen parentScreen;
    @Unique
    protected Button modButton;
    @Unique
    private double pressMx, pressMy;
    @Unique
    private int pressBtn = -1;
    @Unique
    private static final double DRAG_THRESHOLD = 4.0;

    protected AdvancementsScreenMixin(Component t) {
        super(t);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        modButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            setFocused(null);
            minecraft.setScreen(new MainMenuScreen(this));
        }).bounds(0, 0, 20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("advwp.button.modbutton.tooltip")))
                .build());
        advWp_syncModButton();
    }

    @Unique
    private void advWp_syncModButton() {
        if (modButton == null) return;
        int btnW = modButton.getWidth(), btnH = modButton.getHeight();
        int panelLeft = (this.width - AdvancementsScreen.WINDOW_WIDTH) / 2;
        int panelTop  = (this.height - AdvancementsScreen.WINDOW_HEIGHT) / 2;
        int panelRight = panelLeft + AdvancementsScreen.WINDOW_WIDTH;
        int btnX = Math.max(2, Math.min(panelRight - 9 - 3 - btnW, this.width  - btnW - 2));
        int btnY = Math.max(2, Math.min(panelTop + 20, this.height - btnH - 2));
        modButton.setX(btnX);
        modButton.setY(btnY);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        if (modButton == null) return;
        advWp_syncModButton();
        modButton.render(g, mx, my, pt);
        int ix = modButton.getX() + (modButton.getWidth()  - 16) / 2;
        int iy = modButton.getY() + (modButton.getHeight() - 16) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED,
                ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/logo.png"),
                ix, iy, 0f, 0f, 16, 16, 16, 16);
    }

    @Inject(method = "renderWindow", at = @At("HEAD"))
    private void capturePos(GuiGraphics g, int x, int y, CallbackInfo ci) {
        lastWinX = x;
        lastWinY = y;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onPress(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        pressMx = mx;
        pressMy = my;
        pressBtn = btn;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        boolean result = super.mouseReleased(mx, my, btn);
        advWp_handleRelease(mx, my, btn, result);
        return result;
    }

    @Unique
    private void advWp_handleRelease(double mx, double my, int btn, boolean alreadyHandled) {
        int storedBtn = pressBtn;
        double dxPress = mx - pressMx;
        double dyPress = my - pressMy;
        pressBtn = -1;
        if (storedBtn != btn) return;
        if (dxPress*dxPress + dyPress*dyPress > DRAG_THRESHOLD * DRAG_THRESHOLD) return;
        if (selectedTab == null) return;
        if (alreadyHandled) return;

        AdvancementTabAccessor tab = (AdvancementTabAccessor) selectedTab;
        int contentLeft = lastWinX + 9;
        int contentTop = lastWinY + 18;
        int relX = (int) mx - contentLeft;
        int relY = (int) my - contentTop;
        int sx = Mth.floor(tab.getScrollX());
        int sy = Mth.floor(tab.getScrollY());

        for (Object w : tab.getWidgets().values()) {
            AdvancementWidgetAccessor wa = (AdvancementWidgetAccessor) w;
            if (wa.invokeIsMouseOver(sx, sy, relX, relY)) {
                AdvancementNode node = wa.getAdvancementNode();
                node.holder().value().display().ifPresent(d -> {
                    ResourceLocation id = node.holder().id();
                    if(btn==0) {
                        if (selectMode) {
                            selectCallback.accept(id);
                            Screen target = screenToOpen != null ? screenToOpen : (parentScreen != null ? parentScreen : lastScreen);
                            minecraft.setScreen(target);
                        } else {
                            Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                            Navigator nav = Navigator.getInstance();
                            if (!java.util.Objects.equals(nav.getCurrentId(), id) && targets != null) {
                                nav.setCurrentId(id);
                                nav.setTargets(Navigator.Dimension.OVERWORLD, targets.get(Navigator.Dimension.OVERWORLD));
                                nav.setTargets(Navigator.Dimension.NETHER, targets.get(Navigator.Dimension.NETHER));
                                nav.setTargets(Navigator.Dimension.END, targets.get(Navigator.Dimension.END));
                            } else {
                                nav.clearAll();
                                nav.setCurrentId(null);
                            }
                            if (targets != null) Minecraft.getInstance().setScreen(null);
                        }
                    }else if(btn==1) {
                        Player player = Minecraft.getInstance().player;
                        if (player == null) return;
                        Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
                        if (dim == null) return;
                        Map<Navigator.Dimension, List<BlockPos>> parsed = CoordParser.parseForNavigation(d.getDescription().getString());
                        if (parsed == null) return;
                        List<BlockPos> targets = parsed.get(dim);
                        if(targets == null) return;
                        BlockPos target = Navigator.nearestOf(targets, player.blockPosition());
                        if(target == null) return;
                        String command = "tp" + " " + target.getX() + " " + target.getY() + " " + target.getZ();
                        Minecraft.getInstance().player.connection.sendCommand(command);
                        Minecraft.getInstance().setScreen(null);
                    }
                });
                break;
            }
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
    public void advWaypoint_setParentScreen(Screen screen) {
        parentScreen = screen;
    }

    @Override
    public void onClose() {
        selectMode = false;
        Screen target = parentScreen != null ? parentScreen : lastScreen;
        minecraft.setScreen(target);
    }
}