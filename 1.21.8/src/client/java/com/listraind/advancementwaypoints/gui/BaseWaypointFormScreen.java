package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancementMixinHelpers.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.WaypointManager;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseWaypointFormScreen extends Screen implements IWaypointScreen {

    protected static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");

    protected static final int LABEL_COLOR = 0xFF222222;
    protected static final int FIELD_HEIGHT = 18;
    protected static final int BUTTON_HEIGHT = 20;
    protected static final int TITLE_HEIGHT = 14;
    protected static final int COORD_FIELD_WIDTH = 52;
    protected static final int GAP = 4;
    protected static final int FILL_BUTTON_WIDTH = 60;

    public static final String[] DIMENSION_LABELS = {"Верхний мир", "Крыша ада", "Ад", "Энд"};
    protected static final String[] DIMENSION_KEYS = {
            "minecraft:overworld", "minecraft:the_nether", "minecraft:the_nether", "minecraft:the_end"
    };

    protected Item selectedIcon = Items.GRASS_BLOCK;
    protected String selectedParent = "";
    protected ResourceLocation selectedParentId;

    protected String savedName = "";
    protected String savedDescription = "";

    protected static class CoordData {
        public int dim;
        public String sx = "", sy = "", sz = "";
        public EditBox bx, by, bz;
        public CoordData(int dim) { this.dim = dim; }
        public CoordData(int dim, String x, String y, String z) {
            this.dim = dim; this.sx = x; this.sy = y; this.sz = z;
        }
    }

    protected List<CoordData> activeCoords = new ArrayList<>();
    protected boolean isVanilla = false;

    protected EditBox nameField;
    protected EditBox descriptionField;
    protected EditBox[][] coordFields;
    protected Button iconButton;
    protected Button parentButton;

    protected float uiScale = 1.0f;
    protected int vWidth, vHeight;
    protected int panelX, panelY, panelW, panelH;
    protected int separator1Y = 0;
    protected int separator2Y = 0;

    protected BaseWaypointFormScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        int padding = 12;
        int gap = 4;
        int rowHeight = 36;
        int rowGap = 10;

        int contentHeight = padding + TITLE_HEIGHT + gap;
        contentHeight += FIELD_HEIGHT + gap;
        contentHeight += FIELD_HEIGHT + gap;
        contentHeight += FIELD_HEIGHT + gap;
        contentHeight += FIELD_HEIGHT;
        contentHeight += 13;

        if (!activeCoords.isEmpty()) {
            contentHeight += activeCoords.size() * (rowHeight + rowGap);
        }

        contentHeight += BUTTON_HEIGHT;
        contentHeight += 13;
        contentHeight += BUTTON_HEIGHT + padding;

        uiScale = Math.min(1.0f, (float)(height - 10) / contentHeight);
        vWidth = (int) (width / uiScale);
        vHeight = (int) (height / uiScale);

        panelW = Math.min(vWidth - 20, 340);
        panelH = contentHeight;
        panelX = (vWidth - panelW) / 2;
        panelY = (vHeight - panelH) / 2;

        int centerX = panelX + panelW / 2;
        int fieldWidth = panelW - 40;
        int fieldLeft = centerX - fieldWidth / 2;

        int currentY = panelY + padding + TITLE_HEIGHT + gap;

        nameField = addEditBox(fieldLeft, currentY, fieldWidth, "Название...", savedName);
        currentY += FIELD_HEIGHT + gap;

        int pWidth = fieldWidth - 25;

        iconButton = addRenderableWidget(Button.builder(
                Component.literal("Иконка: " + getIconId()),
                button -> {
                    saveCurrentState();
                    this.setFocused(null);
                    openItemPicker(item -> selectedIcon = item);
                }
        ).bounds(fieldLeft, currentY, pWidth, FIELD_HEIGHT).build());
        currentY += FIELD_HEIGHT + gap;

        if (selectedParentId != null && selectedParent.isEmpty()) {
            getParentTitle();
        }

        parentButton = addRenderableWidget(Button.builder(
                Component.literal("Parent: " + (selectedParent.isEmpty() ? "нет" : selectedParent)),
                button -> {
                    saveCurrentState();
                    this.setFocused(null);
                    openAdvancementPicker();
                }
        ).bounds(fieldLeft, currentY, pWidth, FIELD_HEIGHT).build());
        parentButton.active = !isVanilla;

        Button resetBtn = addRenderableWidget(Button.builder(
                Component.literal("X"),
                button -> {
                    selectedParentId = null;
                    selectedParent = "";
                    parentButton.setMessage(Component.literal("Parent: нет"));
                }
        ).bounds(fieldLeft + pWidth + 5, currentY, 20, FIELD_HEIGHT).build());
        resetBtn.active = !isVanilla;
        currentY += FIELD_HEIGHT + gap;

        descriptionField = addEditBox(fieldLeft, currentY, fieldWidth, "Описание...", savedDescription);
        descriptionField.setMaxLength(512);
        currentY += FIELD_HEIGHT;

        currentY += 6;
        separator1Y = currentY;
        currentY += 7;

        int coordsWidth = COORD_FIELD_WIDTH * 3 + GAP * 2;
        int rowLeft = centerX - (coordsWidth + GAP + FILL_BUTTON_WIDTH) / 2;

        coordFields = new EditBox[activeCoords.size()][3];

        for (int i = 0; i < activeCoords.size(); i++) {
            CoordData cd = activeCoords.get(i);

            addRenderableWidget(Button.builder(
                    Component.literal("Удалить"),
                    button -> removeDimensionRow(cd)
            ).bounds(rowLeft + coordsWidth + GAP, currentY, FILL_BUTTON_WIDTH, 16).build());

            int boxY = currentY + 18;

            cd.bx = addCoordField(rowLeft, boxY, "X");
            cd.bx.setValue(cd.sx != null ? cd.sx : "");
            cd.by = addCoordField(rowLeft + COORD_FIELD_WIDTH + GAP, boxY, "Y");
            cd.by.setValue(cd.sy != null ? cd.sy : "");
            cd.bz = addCoordField(rowLeft + 2 * (COORD_FIELD_WIDTH + GAP), boxY, "Z");
            cd.bz.setValue(cd.sz != null ? cd.sz : "");

            coordFields[i][0] = cd.bx;
            coordFields[i][1] = cd.by;
            coordFields[i][2] = cd.bz;

            boolean isCurrent = (cd.dim == getCurrentDimIndex());
            Button fillButton = addRenderableWidget(Button.builder(
                    Component.literal("Текущие"),
                    button -> fillWithPlayerCoords(new EditBox[]{cd.bx, cd.by, cd.bz})
            ).bounds(rowLeft + coordsWidth + GAP, boxY - 2, FILL_BUTTON_WIDTH, 20).build());
            fillButton.active = isCurrent;

            currentY += rowHeight;
            currentY += rowGap;
        }

        addRenderableWidget(Button.builder(
                Component.literal("Добавить"),
                button -> {
                    saveCurrentState();
                    this.setFocused(null);
                    minecraft.setScreen(new DimensionPickerScreen(this));
                }
        ).bounds(fieldLeft, currentY, fieldWidth, BUTTON_HEIGHT).build());
        currentY += BUTTON_HEIGHT;

        currentY += 6;
        separator2Y = currentY;
        currentY += 7;

        initActionButtons(centerX, currentY);
    }

    protected abstract void initActionButtons(int centerX, int buttonsY);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / uiScale, mouseY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / uiScale, mouseY / uiScale, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(uiScale, uiScale);

        int smX = (int)(mouseX / uiScale);
        int smY = (int)(mouseY / uiScale);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, panelX, panelY, 0.0F, 0.0F, panelW, panelH, panelW, panelH);

        int centerX = panelX + panelW / 2;
        int totalRowWidth = COORD_FIELD_WIDTH * 3 + GAP * 2 + GAP + FILL_BUTTON_WIDTH;
        int rowLeft = centerX - totalRowWidth / 2;

        super.render(graphics, smX, smY, delta);

        graphics.drawString(font, title, centerX - font.width(title) / 2, panelY + 8, LABEL_COLOR, false);

        iconButton.setMessage(Component.literal("Иконка: " + getIconId()));
        graphics.renderItem(new ItemStack(selectedIcon), iconButton.getX() + iconButton.getWidth() + 7, iconButton.getY() + 1);

        graphics.fill(panelX + 15, separator1Y, panelX + panelW - 15, separator1Y + 1, 0xFF777777);
        graphics.fill(panelX + 15, separator2Y, panelX + panelW - 15, separator2Y + 1, 0xFF777777);

        for (int i = 0; i < activeCoords.size(); i++) {
            CoordData cd = activeCoords.get(i);
            if (cd.bx != null) {
                int labelY = cd.bx.getY() - 11;
                graphics.drawString(font, DIMENSION_LABELS[cd.dim], rowLeft, labelY, LABEL_COLOR, false);
            }
        }

        graphics.pose().popMatrix();
    }

    @Override
    public void removed() {
        saveCurrentState();
    }

    public void addDimensionRow(int dim) {
        saveCurrentState();
        CoordData cd = new CoordData(dim);
        if (dim == getCurrentDimIndex() && minecraft != null && minecraft.player != null) {
            cd.sx = String.valueOf((int) minecraft.player.getX());
            cd.sy = String.valueOf((int) minecraft.player.getY());
            cd.sz = String.valueOf((int) minecraft.player.getZ());
        }
        activeCoords.add(cd);
    }

    protected void removeDimensionRow(CoordData cd) {
        saveCurrentState();
        activeCoords.remove(cd);
        this.setFocused(null);
        rebuildWidgets();
    }

    protected void saveCurrentState() {
        savedName = nameField != null ? nameField.getValue() : savedName;
        savedDescription = descriptionField != null ? descriptionField.getValue() : savedDescription;
        for (CoordData cd : activeCoords) {
            if (cd.bx != null) cd.sx = cd.bx.getValue();
            if (cd.by != null) cd.sy = cd.by.getValue();
            if (cd.bz != null) cd.sz = cd.bz.getValue();
        }
    }

    protected EditBox addEditBox(int x, int y, int width, String hint, String initialValue) {
        EditBox box = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.literal(""));
        box.setMaxLength(128);
        box.setHint(Component.literal(hint));
        box.setValue(initialValue);
        return addRenderableWidget(box);
    }

    protected EditBox addCoordField(int x, int y, String hint) {
        EditBox box = new EditBox(font, x, y, COORD_FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
        box.setMaxLength(10);
        box.setHint(Component.literal(hint));
        return addRenderableWidget(box);
    }

    protected int getCurrentDimIndex() {
        if (minecraft == null || minecraft.level == null) return 0;
        String dim = minecraft.level.dimension().location().toString();
        if (dim.equals("minecraft:the_nether")) {
            return (minecraft.player != null && minecraft.player.getY() >= 127) ? 1 : 2;
        } else if (dim.equals("minecraft:the_end")) {
            return 3;
        }
        return 0;
    }

    protected void fillWithPlayerCoords(EditBox[] fields) {
        if (minecraft == null || minecraft.player == null) return;
        fields[0].setValue(String.valueOf((int) minecraft.player.getX()));
        fields[1].setValue(String.valueOf((int) minecraft.player.getY()));
        fields[2].setValue(String.valueOf((int) minecraft.player.getZ()));
    }

    protected String getIconId() {
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(selectedIcon);
        return location != null ? location.toString() : "minecraft:stone";
    }

    protected void getParentTitle() {
        if (this.selectedParentId == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientAdvancements manager = mc.player.connection.getAdvancements();
        AdvancementTree tree = manager.getTree();
        AdvancementNode node = tree.get(selectedParentId);

        if (node != null) {
            node.holder().value().display().ifPresent(display -> {
                this.selectedParent = display.getTitle().getString();
            });
        }
    }

    protected void openAdvancementPicker() {
        AdvancementsScreen advScreen = new AdvancementsScreen(
                minecraft.player.connection.getAdvancements(),
                this
        );
        minecraft.setScreen(advScreen);
        ((IAdvancementScreenCustom) advScreen).advWaypoint_setSelectModeStringToWrite(id -> selectedParentId = id);
    }

    public EditBox[][] getStandardCoordFields() {
        EditBox[][] standard = new EditBox[4][3];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                standard[i][j] = new EditBox(font, 0, 0, 0, 0, Component.literal(""));
                standard[i][j].setValue("");
            }
        }
        for (CoordData cd : activeCoords) {
            if (cd.dim >= 0 && cd.dim < 4) {
                standard[cd.dim][0].setValue(cd.bx != null ? cd.bx.getValue() : cd.sx);
                standard[cd.dim][1].setValue(cd.by != null ? cd.by.getValue() : cd.sy);
                standard[cd.dim][2].setValue(cd.bz != null ? cd.bz.getValue() : cd.sz);
            }
        }
        return standard;
    }

    protected String buildDescription() {
        return WaypointManager.buildDescription(getStandardCoordFields(), descriptionField.getValue().trim());
    }

    @Override
    public void openItemPicker(Consumer<Item> onItemSelected) {
        minecraft.setScreen(new ItemPickerScreen(this, onItemSelected));
    }

    @Override
    public void closeItemPicker() {
        assert minecraft != null;
        minecraft.setScreen(this);
    }

    protected String translateColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace('&', '§');
    }
}

class DimensionPickerScreen extends Screen {
    private final BaseWaypointFormScreen parent;
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    private int panelX, panelY, panelWidth, panelHeight;

    public DimensionPickerScreen(BaseWaypointFormScreen parent) {
        super(Component.literal("Выберите измерение"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int btnW = 160, btnH = 20, gap = 4;
        int panelPadding = 5;
        int contentWidth = btnW + panelPadding * 2;
        int contentHeight = 4 * btnH + 3 * gap + panelPadding * 2;
        panelWidth = contentWidth;
        panelHeight = contentHeight;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int startY = panelY + panelPadding + 20;
        for (int i = 0; i < BaseWaypointFormScreen.DIMENSION_LABELS.length; i++) {
            final int dim = i;
            addRenderableWidget(Button.builder(
                    Component.literal(BaseWaypointFormScreen.DIMENSION_LABELS[i]),
                    b -> {
                        parent.addDimensionRow(dim);
                        if (minecraft != null) minecraft.setScreen(parent);
                    }
            ).bounds(panelX + panelPadding, startY + i * (btnH + gap), btnW, btnH).build());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                panelX, panelY+20, 0.0F, 0.0F, panelWidth, panelHeight, panelWidth, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}