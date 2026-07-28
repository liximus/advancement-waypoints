package com.listraind.advancementwaypoints.gui.base;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PopupMenu {
   private boolean visible = false;
   private int x;
   private int y;
   private int menuWidth;
   private int menuHeight;
   private Runnable onHideCallback;
   private static final int PADDING = 3;
   private static final int BUTTON_HEIGHT = 16;
   private static final int BUTTON_GAP = 4;
   private static final int SQUARE_GAP = 8;
   private final List<TextItem> textItems = new ArrayList();
   private final List<SquareItem> squareItems = new ArrayList();
   private final List<Button> textButtons = new ArrayList();
   private final List<Button> squareButtons = new ArrayList();

   public void clear() {
      this.textItems.clear();
      this.squareItems.clear();
      this.textButtons.clear();
      this.squareButtons.clear();
   }

   public PopupMenu addTextButton(Component label, MenuAction action, boolean active) {
      this.textItems.add(new TextItem(label, action, active));
      return this;
   }

   public PopupMenu addTextButton(Component label, MenuAction action) {
      return this.addTextButton(label, action, true);
   }

   public PopupMenu addSquareButton(Identifier icon, Component tooltip, MenuAction action, boolean active) {
      this.squareItems.add(new SquareItem(icon, tooltip, action, active));
      return this;
   }

   public PopupMenu setOnHideCallback(Runnable callback) {
      this.onHideCallback = callback;
      return this;
   }

   public void show(int mouseX, int mouseY) {
      this.visible = true;
      this.textButtons.clear();
      this.squareButtons.clear();
      Minecraft minecraft = Minecraft.getInstance();
      Screen screen = minecraft.gui.screen();
      if (screen != null) {
         Font font = minecraft.font;
         int minWidth = 100;

         for(TextItem item : this.textItems) {
            int w = font.width(item.label.getString()) + 6 + 16;
            if (w > minWidth) {
               minWidth = w;
            }
         }

         this.menuWidth = minWidth;
         int innerWidth = this.menuWidth - 6;
         int textRows = this.textItems.size();
         int squareCount = this.squareItems.size();
         int heightCalc = 3;
         if (textRows > 0) {
            heightCalc += textRows * 16 + (textRows - 1) * 4;
         }

         if (squareCount > 0) {
            if (textRows > 0) {
               heightCalc += 4;
            }

            int squareSize = (innerWidth - (squareCount - 1) * 8) / squareCount;
            heightCalc += squareSize;
         }

         heightCalc += 3;
         this.menuHeight = heightCalc;
         int screenWidth = screen.width;
         int screenHeight = screen.height;
         int offset = 8;
         this.x = mouseX + offset;
         this.y = mouseY + offset;
         if (this.x + this.menuWidth > screenWidth - 2) {
            this.x = mouseX - this.menuWidth - offset;
         }

         if (this.y + this.menuHeight > screenHeight - 2) {
            this.y = mouseY - this.menuHeight - offset;
         }

         if (this.x < 2) {
            this.x = 2;
         }

         if (this.y < 2) {
            this.y = 2;
         }

         if (this.x + this.menuWidth > screenWidth - 2) {
            this.x = screenWidth - this.menuWidth - 2;
         }

         if (this.y + this.menuHeight > screenHeight - 2) {
            this.y = screenHeight - this.menuHeight - 2;
         }

         int currentY = this.y + 3;

         for(TextItem item : this.textItems) {
            Button btn = Button.builder(item.label, (b) -> {
            }).bounds(this.x + 3, currentY, innerWidth, 16).build();
            btn.active = item.active;
            this.textButtons.add(btn);
            currentY += 20;
         }

         if (squareCount > 0) {
            int squareSize = (innerWidth - (squareCount - 1) * 8) / squareCount;

            for(int i = 0; i < squareCount; ++i) {
               SquareItem item = (SquareItem)this.squareItems.get(i);
               int bx = this.x + 3 + i * (squareSize + 8);
               Button.Builder builder = Button.builder(Component.literal(""), (b) -> {
               }).bounds(bx, currentY, squareSize, squareSize);
               if (item.tooltip != null) {
                  builder.tooltip(Tooltip.create(item.tooltip));
               }

               Button btn = builder.build();
               btn.active = item.active;
               this.squareButtons.add(btn);
            }
         }

      }
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void hide() {
      this.visible = false;
      this.clear();
      if (this.onHideCallback != null) {
         this.onHideCallback.run();
      }

   }

   public void render(GuiGraphicsExtractor g, int mx, int my, float pt) {
      if (this.visible) {
         g.blit(RenderPipelines.GUI_TEXTURED, ModBackground.current(), this.x, this.y, 0.0F, 0.0F, this.menuWidth, this.menuHeight, this.menuWidth, this.menuHeight);

         for(Button btn : this.textButtons) {
            btn.extractRenderState(g, mx, my, pt);
         }

         for(int i = 0; i < this.squareButtons.size(); ++i) {
            Button btn = (Button)this.squareButtons.get(i);
            btn.extractRenderState(g, mx, my, pt);
            SquareItem item = (SquareItem)this.squareItems.get(i);
            if (item.icon != null) {
               int iconSize = 11;
               int ix = btn.getX() + (btn.getWidth() - iconSize) / 2;
               int iy = btn.getY() + (btn.getHeight() - iconSize) / 2;
               g.blit(RenderPipelines.GUI_TEXTURED, item.icon, ix, iy, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
         }

      }
   }

   public boolean isMouseOver(double mx, double my) {
      return this.visible && mx >= (double)this.x && mx <= (double)(this.x + this.menuWidth) && my >= (double)this.y && my <= (double)(this.y + this.menuHeight);
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (!this.visible) {
         return false;
      } else if (!this.isMouseOver(mx, my)) {
         this.hide();
         return false;
      } else {
         if (btn == 0) {
            for(int i = 0; i < this.textButtons.size(); ++i) {
               Button button = (Button)this.textButtons.get(i);
               if (button.active && isHit(mx, my, button)) {
                  TextItem item = (TextItem)this.textItems.get(i);
                  if (item.action != null) {
                     item.action.execute();
                  }

                  this.hide();
                  return true;
               }
            }

            for(int i = 0; i < this.squareButtons.size(); ++i) {
               Button button = (Button)this.squareButtons.get(i);
               if (button.active && isHit(mx, my, button)) {
                  SquareItem item = (SquareItem)this.squareItems.get(i);
                  if (item.action != null) {
                     item.action.execute();
                  }

                  this.hide();
                  return true;
               }
            }
         }

         return true;
      }
   }

   private static boolean isHit(double mx, double my, Button b) {
      return mx >= (double)b.getX() && mx <= (double)(b.getX() + b.getWidth()) && my >= (double)b.getY() && my <= (double)(b.getY() + b.getHeight());
   }

   public static class TextItem {
      public final Component label;
      public final MenuAction action;
      public final boolean active;

      public TextItem(Component label, MenuAction action, boolean active) {
         this.label = label;
         this.action = action;
         this.active = active;
      }
   }

   public static class SquareItem {
      public final Identifier icon;
      public final Component tooltip;
      public final MenuAction action;
      public final boolean active;

      public SquareItem(Identifier icon, Component tooltip, MenuAction action, boolean active) {
         this.icon = icon;
         this.tooltip = tooltip;
         this.action = action;
         this.active = active;
      }
   }

   @FunctionalInterface
   public interface MenuAction {
      void execute();
   }
}
