package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
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

    protected AdvancementsScreenMixin(Component t) {
        super(t);
    }

    @Inject(method = "renderWindow", at = @At("HEAD"))
    private void capturePos(GuiGraphics g, int x, int y, CallbackInfo ci) {
        lastWinX = x;
        lastWinY = y;
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void onClick(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab == null) return;

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
                                if (nav.getCurrentId() != id && targets != null) {
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
                        }else if(btn==2) {
                            Player player = Minecraft.getInstance().player;
                            if (player == null) return;
                            Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
                            if (dim == null) return;
                            List<BlockPos> targets = CoordParser.parseForNavigation(d.getDescription().getString()).get(dim);
                            if(targets == null) return;
                            BlockPos target = getNearest(targets,player.blockPosition());
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

    @Unique
    @Nullable
    public BlockPos getNearest(List<BlockPos> list, BlockPos from) {
        if (from == null) return null;
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        BlockPos nearest = null;
        long min = Long.MAX_VALUE;
        for (BlockPos p : list) {
            if (p == null) continue;
            long dx = (long) p.getX() - from.getX();
            long dz = (long) p.getZ() - from.getZ();
            long distSq = dx*dx + dz*dz;
            if (distSq < min) {
                min = distSq;
                nearest = p;
            } else if (distSq == min) {
                if (nearest == null) {
                    nearest = p;
                } else {
                    if (p.getX() < nearest.getX() ||
                            (p.getX() == nearest.getX() && (p.getZ() < nearest.getZ() ||
                                    (p.getZ() == nearest.getZ() && p.getY() < nearest.getY())))) {
                        nearest = p;
                    }
                }
            }
        }
        return nearest;
    }
}