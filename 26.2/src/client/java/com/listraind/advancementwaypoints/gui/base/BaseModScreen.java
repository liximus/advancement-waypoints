package com.listraind.advancementwaypoints.gui.base;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public abstract class BaseModScreen extends Screen {
   protected int panelWidth;
   protected int panelHeight;
   protected int panelX;
   protected int panelY;
   protected boolean drawTitle = true;
   protected int titleColor = -14540254;

   protected BaseModScreen(Component title, int panelWidth, int panelHeight) {
      super(title);
      this.panelWidth = panelWidth;
      this.panelHeight = panelHeight;
   }

   protected void init() {
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.initContent();
   }

   protected abstract void initContent();

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      g.blit(RenderPipelines.GUI_TEXTURED, ModBackground.current(), this.panelX, this.panelY, 0.0F, 0.0F, this.panelWidth, this.panelHeight, this.panelWidth, this.panelHeight);
      super.extractRenderState(g, mx, my, d);
      if (this.drawTitle && this.title != null) {
         int tx = this.panelX + (this.panelWidth - this.font.width(this.title)) / 2;
         g.text(this.font, this.title, tx, this.panelY + 10, this.titleColor, false);
      }

   }

   protected Button addCenteredButton(Component label, int relativeY, int width, int height, Button.OnPress onPress) {
      int bx = this.panelX + (this.panelWidth - width) / 2;
      Button btn = Button.builder(label, onPress).bounds(bx, this.panelY + relativeY, width, height).build();
      return (Button)this.addRenderableWidget(btn);
   }
}
