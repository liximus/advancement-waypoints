package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.api.IAdvancementScreenCustom;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.listraind.advancementwaypoints.DarkModeChecker;

import java.util.ArrayList;
import java.util.List;

public abstract class WaypointFormScreen extends Screen {

    protected static ResourceLocation BG = DarkModeChecker.isDarkModeEnabled() ? ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png") : ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    protected static final int FIELD_HEIGHT = 18, BUTTON_HEIGHT = 20, COORD_FIELD_WIDTH = 52, GAP = 4, FILL_BUTTON_WIDTH = 60;

    protected Item selectedIcon = Items.GRASS_BLOCK;
    protected ResourceLocation selectedParentId;
    protected String savedName = "";
    protected String savedDesc = "";
    protected String savedBackground = "";
    protected List<CoordRow> coordRows = new ArrayList<>();
    protected boolean isVanilla = false;
    protected boolean hadParentBefore = false;

    protected EditBox nameField, descField;
    protected Button iconButton, parentButton, bgButton;
    protected float scale = 1f;
    protected int virtualWidth, virtualHeight, panelX, panelY, panelWidth, panelHeight;
    protected int separator1Y, separator2Y;

    public static void setDarkMode(boolean darkMode) {
        BG = darkMode ? ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackgrounddark.png") : ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    }

    protected static class CoordRow {
        int dim;
        String sx = "", sy = "", sz = "";
        EditBox bx, by, bz;
        CoordRow(int dim) { this.dim = dim; }
        CoordRow(int dim, String x, String y, String z) { this.dim = dim; sx = x; sy = y; sz = z; }
    }

    protected WaypointFormScreen(Component title) {
        super(title);
    }

    protected boolean isRoot() {
        return selectedParentId == null && !isVanilla;
    }

    @Override
    protected void init() {
        int padding = 12, gap = 4, coordRowHeight = 36, coordRowGap = 10;

        boolean root = isRoot();

        int totalContentHeight = padding + 14 + gap + FIELD_HEIGHT + gap + FIELD_HEIGHT + gap + FIELD_HEIGHT + gap;
        if (root) totalContentHeight += FIELD_HEIGHT + gap;
        totalContentHeight += FIELD_HEIGHT + 13;
        if (!root && !coordRows.isEmpty()) totalContentHeight += coordRows.size() * (coordRowHeight + coordRowGap);
        if (!root) totalContentHeight += BUTTON_HEIGHT + 13;
        totalContentHeight += BUTTON_HEIGHT + padding;

        scale = Math.min(1f, (float)(height - 10) / totalContentHeight);
        virtualWidth = (int)(width / scale);
        virtualHeight = (int)(height / scale);
        panelWidth = Math.min(virtualWidth - 20, 340);
        panelHeight = totalContentHeight;
        panelX = (virtualWidth - panelWidth) / 2;
        panelY = (virtualHeight - panelHeight) / 2;

        int centerX = panelX + panelWidth / 2;
        int fieldWidth = panelWidth - 40;
        int fieldLeft = centerX - fieldWidth / 2;
        int currentY = panelY + padding + 14 + gap;

        nameField = addBox(fieldLeft, currentY, fieldWidth, "Название...", savedName);
        currentY += FIELD_HEIGHT + gap;

        int buttonWidth = fieldWidth - 25;
        iconButton = addRenderableWidget(Button.builder(Component.literal("Иконка: " + iconId()), b -> {
            saveState();
            setFocused(null);
            minecraft.setScreen(new ItemPickerScreen(this, item -> selectedIcon = item));
        }).bounds(fieldLeft, currentY, buttonWidth, FIELD_HEIGHT).build());
        currentY += FIELD_HEIGHT + gap;

        String parentDisplayName = selectedParentId != null ? parentName() : "нет§3(вкладка)";
        parentButton = addRenderableWidget(Button.builder(Component.literal("Parent: " + parentDisplayName), b -> {
            saveState();
            setFocused(null);
            hadParentBefore = selectedParentId != null;
            openParentPicker();
        }).bounds(fieldLeft, currentY, buttonWidth, FIELD_HEIGHT).build());
        parentButton.active = !isVanilla;

        Button resetParentButton = addRenderableWidget(Button.builder(Component.literal("X"), b -> {
            selectedParentId = null;
            parentButton.setMessage(Component.literal("Parent: нет§3(вкладка)"));
            saveState();
            rebuildWidgets();
        }).bounds(fieldLeft + buttonWidth + 5, currentY, 20, FIELD_HEIGHT).build());
        resetParentButton.active = !isVanilla;
        currentY += FIELD_HEIGHT + gap;

        if (root) {
            String backgroundLabel = savedBackground != null && !savedBackground.isEmpty() ? shortBgName(savedBackground) : " stone(по умолч., не выбран)";
            bgButton = addRenderableWidget(Button.builder(Component.literal("Фон: " + backgroundLabel), b -> {
                saveState();
                setFocused(null);
                minecraft.setScreen(new ItemPickerScreen(this, item -> {
                    ResourceLocation blockId = BuiltInRegistries.ITEM.getKey(item);
                    if (blockId != null) {
                        savedBackground = blockId.getNamespace() + ":block/" + blockId.getPath();
                    }
                }, true));
            }).bounds(fieldLeft, currentY, fieldWidth, FIELD_HEIGHT).build());
            currentY += FIELD_HEIGHT + gap;
        } else {
            bgButton = null;
        }

        descField = addBox(fieldLeft, currentY, fieldWidth, "Описание...", savedDesc);
        descField.setMaxLength(512);
        currentY += FIELD_HEIGHT;

        currentY += 6;
        separator1Y = currentY;
        currentY += 7;

        if (!root) {
            int coordRowTotalWidth = COORD_FIELD_WIDTH * 3 + GAP * 2;
            int coordRowLeft = centerX - (coordRowTotalWidth + GAP + FILL_BUTTON_WIDTH) / 2;

            for (int i = 0; i < coordRows.size(); i++) {
                CoordRow coordRow = coordRows.get(i);

                addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> {
                    saveState();
                    coordRows.remove(coordRow);
                    setFocused(null);
                    rebuildWidgets();
                }).bounds(coordRowLeft + coordRowTotalWidth + GAP, currentY, FILL_BUTTON_WIDTH, 16).build());

                int coordFieldY = currentY + 18;
                coordRow.bx = addCoord(coordRowLeft, coordFieldY, "X"); coordRow.bx.setValue(coordRow.sx);
                coordRow.by = addCoord(coordRowLeft + COORD_FIELD_WIDTH + GAP, coordFieldY, "Y"); coordRow.by.setValue(coordRow.sy);
                coordRow.bz = addCoord(coordRowLeft + 2 * (COORD_FIELD_WIDTH + GAP), coordFieldY, "Z"); coordRow.bz.setValue(coordRow.sz);

                boolean isCurrentDimension = (coordRow.dim == currentDim());
                Button fillCoordsButton = addRenderableWidget(Button.builder(Component.literal("Текущие"), b -> {
                    if (minecraft.player == null) return;
                    coordRow.bx.setValue(String.valueOf((int) minecraft.player.getX()));
                    coordRow.by.setValue(String.valueOf((int) minecraft.player.getY()));
                    coordRow.bz.setValue(String.valueOf((int) minecraft.player.getZ()));
                }).bounds(coordRowLeft + coordRowTotalWidth + GAP, coordFieldY - 2, FILL_BUTTON_WIDTH, 20).build());
                fillCoordsButton.active = isCurrentDimension;

                currentY += coordRowHeight + coordRowGap;
            }

            Button addCoordsButton = addRenderableWidget(Button.builder(Component.literal("Добавить координаты"), b -> {
                saveState();
                setFocused(null);
                minecraft.setScreen(new DimensionPickerScreen(this));
            }).bounds(fieldLeft, currentY, fieldWidth, BUTTON_HEIGHT).build());
            addCoordsButton.active = !isVanilla;
            currentY += BUTTON_HEIGHT;

            currentY += 6;
            separator2Y = currentY;
            currentY += 7;
        } else {
            separator2Y = separator1Y;
        }

