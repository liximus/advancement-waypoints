package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import betteradvancements.common.gui.BetterAdvancementTab;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(targets = "betteradvancements.common.gui.BetterAdvancementsScreen", remap = false)
public abstract class BetterAdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {

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

    @Inject(method = "mouseClicked", at = @At("RETURN"), remap = false)
    private void onClick(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        if (btn != 0 || selectedTab == null) return;

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
                        if (selectMode) {
                            selectCallback.accept(id);
                            minecraft.setScreen(screenToOpen != null ? screenToOpen : parentScreen);
                        } else {
                            Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                            Navigator nav = Navigator.getInstance();
                            if (nav.getCurrentId() != id && targets!=null) {
                                nav.setCurrentId(id);
                                nav.setTargets(Navigator.Dimension.OVERWORLD, targets.get(Navigator.Dimension.OVERWORLD));
                                nav.setTargets(Navigator.Dimension.NETHER, targets.get(Navigator.Dimension.NETHER));
                                nav.setTargets(Navigator.Dimension.END, targets.get(Navigator.Dimension.END));
                            } else {
                                nav.clearAll();
                                nav.setCurrentId(null);
                            }
                            if(targets!=null) Minecraft.getInstance().setScreen(null);
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
}