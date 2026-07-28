package com.listraind.advancementwaypoints.gui.handler;

import com.listraind.advancementwaypoints.gui.context.AdvancementContextMenu;
import com.listraind.advancementwaypoints.navigator.Navigator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class AdvancementScreenHandler {
   private boolean selectMode;
   private Screen advancementsLastScreen;
   private Consumer<Identifier> selectCallback;
   private Screen screenToOpen;
   private Screen parentScreen;
   private @Nullable AdvancementContextMenu contextMenu;

   public void setSelectMode(Consumer<Identifier> cb) {
      this.selectMode = true;
      this.selectCallback = cb;
   }

   public void setScreenToOpen(Screen s) {
      this.screenToOpen = s;
   }

   public void setParentScreen(Screen screen) {
      this.parentScreen = screen;
   }

   public boolean isSelectMode() {
      return this.selectMode;
   }

   public @Nullable Consumer<Identifier> getSelectCallback() {
      return this.selectCallback;
   }

   public @Nullable Screen getScreenToOpen() {
      return this.screenToOpen;
   }

   public @Nullable Screen getParentScreen() {
      return this.parentScreen;
   }

   public Screen resolveTargetScreen(@Nullable Screen lastScreen) {
      return this.screenToOpen != null ? this.screenToOpen : (this.parentScreen != null ? this.parentScreen : lastScreen);
   }

   public void handleLeftClick(Identifier id, @Nullable Map<Navigator.Dimension, List<BlockPos>> targets) {
      if (this.selectMode) {
         if (this.selectCallback != null) {
            this.selectCallback.accept(id);
         }

         Minecraft.getInstance().gui.setScreen(this.resolveTargetScreen((Screen)null));
      } else {
         Navigator nav = Navigator.getInstance();
         if (!Objects.equals(nav.getCurrentId(), id) && targets != null) {
            nav.setCurrentId(id);
            nav.clearAll();
            targets.forEach((dim, posList) -> {
               if (posList != null) {
                  nav.setTargets(dim, posList);
               }

            });
         } else {
            nav.clearAll();
            nav.setCurrentId((Identifier)null);
         }

         if (targets != null) {
            Minecraft.getInstance().gui.setScreen((Screen)null);
         }
      }

   }

   public void setAdvancementsLastScreen(Screen screen) {
      this.advancementsLastScreen = screen;
   }

   public Screen getAdvancementsLastScreen() {
      return this.advancementsLastScreen;
   }

   public void showContextMenu(double mx, double my, Identifier advancementId, Map<Navigator.Dimension, List<BlockPos>> targets) {
      if (this.contextMenu == null) {
         this.contextMenu = new AdvancementContextMenu();
      }

      this.contextMenu.show((int)mx, (int)my, advancementId, targets);
      this.contextMenu.setLastScreen(this.advancementsLastScreen);
   }

   public void showTabContextMenu(double mx, double my) {
      if (this.contextMenu == null) {
         this.contextMenu = new AdvancementContextMenu();
      }

      this.contextMenu.showTabMenu((int)mx, (int)my);
      this.contextMenu.setLastScreen(this.advancementsLastScreen);
   }

   public boolean handleContextMenuClick(double mx, double my, int btn) {
      return this.contextMenu != null && this.contextMenu.isVisible() && this.contextMenu.mouseClicked(mx, my, btn);
   }

   public boolean isMouseOverContextMenu(double mx, double my) {
      return this.contextMenu != null && this.contextMenu.isVisible() && this.contextMenu.isMouseOver(mx, my);
   }

   public @Nullable AdvancementContextMenu getContextMenu() {
      return this.contextMenu;
   }

   public void renderContextMenu(GuiGraphicsExtractor g, int mx, int my, float pt) {
      if (this.contextMenu != null && this.contextMenu.isVisible()) {
         this.contextMenu.render(g, mx, my, pt);
      }

   }

   public void resetSelectMode() {
      this.selectMode = false;
      this.selectCallback = null;
      this.screenToOpen = null;
   }

   public static record ButtonState(double pressMx, double pressMy, int pressBtn) {
      public static final ButtonState NONE = new ButtonState((double)0.0F, (double)0.0F, -1);
      private static final double DRAG_THRESHOLD = (double)10.0F;

      public boolean isClick(double mx, double my, int btn) {
         if (this.pressBtn != btn) {
            return false;
         } else {
            double dx = mx - this.pressMx;
            double dy = my - this.pressMy;
            return dx * dx + dy * dy <= (double)100.0F;
         }
      }
   }
}
