package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.DarkModeChecker;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.shapes.Shapes;

public class ItemPickerScreen extends BaseModScreen {
   @FunctionalInterface
   public interface ItemSelectionHandler {
      void handle(ItemPickerScreen picker, Item item);
   }

   private static final Identifier SLOTS_LIGHT = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/slots.png");
   private static final Identifier SLOTS_DARK = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/slotsdark.png");
   private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
   private static final int CELL = 18;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HANDLE_HEIGHT = 15;
   private static final Set<String> FUNCTIONAL_BLOCK_IDS = Set.of("crafting_table", "smithing_table", "cartography_table", "fletching_table", "tnt", "note_block", "observer", "piston", "sticky_piston", "target", "redstone_lamp", "redstone_block");
   private final Screen parent;
   private final ItemSelectionHandler handler;
   private final boolean backgroundsOnly;
   private final List<Item> allItems;
   private List<Item> filtered;
   private EditBox searchField;
   private int cols;
   private int scrollRow;
   private float scrollProgress;
   private boolean dragging;

   public ItemPickerScreen(Screen parent, Consumer<Item> callback) {
      this(parent, false, (picker, item) -> {
         callback.accept(item);
         picker.onClose();
      });
   }

   public ItemPickerScreen(Screen parent, boolean backgroundsOnly, Consumer<Item> callback) {
      this(parent, backgroundsOnly, (picker, item) -> {
         callback.accept(item);
         picker.onClose();
      });
   }

   public ItemPickerScreen(Screen parent, boolean backgroundsOnly, ItemSelectionHandler handler) {
      super(backgroundsOnly ? Component.translatable("advwp.picker.background.title") : Component.translatable("advwp.picker.icon.title"), 206, 160);
      this.allItems = new ArrayList();
      this.filtered = new ArrayList();
      this.cols = 9;
      this.scrollRow = 0;
      this.scrollProgress = 0.0F;
      this.dragging = false;
      this.parent = parent;
      this.backgroundsOnly = backgroundsOnly;
      this.handler = handler;
      this.initItems();
   }

   private void initItems() {
      if (this.backgroundsOnly) {
         this.allItems.add(Items.STONE);
         Iterator var1 = BuiltInRegistries.ITEM.iterator();

         label59:
         while(true) {
            Item item;
            do {
               do {
                  if (!var1.hasNext()) {
                     break label59;
                  }

                  item = (Item)var1.next();
               } while(!(item instanceof BlockItem));
            } while(item == Items.STONE);

            BlockItem bi = (BlockItem)item;
            Block block = bi.getBlock();
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            if (!FUNCTIONAL_BLOCK_IDS.contains(path)) {
               if (block instanceof EntityBlock) {
                  continue;
               }

               if (path.contains("shulker_box") || path.contains("banner") || path.contains("sign") || path.contains("chest") || path.contains("bed") || path.contains("door") || path.contains("fence") || path.contains("gate") || path.contains("wall") || path.contains("slab") || path.contains("stairs") || path.contains("torch") || path.contains("lantern") || path.contains("carpet") || path.contains("candle") || path.contains("pane") || path.contains("head") || path.contains("skull") || path.contains("pot") || path.contains("rail") || path.contains("wire") || path.contains("dust") || path.contains("coral") || path.contains("plant") || path.contains("sapling") || path.contains("leaves") || path.contains("glass") || path.contains("ice") || path.contains("chain") || path.contains("rod") || path.contains("bell") || path.contains("cauldron") || path.contains("anvil") || path.contains("hopper") || path.contains("brewing") || path.contains("conduit") || path.contains("beacon") || path.contains("portal") || path.contains("spawner") || path.contains("egg") || path.contains("cake") || path.contains("bush") || path.contains("flower") || path.contains("crop") || path.contains("stem") || path.contains("vine") || path.contains("scaffolding") || path.contains("pointed_dripstone") || path.contains("froglight") || path.contains("lightning") || path.contains("comparator") || path.contains("repeater") || path.contains("tripwire") || path.contains("daylight") || path.contains("sensor") || path.contains("lever") || path.contains("button") || path.contains("pressure_plate")) {
                  continue;
               }

               try {
                  if (block.defaultBlockState().isAir() || !block.defaultBlockState().canOcclude() || block.defaultBlockState().getCollisionShape((BlockGetter)null, BlockPos.ZERO) != Shapes.block()) {
                     continue;
                  }
               } catch (Throwable var7) {
                  continue;
               }
            }

            this.allItems.add(item);
         }
      } else {
         for(Item item : BuiltInRegistries.ITEM) {
            this.allItems.add(item);
         }
      }

      this.filtered = new ArrayList(this.allItems);
   }

   protected void initContent() {
      int searchWidth = this.panelWidth - 20;
      String prevText = this.searchField != null ? this.searchField.getValue() : "";
      int prevScrollRow = this.scrollRow;
      float prevScrollProgress = this.scrollProgress;
      this.searchField = (EditBox)this.addRenderableWidget(new EditBox(this.font, this.panelX + (this.panelWidth - searchWidth) / 2, this.panelY + 18, searchWidth, 16, Component.literal("")));
      this.searchField.setHint(Component.translatable(this.backgroundsOnly ? "advwp.hint.search.background" : "advwp.hint.search.item"));
      this.searchField.setResponder(this::onSearchChanged);
      if (!prevText.isEmpty()) {
         this.searchField.setValue(prevText);
         this.scrollRow = prevScrollRow;
         this.scrollProgress = prevScrollProgress;
      }
   }

