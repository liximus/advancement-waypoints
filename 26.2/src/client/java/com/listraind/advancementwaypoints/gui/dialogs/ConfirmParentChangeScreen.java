package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmParentChangeScreen extends BaseModScreen {
   private final Screen parentScreen;
   private final Runnable onOnlyCurrent;
   private final Runnable onWithChildren;

   public ConfirmParentChangeScreen(Screen parentScreen, Runnable onOnlyCurrent, Runnable onWithChildren) {
      super(Component.translatable("advwp.dialog.parent_change.title"), 240, 140);
      this.parentScreen = parentScreen;
      this.onOnlyCurrent = onOnlyCurrent;
      this.onWithChildren = onWithChildren;
   }

   protected void initContent() {
      int bw = this.panelWidth - 40;
      this.addCenteredButton(Component.translatable("advwp.dialog.parent_change.only_current"), 50, bw, 20, (b) -> this.onOnlyCurrent.run());
      this.addCenteredButton(Component.translatable("advwp.dialog.parent_change.with_children"), 78, bw, 20, (b) -> this.onWithChildren.run());
      this.addCenteredButton(Component.translatable("advwp.dialog.cancel"), 106, bw, 20, (b) -> this.minecraft.gui.setScreen(this.parentScreen));
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      super.extractRenderState(g, mx, my, d);
      String msg1 = Component.translatable("advwp.dialog.parent_change.message").getString();
      String msg2 = Component.translatable("advwp.dialog.parent_change.submessage").getString();
      int tx1 = this.panelX + (this.panelWidth - this.font.width(msg1)) / 2;
      int tx2 = this.panelX + (this.panelWidth - this.font.width(msg2)) / 2;
      g.text(this.font, msg1, tx1, this.panelY + 24, -12303292, false);
      g.text(this.font, msg2, tx2, this.panelY + 36, -10066330, false);
   }

   public void onClose() {
      this.minecraft.gui.setScreen(this.parentScreen);
   }
}
