package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.advancement.TextureHelper;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class BlockFacePickerScreen extends BaseModScreen {
   private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/button"),
      Identifier.withDefaultNamespace("widget/button_disabled"),
      Identifier.withDefaultNamespace("widget/button_highlighted")
   );

   private final Screen cancelScreen;
   private final Screen targetScreen;
   private final Item item;
   private final List<TextureHelper.BlockFace> faces;
   private final Consumer<Identifier> callback;
   private final int cols;
   private final int rows;
   private final int btnWidth = 76;
   private final int btnHeight = 64;
   private final int btnGap = 8;

   public BlockFacePickerScreen(Screen cancelScreen, Screen targetScreen, Item item, List<TextureHelper.BlockFace> faces, Consumer<Identifier> callback) {
      super(Component.translatable("advwp.picker.face.title"), 
            Math.max(210, Math.min(faces.size(), 3) * 76 + (Math.min(faces.size(), 3) - 1) * 8 + 36),
            ((int) Math.ceil((double) faces.size() / Math.min(faces.size(), 3))) * 64 + (((int) Math.ceil((double) faces.size() / Math.min(faces.size(), 3))) - 1) * 8 + 65);
      this.cancelScreen = cancelScreen;
      this.targetScreen = targetScreen;
      this.item = item;
      this.faces = faces;
      this.callback = callback;
      this.cols = Math.min(faces.size(), 3);
      this.rows = (int) Math.ceil((double) faces.size() / this.cols);
   }

   protected void initContent() {
      int totalWidth = this.cols * this.btnWidth + (this.cols - 1) * this.btnGap;
      int startX = this.panelX + (this.panelWidth - totalWidth) / 2;
      int startY = this.panelY + 26;

      for (int i = 0; i < this.faces.size(); ++i) {
         TextureHelper.BlockFace face = this.faces.get(i);
         int col = i % this.cols;
         int row = i / this.cols;
         int bx = startX + col * (this.btnWidth + this.btnGap);
         int by = startY + row * (this.btnHeight + this.btnGap);

         FaceButton btn = new FaceButton(bx, by, this.btnWidth, this.btnHeight, face, (b) -> {
            this.callback.accept(face.textureId());
            if (this.minecraft != null) {
               this.minecraft.gui.setScreen(this.targetScreen);
            }
         });
         this.addRenderableWidget(btn);
      }

      this.addCenteredButton(Component.translatable("advwp.dialog.cancel"), this.panelHeight - 24, this.panelWidth - 36, 20, (b) -> {
         this.onClose();
      });
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(this.cancelScreen);
      }
   }

   private static class FaceButton extends Button {
      private final TextureHelper.BlockFace face;

      public FaceButton(int x, int y, int width, int height, TextureHelper.BlockFace face, Button.OnPress onPress) {
         super(x, y, width, height, face.name(), onPress, DEFAULT_NARRATION);
         this.face = face;
         this.setTooltip(Tooltip.create(face.name()));
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
         Identifier sprite = BUTTON_SPRITES.get(this.active, this.isHoveredOrFocused());
         g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());

         int previewSize = 32;
         int previewX = this.getX() + (this.getWidth() - previewSize) / 2;
         int previewY = this.getY() + 6;
         TextureHelper.blitBackground(g, RenderPipelines.GUI_TEXTURED, TextureHelper.toTextureFilePath(this.face.textureId()), previewX, previewY, previewSize, previewSize);

         Minecraft mc = Minecraft.getInstance();
         if (mc.font != null) {
            Component label = this.face.name();
            int textWidth = mc.font.width(label);
            int maxTextWidth = this.getWidth() - 6;
            int textColor = this.isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF;

            if (textWidth > maxTextWidth) {
               float textScale = (float) maxTextWidth / (float) textWidth;
               g.pose().pushMatrix();
               g.pose().translate(this.getX() + this.getWidth() / 2.0F, this.getY() + 46.0F);
               g.pose().scale(textScale, textScale);
               g.text(mc.font, label, -textWidth / 2, 0, textColor, true);
               g.pose().popMatrix();
            } else {
               int tx = this.getX() + (this.getWidth() - textWidth) / 2;
               int ty = this.getY() + 46;
               g.text(mc.font, label, tx, ty, textColor, true);
            }
         }
      }
   }
}
