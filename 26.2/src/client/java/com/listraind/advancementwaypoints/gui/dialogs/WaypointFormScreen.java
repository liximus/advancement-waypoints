package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import com.listraind.advancementwaypoints.gui.base.ModBackground;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public abstract class WaypointFormScreen extends BaseModScreen {
   protected static final int FIELD_HEIGHT = 18;
   protected static final int BUTTON_HEIGHT = 20;
   protected static final int COORD_FIELD_WIDTH = 52;
   protected static final int GAP = 4;
   protected static final int FILL_BUTTON_WIDTH = 60;
   protected Item selectedIcon;
   protected Identifier selectedParentId;
   protected String savedName;
   protected String savedDesc;
   protected String savedBackground;
   protected List<CoordRow> coordRows;
   protected boolean isVanilla;
   protected boolean hadParentBefore;
   public Runnable onCloseAction;
   protected EditBox nameField;
   protected EditBox descField;
   protected Button iconButton;
   protected Button parentButton;
   protected Button bgButton;
   protected float scale;
   protected int virtualWidth;
   protected int virtualHeight;
   protected int separator1Y;
   protected int separator2Y;

   protected WaypointFormScreen(Component title) {
      super(title, 230, 230);
      this.selectedIcon = Items.GRASS_BLOCK;
      this.savedName = "";
      this.savedDesc = "";
      this.savedBackground = "";
      this.coordRows = new ArrayList();
      this.isVanilla = false;
      this.hadParentBefore = false;
      this.onCloseAction = () -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null) {
            mc.gui.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements(), (Screen)null));
         } else if (mc != null) {
            mc.gui.setScreen((Screen)null);
         }

      };
      this.scale = 1.0F;
   }

   protected boolean isRoot() {
      return this.selectedParentId == null && !this.isVanilla;
   }

   protected boolean showParentField() {
      return true;
   }

   protected boolean showBackgroundField() {
      return false;
   }

   protected boolean showCoordsField() {
      return true;
   }

   protected boolean showResetParentButton() {
      return true;
   }

   protected boolean hideFieldsUntilParentSelected() {
      return false;
   }

   protected void initContent() {
      boolean awaitingParentSelection = this.hideFieldsUntilParentSelected() && this.selectedParentId == null && !this.isVanilla;
      boolean fieldsVisible = !awaitingParentSelection;
      boolean coordsVisible = this.showCoordsField() && fieldsVisible;
      int totalContentHeight = this.computeContentHeight(fieldsVisible, coordsVisible);
      this.scale = Math.min(1.0F, (float)(this.height - 10) / (float)totalContentHeight);
      this.virtualWidth = (int)((float)this.width / this.scale);
      this.virtualHeight = (int)((float)this.height / this.scale);
      this.panelWidth = Math.min(this.virtualWidth - 20, 340);
      this.panelHeight = totalContentHeight;
      this.panelX = (this.virtualWidth - this.panelWidth) / 2;
      this.panelY = (this.virtualHeight - this.panelHeight) / 2;
      int centerX = this.panelX + this.panelWidth / 2;
      int fieldLeft = centerX - (this.panelWidth - 40) / 2;
      int currentY = this.panelY + 12 + 14 + 4;
      if (fieldsVisible) {
         currentY = this.initNameAndIcon(fieldLeft, currentY);
      }

      if (this.showParentField()) {
         currentY = this.initParentField(fieldLeft, currentY, awaitingParentSelection);
      }

      if (this.showBackgroundField() && fieldsVisible) {
         currentY = this.initBackgroundField(fieldLeft, currentY);
      } else {
         this.bgButton = null;
      }

      if (fieldsVisible) {
         currentY = this.initDescriptionField(fieldLeft, currentY);
      }

      if (coordsVisible) {
         currentY = this.initCoordsField(centerX, fieldLeft, currentY);
      } else {
         this.separator2Y = this.separator1Y;
      }

      this.initActions(centerX, currentY);
   }

   private int computeContentHeight(boolean fieldsVisible, boolean coordsVisible) {
      int h = 30;
      if (fieldsVisible) {
         h += 44;
      }

      if (this.showParentField()) {
         h += 22;
      }

      if (this.showBackgroundField() && fieldsVisible) {
         h += 22;
      }

      if (fieldsVisible) {
         h += 31;
      }

      if (coordsVisible) {
         h += this.coordRows.isEmpty() ? 0 : this.coordRows.size() * 46;
         h += 33;
      }

      return h + 20 + 12;
   }

   private int initNameAndIcon(int fieldLeft, int y) {
      int buttonWidth = this.panelWidth - 40 - 25;
      this.nameField = this.addBox(fieldLeft, y, this.panelWidth - 40, "advwp.hint.name", this.savedName);
      if (this.isVanilla) {
         this.nameField.setEditable(false);
      }

      y += 22;
      this.iconButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.field.icon", new Object[]{this.iconId()}), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.minecraft.gui.setScreen(new ItemPickerScreen(this, (item) -> this.selectedIcon = item));
      }).bounds(fieldLeft, y, buttonWidth, 18).build());
      this.iconButton.active = !this.isVanilla;
      y += 22;
      return y;
   }

   private int initParentField(int fieldLeft, int y, boolean awaitingParentSelection) {
      int buttonWidth = this.panelWidth - 40 - 25;
      Component parentDisplayName = this.selectedParentId != null ? Component.literal(this.parentName()) : Component.translatable(awaitingParentSelection ? "advwp.field.parent.select" : "advwp.field.parent.none");
      this.parentButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.field.parent", new Object[]{parentDisplayName}), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.hadParentBefore = this.selectedParentId != null;
         this.openParentPicker();
      }).bounds(fieldLeft, y, buttonWidth, 18).build());
      this.parentButton.active = !this.isVanilla;
      if (this.showResetParentButton()) {
         Button resetParentButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.field.parent.reset"), (b) -> {
            this.selectedParentId = null;
            this.parentButton.setMessage(Component.translatable("advwp.field.parent", new Object[]{Component.translatable("advwp.field.parent.none")}));
            this.saveState();
            this.rebuildWidgets();
         }).bounds(fieldLeft + buttonWidth + 5, y, 20, 18).build());
         resetParentButton.active = !this.isVanilla;
      }

      return y + 18 + 4;
   }

   private int initBackgroundField(int fieldLeft, int y) {
      Component backgroundLabel = this.savedBackground != null && !this.savedBackground.isEmpty() ? Component.literal(this.shortBgName(this.savedBackground)) : Component.translatable("advwp.field.background.default");
      this.bgButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.field.background", new Object[]{backgroundLabel}), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.minecraft.gui.setScreen(new ItemPickerScreen(this, true, (item) -> {
            Identifier blockId = BuiltInRegistries.ITEM.getKey(item);
            if (blockId != null) {
               String var10001 = blockId.getNamespace();
               this.savedBackground = var10001 + ":block/" + blockId.getPath();
            }

         }));
      }).bounds(fieldLeft, y, this.panelWidth - 40, 18).build());
      this.bgButton.active = !this.isVanilla;
      return y + 18 + 4;
   }

   private int initDescriptionField(int fieldLeft, int y) {
      this.descField = this.addBox(fieldLeft, y, this.panelWidth - 40, "advwp.hint.description", this.savedDesc);
      this.descField.setMaxLength(512);
      if (this.isVanilla) {
         this.descField.setEditable(false);
      }

      y += 18;
      y += 6;
      this.separator1Y = y;
      y += 7;
      return y;
   }

   private int initCoordsField(int centerX, int fieldLeft, int y) {
      int coordRowTotalWidth = 164;
      int coordRowLeft = centerX - (coordRowTotalWidth + 4 + 60) / 2;

      for(CoordRow coordRow : this.coordRows) {
         Button deleteRowBtn = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.coord.row.delete"), (b) -> {
            this.saveState();
            this.coordRows.remove(coordRow);
            this.setFocused((GuiEventListener)null);
            this.rebuildWidgets();
         }).bounds(coordRowLeft + coordRowTotalWidth + 4, y, 60, 16).build());
         deleteRowBtn.active = !this.isVanilla;
         int coordFieldY = y + 18;
         coordRow.bx = this.addCoord(coordRowLeft, coordFieldY, "X");
         coordRow.bx.setValue(coordRow.sx);
         coordRow.by = this.addCoord(coordRowLeft + 52 + 4, coordFieldY, "Y");
         coordRow.by.setValue(coordRow.sy);
         coordRow.bz = this.addCoord(coordRowLeft + 112, coordFieldY, "Z");
         coordRow.bz.setValue(coordRow.sz);
         if (this.isVanilla) {
            coordRow.bx.setEditable(false);
            coordRow.by.setEditable(false);
            coordRow.bz.setEditable(false);
         }

         boolean isCurrentDimension = coordRow.dim == this.currentDim();
         Button fillCoordsButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.coord.row.fill"), (b) -> {
            if (this.minecraft.player != null) {
               coordRow.bx.setValue(String.valueOf((int)this.minecraft.player.getX()));
               coordRow.by.setValue(String.valueOf((int)this.minecraft.player.getY()));
               coordRow.bz.setValue(String.valueOf((int)this.minecraft.player.getZ()));
            }
         }).bounds(coordRowLeft + coordRowTotalWidth + 4, coordFieldY - 2, 60, 20).build());
         fillCoordsButton.active = isCurrentDimension && !this.isVanilla;
         y += 46;
      }

      Button addCoordsButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("advwp.coord.row.add"), (b) -> {
         this.setFocused((GuiEventListener)null);
         this.minecraft.gui.setScreen(new DimensionPickerScreen(this));
      }).bounds(fieldLeft, y, this.panelWidth - 40, 20).build());
      addCoordsButton.active = !this.isVanilla;
      y += 20;
      y += 6;
      this.separator2Y = y;
      y += 7;
      return y;
   }

   protected abstract void initActions(int var1, int var2);

   public boolean mouseClicked(MouseButtonEvent event, boolean unknown) {
      return super.mouseClicked(new MouseButtonEvent(event.x() / (double)this.scale, event.y() / (double)this.scale, event.buttonInfo()), unknown);
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      return super.mouseReleased(new MouseButtonEvent(event.x() / (double)this.scale, event.y() / (double)this.scale, event.buttonInfo()));
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      return super.mouseDragged(new MouseButtonEvent(event.x() / (double)this.scale, event.y() / (double)this.scale, event.buttonInfo()), deltaX / (double)this.scale, deltaY / (double)this.scale);
   }

   public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(mouseX / (double)this.scale, mouseY / (double)this.scale);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return super.mouseScrolled(mouseX / (double)this.scale, mouseY / (double)this.scale, scrollX, scrollY);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      graphics.pose().pushMatrix();
      graphics.pose().scale(this.scale, this.scale);
      int scaledMouseX = (int)((float)mouseX / this.scale);
      int scaledMouseY = (int)((float)mouseY / this.scale);
      graphics.blit(RenderPipelines.GUI_TEXTURED, ModBackground.current(), this.panelX, this.panelY, 0.0F, 0.0F, this.panelWidth, this.panelHeight, this.panelWidth, this.panelHeight);
      super.extractRenderState(graphics, scaledMouseX, scaledMouseY, delta);
      if (this.iconButton != null) {
         this.iconButton.setMessage(Component.translatable("advwp.field.icon", new Object[]{this.iconId()}));
         graphics.item(new ItemStack(this.selectedIcon), this.iconButton.getX() + this.iconButton.getWidth() + 7, this.iconButton.getY() + 1);
      }

      if (this.bgButton != null) {
         Component backgroundLabel = this.savedBackground != null && !this.savedBackground.isEmpty() ? Component.literal(this.shortBgName(this.savedBackground)) : Component.translatable("advwp.field.background.default");
         this.bgButton.setMessage(Component.translatable("advwp.field.background", new Object[]{backgroundLabel}));
      }

      if (this.separator1Y != 0) {
         graphics.fill(this.panelX + 15, this.separator1Y, this.panelX + this.panelWidth - 15, this.separator1Y + 1, -8947849);
         if (this.separator2Y != this.separator1Y) {
            graphics.fill(this.panelX + 15, this.separator2Y, this.panelX + this.panelWidth - 15, this.separator2Y + 1, -8947849);
         }
      }

      if (!this.isRoot() && this.selectedParentId != null) {
         int coordRowTotalWidth = 164;
         int coordRowLeft = this.panelX + this.panelWidth / 2 - (coordRowTotalWidth + 4 + 60) / 2;

         for(CoordRow coordRow : this.coordRows) {
            if (coordRow.bx != null) {
               graphics.text(this.font, Component.translatable(CoordParser.DIM_LABEL_KEYS[coordRow.dim]), coordRowLeft, coordRow.bx.getY() - 11, -1, false);
            }
         }
      }

      graphics.pose().popMatrix();
   }

   public void removed() {
      this.saveState();
   }

   public void addDimRow(int dim) {
      CoordRow newRow = new CoordRow(dim);
      if (dim == this.currentDim() && this.minecraft != null && this.minecraft.player != null) {
         newRow.sx = String.valueOf((int)this.minecraft.player.getX());
         newRow.sy = String.valueOf((int)this.minecraft.player.getY());
         newRow.sz = String.valueOf((int)this.minecraft.player.getZ());
      }

      this.coordRows.add(newRow);
   }

   public void onParentSelected(Identifier newParent) {
      this.selectedParentId = newParent;
      if (!this.hadParentBefore && this.coordRows.isEmpty()) {
         CoordRow newRow = new CoordRow(this.currentDim());
         if (this.minecraft != null && this.minecraft.player != null) {
            newRow.sx = String.valueOf((int)this.minecraft.player.getX());
            newRow.sy = String.valueOf((int)this.minecraft.player.getY());
            newRow.sz = String.valueOf((int)this.minecraft.player.getZ());
         }

         this.coordRows.add(newRow);
      }

   }

   protected void saveState() {
      if (this.nameField != null) {
         this.savedName = this.nameField.getValue();
      }

      if (this.descField != null) {
         this.savedDesc = this.descField.getValue();
      }

      for(CoordRow coordRow : this.coordRows) {
         if (coordRow.bx != null) {
            coordRow.sx = coordRow.bx.getValue();
         }

         if (coordRow.by != null) {
            coordRow.sy = coordRow.by.getValue();
         }

         if (coordRow.bz != null) {
            coordRow.sz = coordRow.bz.getValue();
         }
      }

   }

   protected List<CoordParser.DimCoords> collectCoords() {
      List<List<String[]>> coordsPerDimension = new ArrayList();

      for(int i = 0; i < 4; ++i) {
         coordsPerDimension.add(new ArrayList());
      }

      for(CoordRow coordRow : this.coordRows) {
         String x = coordRow.bx != null ? coordRow.bx.getValue().trim() : coordRow.sx.trim();
         String y = coordRow.by != null ? coordRow.by.getValue().trim() : coordRow.sy.trim();
         String z = coordRow.bz != null ? coordRow.bz.getValue().trim() : coordRow.sz.trim();
         if (!x.isEmpty() || !y.isEmpty() || !z.isEmpty()) {
            ((List)coordsPerDimension.get(coordRow.dim)).add(new String[]{x.isEmpty() ? "0" : x, y.isEmpty() ? "0" : y, z.isEmpty() ? "0" : z});
         }
      }

      List<CoordParser.DimCoords> result = new ArrayList();

      for(int i = 0; i < 4; ++i) {
         if (!((List)coordsPerDimension.get(i)).isEmpty()) {
            result.add(new CoordParser.DimCoords(i, (List)coordsPerDimension.get(i)));
         }
      }

      return result;
   }

   protected String buildFinalDescription() {
      String description = this.isRoot() ? (this.descField != null ? this.descField.getValue().trim() : this.savedDesc) : CoordParser.buildDescription((List)(this.selectedParentId != null ? this.collectCoords() : new ArrayList()), this.descField != null ? this.descField.getValue().trim() : this.savedDesc);
      return this.colorCodes(description);
   }

   protected String getBackgroundValue() {
      if (this.savedBackground != null && !this.savedBackground.isEmpty()) {
         return this.savedBackground;
      } else {
         String var10000 = BuiltInRegistries.ITEM.getKey(Items.STONE).getNamespace();
         return var10000 + ":block/" + BuiltInRegistries.ITEM.getKey(Items.STONE).getPath();
      }
   }

   protected String iconId() {
      Identifier iconLocation = BuiltInRegistries.ITEM.getKey(this.selectedIcon);
      return iconLocation.toString();
   }

   protected int currentDim() {
      if (this.minecraft != null && this.minecraft.level != null) {
         String dimensionId = this.minecraft.level.dimension().identifier().toString();
         if (!dimensionId.equals("minecraft:the_nether")) {
            return dimensionId.equals("minecraft:the_end") ? 3 : 0;
         } else {
            return this.minecraft.player != null && this.minecraft.player.getY() >= (double)127.0F ? 1 : 2;
         }
      } else {
         return 0;
      }
   }

   protected String colorCodes(String text) {
      return text == null ? null : text.replaceAll("&(?=[0-9a-fk-orA-FK-OR])", "§");
   }

   protected String uncolorCodes(String text) {
      return text == null ? null : text.replace('§', '&');
   }

   private String parentName() {
      if (this.selectedParentId == null) {
         return Component.translatable("advwp.parent.none").getString();
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return this.selectedParentId.toString();
         } else {
            AdvancementNode advancementNode = mc.player.connection.getAdvancements().getTree().get(this.selectedParentId);
            return advancementNode != null && advancementNode.holder().value().display().isPresent() ? ((DisplayInfo)advancementNode.holder().value().display().get()).getTitle().getString() : this.selectedParentId.toString();
         }
      }
   }

   private void openParentPicker() {
      this.minecraft.gui.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this));
      Screen var2 = this.minecraft.gui.screen();
      if (var2 instanceof IAdvancementScreenCustom customScreen) {
         customScreen.advWaypoint_setParentScreen(this);
         customScreen.advWaypoint_setSelectMode(this::onParentSelected);
      }

   }

   private String shortBgName(String fullPath) {
      if (fullPath == null) {
         return "?";
      } else {
         int lastSlash = fullPath.lastIndexOf(47);
         String name = lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
         if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
         }

         return name;
      }
   }

   private EditBox addBox(int x, int y, int width, String hintKey, String value) {
      EditBox editBox = new EditBox(this.font, x, y, width, 18, Component.literal(""));
      editBox.setMaxLength(256);
      editBox.setHint(Component.translatable(hintKey));
      editBox.setValue(value != null ? this.uncolorCodes(value) : "");
      return (EditBox)this.addRenderableWidget(editBox);
   }

   private EditBox addCoord(int x, int y, String hint) {
      EditBox coordBox = new EditBox(this.font, x, y, 52, 18, Component.literal(""));
      coordBox.setMaxLength(10);
      coordBox.setHint(Component.literal(hint));
      coordBox.setResponder((s) -> {
         String filtered = s.replaceAll("[^-\\d]", "");
         if (!filtered.equals(s)) {
            coordBox.setValue(filtered);
         }

      });
      return (EditBox)this.addRenderableWidget(coordBox);
   }

   public void onClose() {
      if (this.onCloseAction != null) {
         this.onCloseAction.run();
      } else if (this.minecraft != null && this.minecraft.player != null) {
         this.minecraft.gui.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), (Screen)null));
      } else if (this.minecraft != null) {
         this.minecraft.gui.setScreen((Screen)null);
      }

   }

   protected static class CoordRow {
      int dim;
      String sx = "";
      String sy = "";
      String sz = "";
      EditBox bx;
      EditBox by;
      EditBox bz;

      CoordRow(int dim) {
         this.dim = dim;
      }

      CoordRow(int dim, String x, String y, String z) {
         this.dim = dim;
         this.sx = x;
         this.sy = y;
         this.sz = z;
      }
   }
}