   private void onSearchChanged(String text) {
      String query = text.toLowerCase().trim();
      if (query.isEmpty()) {
         this.filtered = new ArrayList(this.allItems);
      } else {
         this.filtered = this.allItems.stream().filter((i) -> (new ItemStack(i)).getHoverName().getString().toLowerCase().contains(query) || BuiltInRegistries.ITEM.getKey(i).getPath().contains(query)).toList();
      }

      this.scrollRow = 0;
      this.scrollProgress = 0.0F;
   }

   private int gridLeft() {
      return this.panelX + 13;
   }

   private int gridTop() {
      return this.panelY + 38;
   }

   private int visRows() {
      return 6;
   }

   private int scrollbarX() {
      return this.gridLeft() + this.cols * 18 + 3;
   }

   private int scrollbarHeight() {
      return this.visRows() * 18;
   }

   private int maxRow() {
      return Math.max(0, (int)Math.ceil((double)this.filtered.size() / (double)this.cols) - this.visRows());
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
      Identifier slots = DarkModeChecker.isDarkModeEnabled() ? SLOTS_DARK : SLOTS_LIGHT;
      super.extractRenderState(g, mx, my, d);

      for(int r = 0; r < this.visRows(); ++r) {
         for(int c = 0; c < this.cols; ++c) {
            int sx = this.gridLeft() + c * 18;
            int sy = this.gridTop() + r * 18;
            g.blit(RenderPipelines.GUI_TEXTURED, slots, sx, sy, 0.0F, 0.0F, 18, 18, 90, 54);
         }
      }

      Item hovered = null;

      for(int row = 0; row < this.visRows(); ++row) {
         for(int col = 0; col < this.cols; ++col) {
            int idx = (this.scrollRow + row) * this.cols + col;
            if (idx >= this.filtered.size()) {
               break;
            }

            int x = this.gridLeft() + col * 18;
            int y = this.gridTop() + row * 18;
            Item item = (Item)this.filtered.get(idx);
            g.item(new ItemStack(item), x + 1, y + 1);
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
               g.fill(x, y, x + 18, y + 18, 1358954495);
               hovered = item;
            }
         }
      }

      int scrollHandleY = this.gridTop() + (int)((float)(this.scrollbarHeight() - 15) * this.scrollProgress);
      g.fill(this.scrollbarX(), this.gridTop(), this.scrollbarX() + 12, this.gridTop() + this.scrollbarHeight(), Integer.MIN_VALUE);
      g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, this.scrollbarX(), scrollHandleY, 12, 15);
      if (hovered != null) {
         String tooltipText = (new ItemStack(hovered)).getHoverName().getString();
         int tx = mx + 12;
         int ty = my - 4;
         int tw = this.font.width(tooltipText);
         g.fill(tx - 3, ty - 3, tx + tw + 3, ty + 12, -267386864);
         g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 11, -265809840);
         g.text(this.font, tooltipText, tx, ty, -1, true);
      }

   }

   public boolean mouseClicked(MouseButtonEvent event, boolean unknown) {
      double mx = event.x();
      double my = event.y();
      if (this.maxRow() > 0 && mx >= (double)this.scrollbarX() && mx < (double)(this.scrollbarX() + 12) && my >= (double)this.gridTop() && my < (double)(this.gridTop() + this.scrollbarHeight())) {
         this.dragging = true;
         this.updateScrollFromMouse(my);
         return true;
      } else {
         for(int row = 0; row < this.visRows(); ++row) {
            for(int col = 0; col < this.cols; ++col) {
               int idx = (this.scrollRow + row) * this.cols + col;
               if (idx >= this.filtered.size()) {
                  break;
               }

               int x = this.gridLeft() + col * 18;
               int y = this.gridTop() + row * 18;
                if (mx >= (double)x && mx < (double)(x + 18) && my >= (double)y && my < (double)(y + 18)) {
                   this.handler.handle(this, (Item)this.filtered.get(idx));
                   return true;
                }
            }
         }

         return super.mouseClicked(event, unknown);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      this.dragging = false;
      return super.mouseReleased(event);
   }

   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.dragging && this.maxRow() > 0) {
         this.updateScrollFromMouse(event.y());
         return true;
      } else {
         return super.mouseDragged(event, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
      if (this.maxRow() > 0) {
         this.scrollRow = Math.clamp((long)(this.scrollRow - (int)Math.signum(scrollY)), 0, this.maxRow());
         this.scrollProgress = (float)this.scrollRow / (float)this.maxRow();
         return true;
      } else {
         return super.mouseScrolled(mx, my, scrollX, scrollY);
      }
   }

   private void updateScrollFromMouse(double my) {
      float relY = (float)(my - (double)this.gridTop() - (double)7.5F);
      this.scrollProgress = Math.clamp(relY / (float)(this.scrollbarHeight() - 15), 0.0F, 1.0F);
      this.scrollRow = Math.round(this.scrollProgress * (float)this.maxRow());
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(this.parent);
      }

   }
}
