package com.listraind.advancementwaypoints.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementTab;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.dialogs.MainMenuScreen;
import com.listraind.advancementwaypoints.gui.handler.AdvancementScreenHandler;
import com.listraind.advancementwaypoints.navigator.Navigator;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
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

@Mixin(
   targets = {"betteradvancements.common.gui.BetterAdvancementsScreen"}
)
public abstract class BetterAdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom, IBetterAdvancementsScreen {
   @Shadow(
      remap = false
   )
   private BetterAdvancementTab selectedTab;
   @Shadow(
      remap = false
   )
   protected static float zoom;
   @Shadow(
      remap = false
   )
   private int internalWidth;
   @Shadow(
      remap = false
   )
   private int internalHeight;
   @Shadow(
      remap = false
   )
   private Map<AdvancementHolder, BetterAdvancementTab> tabs;
   @Unique
   private final AdvancementScreenHandler advWp_delegate = new AdvancementScreenHandler();
   @Unique
   protected Button modButton;
   @Unique
   private AdvancementScreenHandler.ButtonState advWp_press;
   @Unique
   private Method cachedIsMouseOverMethod;

   protected BetterAdvancementsScreenMixin(Component component) {
      super(component);
      this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
   }

   @Inject(
      method = {"onAddAdvancementRoot"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAddRoot(AdvancementNode root, CallbackInfo ci) {
      if (WaypointStorage.isTabHidden(root.holder().id().toString())) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"init"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
      int panelLeft = (this.width - this.internalWidth) / 2 + 30;
      int panelTop = (this.height - this.internalHeight) / 2 + 40;
      int panelRight = panelLeft + this.internalWidth - 70;
      int btnW = 26;
      int btnH = 26;
      int gap = 5;
      int btnX = Math.max(2, Math.min(panelRight - btnW - gap, this.width - btnW - 2));
      int btnY = Math.max(2, Math.min(panelTop + 20, this.height - btnH - 2));
      this.modButton = (Button)this.addRenderableWidget(Button.builder(Component.empty(), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.minecraft.gui.setScreen(new MainMenuScreen(this));
      }).bounds(btnX, btnY, btnW, btnH).tooltip(Tooltip.create(Component.translatable("advwp.button.modbutton.tooltip"))).build());
      this.modButton.visible = false;
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void onRender(GuiGraphicsExtractor g, int mx, int my, float pt, CallbackInfo ci) {
      super.extractRenderState(g, mx, my, pt);
      if (this.modButton != null && this.modButton.visible) {
         int ix = this.modButton.getX() + (this.modButton.getWidth() - 20) / 2;
         int iy = this.modButton.getY() + (this.modButton.getHeight() - 20) / 2;
         g.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/logo.png"), ix, iy, 0.0F, 0.0F, 20, 20, 20, 20);
      }

      this.advWp_delegate.renderContextMenu(g, mx, my, pt);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPress(MouseButtonEvent event, boolean unknown, CallbackInfoReturnable<Boolean> cir) {
      if (this.advWp_delegate.handleContextMenuClick(event.x(), event.y(), event.button())) {
         cir.setReturnValue(true);
      } else {
         this.advWp_press = new AdvancementScreenHandler.ButtonState(event.x(), event.y(), event.button());
         if (event.button() == 1) {
            int left = 30 + (this.width - this.internalWidth) / 2;
            int top = 40 + (this.height - this.internalHeight) / 2;
            boolean tabClicked = false;
            if (event.y() >= (double)(top - 36) && event.y() <= (double)(top + 24) && event.x() >= (double)(left - 20) && event.x() <= (double)(left + this.internalWidth + 20)) {
               tabClicked = true;
            }

            if (tabClicked) {
               cir.setReturnValue(true);
            }
         }

      }
   }

   @Inject(
      method = {"mouseReleased"},
      at = {@At("HEAD")}
   )
   private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
      this.advWp_handleRelease(event.x(), event.y(), event.button());
   }

   @Unique
   private void advWp_handleRelease(double mx, double my, int btn) {
      if (!this.advWp_press.isClick(mx, my, btn)) {
         this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
      } else {
         this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
         boolean widgetClicked = false;
         if (this.selectedTab != null) {
            BetterAdvancementTabAccessor tab = (BetterAdvancementTabAccessor)this.selectedTab;
            int left = 30 + (this.width - this.internalWidth) / 2;
            int top = 40 + (this.height - this.internalHeight) / 2;
            boolean inGui = mx < (double)(left + this.internalWidth - 60 - 9) && mx > (double)(left + 9) && my < (double)(top + this.internalHeight - 40 + 1) && my > (double)(top + 18);
            if (inGui) {
               double relX = mx - (double)left - (double)9.0F;
               double relY = my - (double)top - (double)18.0F;
               boolean isPlaneAdv = FabricLoader.getInstance().isModLoaded("planeadvancements") && PlaneAdvancementsHelper.isPlaneTab(this.selectedTab);
               double scrollX = isPlaneAdv ? PlaneAdvancementsHelper.getPanX(this.selectedTab) : (double)tab.getScrollX();
               double scrollY = isPlaneAdv ? PlaneAdvancementsHelper.getPanY(this.selectedTab) : (double)tab.getScrollY();
               Collection<?> widgets = isPlaneAdv ? PlaneAdvancementsHelper.getWidgets(this.selectedTab) : tab.getWidgets().values();

               try {
                  for(Object w : widgets) {
                     boolean hovered;
                     if (isPlaneAdv) {
                        hovered = PlaneAdvancementsHelper.isWidgetHovered(w, scrollX, scrollY, mx, my, left + 9, top + 18);
                     } else {
                        if (this.cachedIsMouseOverMethod == null) {
                           this.cachedIsMouseOverMethod = w.getClass().getMethod("isMouseOver", Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE);
                        }

                        hovered = (Boolean)this.cachedIsMouseOverMethod.invoke(w, scrollX, scrollY, relX, relY, zoom);
                     }

                     if (hovered) {
                        AdvancementHolder holder = null;
                        if (isPlaneAdv) {
                           AdvancementNode node = PlaneAdvancementsHelper.getAdvancementNode(w);
                           if (node != null) {
                              holder = node.holder();
                           }
                        }

                        if (holder == null && w instanceof BetterAdvancementWidget) {
                           BetterAdvancementWidget bw = (BetterAdvancementWidget)w;

                           for(Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                              if (entry.getValue() == bw) {
                                 holder = (AdvancementHolder)entry.getKey();
                                 break;
                              }
                           }
                        }

                        AdvancementHolder targetHolder = holder;
                        if (targetHolder != null) {
                           targetHolder.value().display().ifPresent((d) -> {
                              Identifier id = targetHolder.id();
                              String idStr = id.toString();
                              if (btn == 0) {
                                 if (WaypointStorage.isBranchHidden(idStr)) {
                                    WaypointStorage.setBranchHidden(idStr, false);
                                 } else {
                                    Map<Navigator.Dimension, List<BlockPos>> targets = CoordParser.parseForNavigation(d.getDescription().getString());
                                    this.advWp_delegate.handleLeftClick(id, targets);
                                 }
                              } else if (btn == 1) {
                                 Map<Navigator.Dimension, List<BlockPos>> parsed = CoordParser.parseForNavigation(d.getDescription().getString());
                                 this.advWp_delegate.showContextMenu(mx, my, id, parsed);
                              }

                           });
                           widgetClicked = true;
                           break;
                        }
                     }
                  }
               } catch (Exception e) {
                  AdvancementWaypoints.LOGGER.error("Failed to invoke isMouseOver", e);
               }
            }
         }

         if (!widgetClicked && btn == 1) {
            int left = 30 + (this.width - this.internalWidth) / 2;
            int top = 40 + (this.height - this.internalHeight) / 2;
            boolean tabClicked = false;
            if (my >= (double)(top - 36) && my <= (double)(top + 24) && mx >= (double)(left - 20) && mx <= (double)(left + this.internalWidth + 20)) {
               tabClicked = true;
            }

            if (tabClicked) {
               this.advWp_delegate.showTabContextMenu(mx, my);
            }
         }

      }
   }

   public void advWaypoint_setSelectMode(Consumer<Identifier> cb) {
      this.advWp_delegate.setSelectMode(cb);
   }

   public void advWaypoint_setScreenToOpen(Screen s) {
      this.advWp_delegate.setScreenToOpen(s);
   }

   public void advWaypoint_setParentScreen(Screen screen) {
      this.advWp_delegate.setParentScreen(screen);
   }

   public boolean advWaypoint_isMouseOverContextMenu(double mx, double my) {
      return this.advWp_delegate.isMouseOverContextMenu(mx, my);
   }

   public void onClose() {
      this.advWp_delegate.resetSelectMode();
      this.minecraft.gui.setScreen(this.advWp_delegate.getParentScreen());
   }

   public void advWp_recalculateAll() {
      for(BetterAdvancementTab tab : this.tabs.values()) {
         if (tab instanceof IBetterAdvancementTab iTab) {
            iTab.advWp_recalculate();
         }
      }

   }
}
