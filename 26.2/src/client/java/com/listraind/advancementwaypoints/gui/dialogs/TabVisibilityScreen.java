package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class TabVisibilityScreen extends BaseModScreen {
   private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
   private static final int ROW_HEIGHT = 24;
   private static final int VISIBLE_ROWS = 6;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HANDLE_HEIGHT = 15;
   private final Screen parentScreen;
   private final List<TabEntry> tabEntries = new ArrayList();
   private final List<Button> rowButtons = new ArrayList();
   private int scrollRow = 0;
   private float scrollProgress = 0.0F;
   private boolean dragging = false;

   public TabVisibilityScreen(Screen parentScreen) {
      super(Component.translatable("advwp.screen.tab_visibility"), 280, 210);
      this.parentScreen = parentScreen;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         for(AdvancementNode rootNode : mc.player.connection.getAdvancements().getTree().roots()) {
            if (rootNode.holder().value().display().isPresent()) {
               DisplayInfo display = (DisplayInfo)rootNode.holder().value().display().get();
               String rootId = rootNode.holder().id().toString();
               Component title = display.getTitle();
               ItemStack icon = display.getIcon().create();
               boolean visible = !WaypointStorage.isTabHidden(rootId);
               this.tabEntries.add(new TabEntry(rootId, title, icon, visible));
            }
         }
      }

   }

   private int listLeft() {
      return this.panelX + 15;
   }

   private int listTop() {
      return this.panelY + 28;
   }

   private int listWidth() {
      return this.panelWidth - 30;
   }

   private int listHeight() {
      return 144;
   }

   private int scrollbarX() {
      return this.listLeft() + this.listWidth() - 12;
   }

   private int maxRow() {
      return Math.max(0, this.tabEntries.size() - 6);
   }

   protected void initContent() {
      this.rowButtons.clear();
      int btnWidth = 70;

      for(int r = 0; r < 6; ++r) {
         int rowIdx = r;
         int btnX = this.scrollbarX() - btnWidth - 5;
         int btnY = this.listTop() + rowIdx * 24 + 3;
         Button btn = (Button)this.addRenderableWidget(Button.builder(Component.empty(), (b) -> {
            int actualIdx = this.scrollRow + rowIdx;
            if (actualIdx >= 0 && actualIdx < this.tabEntries.size()) {
               TabEntry entry = (TabEntry)this.tabEntries.get(actualIdx);
               entry.visible = !entry.visible;
               WaypointStorage.setTabHidden(entry.rootId, !entry.visible);
               this.updateRowButtons();
            }

         }).bounds(btnX, btnY, btnWidth, 18).build());
         this.rowButtons.add(btn);
      }

      this.updateRowButtons();
      this.addCenteredButton(Component.translatable("advwp.dialog.done"), this.panelHeight - 26, this.panelWidth - 40, 20, (b) -> this.onClose());
   }

   private void updateRowButtons() {
      for(int r = 0; r < 6; ++r) {
         Button btn = (Button)this.rowButtons.get(r);
         int idx = this.scrollRow + r;
         if (idx < this.tabEntries.size()) {
            TabEntry entry = (TabEntry)this.tabEntries.get(idx);
            btn.visible = true;
            btn.active = true;
            btn.setMessage(Component.translatable(entry.visible ? "advwp.visibility.shown" : "advwp.visibility.hidden"));
         } else {
            btn.visible = false;
            btn.active = false;
         }
      }

   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.maxRow() > 0) {
         this.scrollProgress = Mth.clamp(this.scrollProgress - (float)(verticalAmount / (double)this.maxRow()), 0.0F, 1.0F);
         this.scrollRow = Math.round(this.scrollProgress * (float)this.maxRow());
         this.updateRowButtons();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean unknown) {
      double mx = event.x();
      double my = event.y();
      if (this.maxRow() > 0 && mx >= (double)this.scrollbarX() && mx < (double)(this.scrollbarX() + 12) && my >= (double)this.listTop() && my < (double)(this.listTop() + this.listHeight())) {
         this.dragging = true;
         this.updateScrollFromMouse(my);
         return true;
      } else {
         return super.mouseClicked(event, unknown);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.dragging) {
         this.dragging = false;
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (this.dragging && this.maxRow() > 0) {
         this.updateScrollFromMouse(event.y());
         return true;
      } else {
         return super.mouseDragged(event, deltaX, deltaY);
      }
   }

   private void updateScrollFromMouse(double my) {
      float relativeY = (float)(my - (double)this.listTop() - (double)7.5F);
      float trackHeight = (float)(this.listHeight() - 15);
      this.scrollProgress = Mth.clamp(relativeY / trackHeight, 0.0F, 1.0F);
      this.scrollRow = Math.round(this.scrollProgress * (float)this.maxRow());
      this.updateRowButtons();
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      super.extractRenderState(g, mx, my, d);

      for(int r = 0; r < 6; ++r) {
         int idx = this.scrollRow + r;
         if (idx >= this.tabEntries.size()) {
            break;
         }

         TabEntry entry = (TabEntry)this.tabEntries.get(idx);
         int textX = this.listLeft() + 24;
         int textY = this.listTop() + r * 24 + 4;
         int iconX = this.listLeft() + 2;
         int iconY = this.listTop() + r * 24 + 4;
         g.item(entry.icon, iconX, iconY);
         int maxTextWidth = this.scrollbarX() - textX - 75;
         Component title = entry.title;
         if (this.font.width(title) > maxTextWidth) {
            Font var10000 = this.font;
            String str = var10000.plainSubstrByWidth(title.getString(), maxTextWidth - 8) + "...";
            title = Component.literal(str);
         }

         g.text(this.font, title, textX, textY + 2, -13421773, false);
      }

      if (this.maxRow() > 0) {
         int scrollHandleY = this.listTop() + (int)((float)(this.listHeight() - 15) * this.scrollProgress);
         g.fill(this.scrollbarX(), this.listTop(), this.scrollbarX() + 12, this.listTop() + this.listHeight(), 1073741824);
         g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, this.scrollbarX(), scrollHandleY, 12, 15);
      }

   }

   public void onClose() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.gui.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements(), this.parentScreen));
      } else {
         mc.gui.setScreen(this.parentScreen);
      }

   }

   private static class TabEntry {
      String rootId;
      Component title;
      ItemStack icon;
      boolean visible;

      TabEntry(String rootId, Component title, ItemStack icon, boolean visible) {
         this.rootId = rootId;
         this.title = title;
         this.icon = icon;
         this.visible = visible;
      }
   }
}
