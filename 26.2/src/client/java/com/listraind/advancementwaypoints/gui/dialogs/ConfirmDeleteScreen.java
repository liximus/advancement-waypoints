package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteScreen extends BaseModScreen {
   private final Screen parentScreen;
   private final Runnable onConfirm;

   public ConfirmDeleteScreen(Screen parentScreen, Runnable onConfirm) {
      super(Component.translatable("advwp.dialog.delete.title"), 240, 90);
      this.parentScreen = parentScreen;
      this.onConfirm = onConfirm;
   }

   protected void initContent() {
      int buttonWidth = (this.panelWidth - 50) / 2;
      int y = 52;
      this.addRenderableWidget(Button.builder(Component.translatable("advwp.dialog.delete.confirm"), (b) -> this.onConfirm.run()).bounds(this.panelX + 20, this.panelY + y, buttonWidth, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("advwp.dialog.cancel"), (b) -> this.minecraft.gui.setScreen(this.parentScreen)).bounds(this.panelX + 20 + buttonWidth + 10, this.panelY + y, buttonWidth, 20).build());
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      super.extractRenderState(g, mx, my, d);
      String msg1 = Component.translatable("advwp.dialog.delete.message").getString();
      int tx1 = this.panelX + (this.panelWidth - this.font.width(msg1)) / 2;
      g.text(this.font, msg1, tx1, this.panelY + 28, -12303292, false);
   }

   public void onClose() {
      this.minecraft.gui.setScreen(this.parentScreen);
   }
}