        initActions(centerX, currentY);
    }

    protected abstract void initActions(int centerX, int currentY);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return super.mouseClicked(mouseX / scale, mouseY / scale, button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return super.mouseReleased(mouseX / scale, mouseY / scale, button); }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return super.mouseDragged(mouseX / scale, mouseY / scale, button, deltaX / scale, deltaY / scale); }
    @Override
    public void mouseMoved(double mouseX, double mouseY) { super.mouseMoved(mouseX / scale, mouseY / scale); }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return super.mouseScrolled(mouseX / scale, mouseY / scale, scrollX, scrollY); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        int scaledMouseX = (int)(mouseX / scale), scaledMouseY = (int)(mouseY / scale);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, panelX, panelY, 0f, 0f, panelWidth, panelHeight, panelWidth, panelHeight);
        super.render(graphics, scaledMouseX, scaledMouseY, delta);

        graphics.drawString(font, title, panelX + panelWidth / 2 - font.width(title) / 2, panelY + 8, 0xFF222222, false);
        iconButton.setMessage(Component.literal("Иконка: " + iconId()));
        if (bgButton != null) {
            String backgroundLabel = savedBackground != null && !savedBackground.isEmpty() ? shortBgName(savedBackground) : "stone(по умолч., не выбран)";
            bgButton.setMessage(Component.literal("Фон: " + backgroundLabel));
        }
        graphics.renderItem(new ItemStack(selectedIcon), iconButton.getX() + iconButton.getWidth() + 7, iconButton.getY() + 1);
        graphics.fill(panelX + 15, separator1Y, panelX + panelWidth - 15, separator1Y + 1, 0xFF777777);
        if (separator2Y != separator1Y) {
            graphics.fill(panelX + 15, separator2Y, panelX + panelWidth - 15, separator2Y + 1, 0xFF777777);
        }

        if (!isRoot()) {
            int coordRowTotalWidth = COORD_FIELD_WIDTH * 3 + GAP * 2;
            int coordRowLeft = panelX + panelWidth / 2 - (coordRowTotalWidth + GAP + FILL_BUTTON_WIDTH) / 2;
            for (CoordRow coordRow : coordRows) {
                if (coordRow.bx != null) {
                    graphics.drawString(font, CoordParser.DIM_LABELS[coordRow.dim], coordRowLeft, coordRow.bx.getY() - 11, 0xFFFFFFFF, false);
                }
            }
        }
        graphics.pose().popMatrix();
    }

    @Override
    public void removed() { saveState(); }

    public void addDimRow(int dim) {
        saveState();
        CoordRow newRow = new CoordRow(dim);
        if (dim == currentDim() && minecraft != null && minecraft.player != null) {
            newRow.sx = String.valueOf((int) minecraft.player.getX());
            newRow.sy = String.valueOf((int) minecraft.player.getY());
            newRow.sz = String.valueOf((int) minecraft.player.getZ());
        }
        coordRows.add(newRow);
    }

    public void onParentSelected(ResourceLocation newParent) {
        selectedParentId = newParent;
        if (!hadParentBefore && coordRows.isEmpty()) {
            CoordRow newRow = new CoordRow(currentDim());
            if (minecraft != null && minecraft.player != null) {
                newRow.sx = String.valueOf((int) minecraft.player.getX());
                newRow.sy = String.valueOf((int) minecraft.player.getY());
                newRow.sz = String.valueOf((int) minecraft.player.getZ());
            }
            coordRows.add(newRow);
        }
    }

    protected void saveState() {
        if (nameField != null) savedName = nameField.getValue();
        if (descField != null) savedDesc = descField.getValue();
        for (CoordRow coordRow : coordRows) {
            if (coordRow.bx != null) coordRow.sx = coordRow.bx.getValue();
            if (coordRow.by != null) coordRow.sy = coordRow.by.getValue();
            if (coordRow.bz != null) coordRow.sz = coordRow.bz.getValue();
        }
    }

    protected List<CoordParser.DimCoords> collectCoords() {
        List<List<String[]>> coordsPerDimension = new ArrayList<>();
        for (int i = 0; i < 4; i++) coordsPerDimension.add(new ArrayList<>());

        for (CoordRow coordRow : coordRows) {
            String x = coordRow.bx != null ? coordRow.bx.getValue().trim() : coordRow.sx.trim();
            String y = coordRow.by != null ? coordRow.by.getValue().trim() : coordRow.sy.trim();
            String z = coordRow.bz != null ? coordRow.bz.getValue().trim() : coordRow.sz.trim();
            if (!x.isEmpty() || !y.isEmpty() || !z.isEmpty()) {
                coordsPerDimension.get(coordRow.dim).add(new String[]{
                        x.isEmpty() ? "0" : x,
                        y.isEmpty() ? "0" : y,
                        z.isEmpty() ? "0" : z
                });
            }
        }

        List<CoordParser.DimCoords> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!coordsPerDimension.get(i).isEmpty()) result.add(new CoordParser.DimCoords(i, coordsPerDimension.get(i)));
        }
        return result;
    }

    protected String buildFinalDescription() {
        String description = isRoot() ? (descField != null ? descField.getValue().trim() : savedDesc)
                : CoordParser.buildDescription(selectedParentId != null ? collectCoords() : new ArrayList<>(), descField != null ? descField.getValue().trim() : savedDesc);
        return colorCodes(description);
    }

    protected String getBackgroundValue() {
        if (savedBackground != null && !savedBackground.isEmpty()) return savedBackground;
        return BuiltInRegistries.ITEM.getKey(Items.STONE).getNamespace() + ":block/" + BuiltInRegistries.ITEM.getKey(Items.STONE).getPath();
    }

    protected String iconId() {
        ResourceLocation iconLocation = BuiltInRegistries.ITEM.getKey(selectedIcon);
        return iconLocation.toString();
    }

    protected int currentDim() {
        if (minecraft == null || minecraft.level == null) return 0;
        String dimensionId = minecraft.level.dimension().location().toString();
        if (dimensionId.equals("minecraft:the_nether")) {
            return (minecraft.player != null && minecraft.player.getY() >= 127) ? 1 : 2;
        }
        if (dimensionId.equals("minecraft:the_end")) return 3;
        return 0;
    }

    protected String colorCodes(String text) {
        return text != null ? text.replace('&', '§') : text;
    }

    private String parentName() {
        if (selectedParentId == null) return "нет";
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return selectedParentId.toString();
        AdvancementNode advancementNode = mc.player.connection.getAdvancements().getTree().get(selectedParentId);
        if (advancementNode != null && advancementNode.holder().value().display().isPresent()) {
            return advancementNode.holder().value().display().get().getTitle().getString();
        }
        return selectedParentId.toString();
    }

    private void openParentPicker() {
        minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this));

        if (minecraft.screen instanceof IAdvancementScreenCustom customScreen) {
            customScreen.advWaypoint_setParentScreen(this);
            customScreen.advWaypoint_setSelectMode(this::onParentSelected);
        }
    }

    private String shortBgName(String fullPath) {
        if (fullPath == null) return "?";
        int lastSlash = fullPath.lastIndexOf('/');
        String name = lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
        if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
        return name;
    }

    private EditBox addBox(int x, int y, int width, String hint, String value) {
        EditBox editBox = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.literal(""));
        editBox.setMaxLength(256);
        editBox.setHint(Component.literal(hint));
        editBox.setValue(value != null ? value : "");
        return addRenderableWidget(editBox);
    }

    private EditBox addCoord(int x, int y, String hint) {
        EditBox coordBox = new EditBox(font, x, y, COORD_FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
        coordBox.setMaxLength(10);
        coordBox.setHint(Component.literal(hint));
        return addRenderableWidget(coordBox);
    }
}