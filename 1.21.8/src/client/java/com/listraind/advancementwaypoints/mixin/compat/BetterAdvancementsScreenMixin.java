package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import betteradvancements.common.gui.BetterAdvancementTab;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(targets = "betteradvancements.common.gui.BetterAdvancementsScreen")
public abstract class BetterAdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom, IBetterAdvancementsScreen {

    @Shadow(remap = false)
    private BetterAdvancementTab selectedTab;

    @Shadow(remap = false)
    protected static float zoom;

    @Shadow(remap = false)
    private int internalWidth;

    @Shadow(remap = false)
    private int internalHeight;

    @Unique
    private Screen parentScreen;
    @Unique
    private boolean selectMode;
    @Unique
    private Consumer<ResourceLocation> selectCallback;
    @Unique
    private Screen screenToOpen;
    @Unique
    private java.lang.reflect.Method cachedIsMouseOverMethod;

    protected BetterAdvancementsScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        System.out.println("BetterAdvancementsScreenMixin applied");
    }


    @Inject(method = "mouseClicked(DDI)Z", at = @At("RETURN"))
    private void onClick(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab == null) return;

        System.out.println("mouseClicked in BetterAdvancementsScreen");

        BetterAdvancementTabAccessor tab = (BetterAdvancementTabAccessor) selectedTab;

        int left = 30 + (this.width - this.internalWidth) / 2;
        int top = 40 + (this.height - this.internalHeight) / 2;

        boolean inGui = mx < (double)(left + this.internalWidth - 60 - 9)
                && mx > (double)(left + 9)
                && my < (double)(top + this.internalHeight - 40 + 1)
                && my > (double)(top + 18);

        if (!inGui) return;

        double relX = mx - (double)left - 9.0;
        double relY = my - (double)top - 18.0;

        int sx = tab.getScrollX();
        int sy = tab.getScrollY();

        try {
            for (Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                Object w = entry.getValue();

                if (cachedIsMouseOverMethod == null) {
                    cachedIsMouseOverMethod = w.getClass().getMethod(
                            "isMouseOver",
                            double.class, double.class, double.class, double.class, float.class
                    );
                }

                boolean hovered = (boolean) cachedIsMouseOverMethod.invoke(w,
                        (double) sx, (double) sy, relX, relY, zoom
                );

                if (hovered) {
                    AdvancementHolder holder = entry.getKey();
                    holder.value().display().ifPresent(d -> {
                        ResourceLocation id = holder.id();
                        if(btn==0) {
                            if (selectMode) {
                                selectCallback.accept(id);
                                Screen target = screenToOpen != null ? screenToOpen : (parentScreen != null ? parentScreen : parentScreen);
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
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("Failed to invoke isMouseOver", e);
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
        minecraft.setScreen(parentScreen);
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

    @Shadow private Map<AdvancementHolder, BetterAdvancementTab> tabs;

    @Override
    public void advWp_recalculateAll() {
        for (BetterAdvancementTab tab : tabs.values()) {
            if (tab instanceof IBetterAdvancementTab iTab) {
                iTab.advWp_recalculate();
            }
        }
    }
}