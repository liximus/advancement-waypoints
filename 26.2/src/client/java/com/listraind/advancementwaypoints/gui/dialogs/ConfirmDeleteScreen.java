package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteScreen extends BaseModScreen {
   private final Screen parentScreen;
   private final Component message;
   private final Component submessage;
   private final Runnable onConfirm;
   private final Runnable onWithChildren;
   private final boolean hasChildrenMode;

   public ConfirmDeleteScreen(Screen parentScreen, Runnable onConfirm) {
      this(parentScreen, Component.translatable("advwp.dialog.delete.title"), Component.translatable("advwp.dialog.delete.message"), onConfirm);
   }

   public ConfirmDeleteScreen(Screen parentScreen, Component title, Component message, Runnable onConfirm) {
      super(title, 240, 90);
      this.parentScreen = parentScreen;
      this.message = message;
      this.submessage = null;
      this.onConfirm = onConfirm;
      this.onWithChildren = null;
      this.hasChildrenMode = false;
   }

   public ConfirmDeleteScreen(Screen parentScreen, Runnable onOnlyCurrent, Runnable onWithChildren) {
      this(parentScreen, Component.translatable("advwp.dialog.delete.title"), Component.translatable("advwp.dialog.delete.message"), onOnlyCurrent, onWithChildren);
   }

   public ConfirmDeleteScreen(Screen parentScreen, Component title, Component message, Runnable onOnlyCurrent, Runnable onWithChildren) {
      super(title, 240, 140);
      this.parentScreen = parentScreen;
      this.message = message;
      this.submessage = Component.translatable("advwp.dialog.delete.has_children_submessage");
      this.onConfirm = onOnlyCurrent;
      this.onWithChildren = onWithChildren;
      this.hasChildrenMode = true;
   }

   protected void initContent() {
      if (this.hasChildrenMode) {
         int bw = this.panelWidth - 40;
         this.addCenteredButton(Component.translatable("advwp.dialog.delete.only_current"), 50, bw, 20, (b) -> this.onConfirm.run());
         this.addCenteredButton(Component.translatable("advwp.dialog.delete.with_children"), 78, bw, 20, (b) -> {
            if (this.onWithChildren != null) {
               this.onWithChildren.run();
            }
         });
         this.addCenteredButton(Component.translatable("advwp.dialog.cancel"), 106, bw, 20, (b) -> this.minecraft.gui.setScreen(this.parentScreen));
      } else {
         int buttonWidth = (this.panelWidth - 50) / 2;
         int y = 52;
         this.addRenderableWidget(Button.builder(Component.translatable("advwp.dialog.delete.confirm"), (b) -> this.onConfirm.run()).bounds(this.panelX + 20, this.panelY + y, buttonWidth, 20).build());
         this.addRenderableWidget(Button.builder(Component.translatable("advwp.dialog.cancel"), (b) -> this.minecraft.gui.setScreen(this.parentScreen)).bounds(this.panelX + 20 + buttonWidth + 10, this.panelY + y, buttonWidth, 20).build());
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      super.extractRenderState(g, mx, my, d);
      if (this.hasChildrenMode) {
         String msg1 = this.message.getString();
         String msg2 = this.submessage != null ? this.submessage.getString() : "";
         int tx1 = this.panelX + (this.panelWidth - this.font.width(msg1)) / 2;
         int tx2 = this.panelX + (this.panelWidth - this.font.width(msg2)) / 2;
         g.text(this.font, msg1, tx1, this.panelY + 24, -12303292, false);
         g.text(this.font, msg2, tx2, this.panelY + 36, -10066330, false);
      } else {
         String msg1 = this.message.getString();
         int tx1 = this.panelX + (this.panelWidth - this.font.width(msg1)) / 2;
         g.text(this.font, msg1, tx1, this.panelY + 28, -12303292, false);
      }
   }

   public void onClose() {
      this.minecraft.gui.setScreen(this.parentScreen);
   }
}
