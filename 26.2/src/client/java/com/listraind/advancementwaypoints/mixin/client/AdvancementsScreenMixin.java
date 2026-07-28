package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.advancement.AdvancementTabCapture;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.compat.PlaneAdvancementsHelper;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.dialogs.MainMenuScreen;
import com.listraind.advancementwaypoints.gui.handler.AdvancementScreenHandler;
import com.listraind.advancementwaypoints.navigator.Navigator;
import java.util.ArrayList;
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
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {AdvancementsScreen.class},
   priority = 500
)
public abstract class AdvancementsScreenMixin extends Screen implements IAdvancementScreenCustom {
   @Shadow
   private AdvancementTab selectedTab;
   @Final
   @Shadow
   private Screen lastScreen;
   @Final
   @Shadow
   private Map<AdvancementHolder, AdvancementTab> tabs;
   @Unique
   private final AdvancementScreenHandler advWp_delegate = new AdvancementScreenHandler();
   @Unique
   protected Button modButton;
   @Unique
   private AdvancementScreenHandler.ButtonState advWp_press;
   @Unique
   private AdvancementNode advWp_pressedNode;

   protected AdvancementsScreenMixin(Component t) {
      super(t);
      this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
      this.advWp_pressedNode = null;
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
      this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
      this.advWp_delegate.setAdvancementsLastScreen(this.lastScreen);
      this.modButton = (Button)this.addRenderableWidget(Button.builder(Component.empty(), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.minecraft.gui.setScreen(new MainMenuScreen(this));
      }).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("advwp.button.modbutton.tooltip"))).build());
      this.modButton.visible = false;
      this.advWp_syncModButton();
   }

   @Unique
   private void advWp_syncModButton() {
      if (this.modButton != null) {
         int btnW = this.modButton.getWidth();
         int btnH = this.modButton.getHeight();
         int captX = AdvancementTabCapture.getX();
         int captY = AdvancementTabCapture.getY();
         int panelTop;
         int panelRight;
         if (captX == 0 && captY == 0) {
            int panelLeft = (this.width - 252) / 2;
            panelTop = (this.height - 140) / 2;
            panelRight = panelLeft + 252;
         } else {
            int panelLeft = captX - 9;
            panelTop = captY - 18;
            panelRight = this.width - panelLeft;
         }

         int btnX = Math.max(2, Math.min(panelRight - 9 - 3 - btnW, this.width - btnW - 2));
         int btnY = Math.max(2, Math.min(panelTop + 20, this.height - btnH - 2));
         this.modButton.setX(btnX);
         this.modButton.setY(btnY);
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void onRender(GuiGraphicsExtractor g, int mx, int my, float pt, CallbackInfo ci) {
      if (this.modButton != null && this.modButton.visible) {
         this.advWp_syncModButton();
         this.modButton.extractRenderState(g, mx, my, pt);
         int ix = this.modButton.getX() + (this.modButton.getWidth() - 16) / 2;
         int iy = this.modButton.getY() + (this.modButton.getHeight() - 16) / 2;
         g.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/logo.png"), ix, iy, 0.0F, 0.0F, 16, 16, 16, 16);
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
         this.advWp_pressedNode = this.advWp_findHoveredNode(event.x(), event.y());
         if (event.button() == 1) {
            int captX = AdvancementTabCapture.getX();
            int captY = AdvancementTabCapture.getY();
            int panelLeft = captX == 0 && captY == 0 ? (this.width - 252) / 2 : captX - 9;
            int panelTop = captX == 0 && captY == 0 ? (this.height - 140) / 2 : captY - 18;
            boolean tabClicked = false;
            if (this.tabs != null) {
               for(AdvancementTab tab : this.tabs.values()) {
                  if (tab.isMouseOver(panelLeft, panelTop, event.x(), event.y())) {
                     tabClicked = true;
                     break;
                  }
               }
            }

            if (!tabClicked && event.x() >= (double)panelLeft && event.x() <= (double)(panelLeft + 252) && event.y() >= (double)(panelTop - 32) && event.y() <= (double)(panelTop + 20)) {
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
   private AdvancementNode advWp_findHoveredNode(double mx, double my) {
      if (this.tabs != null && !this.tabs.isEmpty()) {
         int contentLeft = (this.width - 252) / 2 + 9;
         int contentTop = (this.height - 140) / 2 + 18;
         int captX = AdvancementTabCapture.getX();
         int captY = AdvancementTabCapture.getY();
         if (captX != 0 || captY != 0) {
            contentLeft = captX;
            contentTop = captY;
         }

         boolean isPlaneMod = FabricLoader.getInstance().isModLoaded("planeadvancements");
         if (isPlaneMod) {
            int defLeft = (this.width - 252) / 2 + 9;
            int defTop = (this.height - 140) / 2 + 18;
            contentLeft = PlaneAdvancementsHelper.getContentLeft(this, defLeft);
            contentTop = PlaneAdvancementsHelper.getContentTop(this, defTop);
         }

         List<AdvancementTab> tabList = new ArrayList();
         if (this.selectedTab != null) {
            tabList.add(this.selectedTab);
         }

         for(AdvancementTab t : this.tabs.values()) {
            if (t != null && !tabList.contains(t)) {
               tabList.add(t);
            }
         }

         for(AdvancementTab currentTab : tabList) {
            AdvancementTabAccessor tabAccessor = (AdvancementTabAccessor)currentTab;
            boolean isPlaneAdv = isPlaneMod && PlaneAdvancementsHelper.isPlaneTab(currentTab);
            double scrollX = isPlaneAdv ? PlaneAdvancementsHelper.getPanX(currentTab) : tabAccessor.getScrollX();
            double scrollY = isPlaneAdv ? PlaneAdvancementsHelper.getPanY(currentTab) : tabAccessor.getScrollY();
            Collection<?> widgets = isPlaneAdv ? PlaneAdvancementsHelper.getWidgets(currentTab) : tabAccessor.getWidgets().values();
            int relX = (int)mx - contentLeft;
            int relY = (int)my - contentTop;

            for(Object w : widgets) {
               boolean isHovered = isPlaneAdv ? PlaneAdvancementsHelper.isWidgetHovered(w, scrollX, scrollY, mx, my, contentLeft, contentTop) : ((AdvancementWidgetAccessor)w).invokeIsMouseOver(Mth.floor(scrollX), Mth.floor(scrollY), relX, relY);
               if (isHovered) {
                  AdvancementNode node = isPlaneAdv ? PlaneAdvancementsHelper.getAdvancementNode(w) : ((AdvancementWidgetAccessor)w).getAdvancementNode();
                  if (node != null && !WaypointStorage.isNodeHidden(node)) {
                     return node;
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Unique
   private void advWp_handleRelease(double mx, double my, int btn) {
      if (!this.advWp_press.isClick(mx, my, btn)) {
         this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
         this.advWp_pressedNode = null;
      } else {
         this.advWp_press = AdvancementScreenHandler.ButtonState.NONE;
         final AdvancementNode targetNode = this.advWp_pressedNode != null ? this.advWp_pressedNode : this.advWp_findHoveredNode(mx, my);
         this.advWp_pressedNode = null;
         if (targetNode != null) {
            targetNode.holder().value().display().ifPresent((d) -> {
               Identifier id = targetNode.holder().id();
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
         } else if (btn == 1) {
            int captX = AdvancementTabCapture.getX();
            int captY = AdvancementTabCapture.getY();
            int panelLeft = captX == 0 && captY == 0 ? (this.width - 252) / 2 : captX - 9;
            int panelTop = captX == 0 && captY == 0 ? (this.height - 140) / 2 : captY - 18;
            boolean tabClicked = false;
            if (this.tabs != null) {
               for(AdvancementTab tab : this.tabs.values()) {
                  if (tab.isMouseOver(panelLeft, panelTop, mx, my)) {
                     tabClicked = true;
                     break;
                  }
               }
            }

            if (!tabClicked && mx >= (double)panelLeft && mx <= (double)(panelLeft + 252) && my >= (double)(panelTop - 32) && my <= (double)(panelTop + 20)) {
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
      Screen target = this.advWp_delegate.getParentScreen() != null ? this.advWp_delegate.getParentScreen() : this.lastScreen;
      this.minecraft.gui.setScreen(target);
   }
}
