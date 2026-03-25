package com.listraind.advancementwaypoints.gui;

import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.Command;
import com.listraind.advancementwaypoints.advancementMixinHelpers.IAdvancementScreenCustom;
import com.listraind.advancementwaypoints.config.jsonGenerator;
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



public class WaypointScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");

    private static final int LABEL_COLOR = 0xFF222222;
    private static final int FIELD_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 14;
    private static final int TITLE_GAP = 6;
    private static final int COORD_FIELD_WIDTH = 52;
    private static final int GAP = 4;
    private static final int FILL_BUTTON_WIDTH = 60;

    private static final String[] DIMENSION_LABELS = {"Верхний мир", "Крыша ада", "Ад", "Энд"};
    private static final String[] DIMENSION_KEYS = {
            "minecraft:overworld", "minecraft:the_nether", "minecraft:the_nether", "minecraft:the_end"
    };

    private Item selectedIcon = Items.GRASS_BLOCK;
    private String selectedParent = "";
    private ResourceLocation selectedParentId;

    private String savedName = "";
    private String savedDescription = "";
    private String[] savedOverworld = {"", "", ""};
    private String[] savedNetherRoof = {"", "", ""};
    private String[] savedNether = {"", "", ""};
    private String[] savedEnd = {"", "", ""};

    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox[][] coordFields;
    private Button iconButton;
    private Button parentButton;

    private int panelX, panelY, panelW, panelH;

    public WaypointScreen() {
        super(Component.literal("Создание вейпоинта"));
    }

    @Override
    protected void init() {
        int contentMinHeight = TITLE_HEIGHT + TITLE_GAP + FIELD_HEIGHT * 3 + 4 * 30 + FIELD_HEIGHT + BUTTON_HEIGHT + 40;

        panelW = Math.min(width - 20, 340);
        panelH = Math.min(height - 10, Math.max(contentMinHeight, 350));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int centerX = panelX + panelW / 2;
        int fieldWidth = panelW - 100;
        int fieldLeft = centerX - fieldWidth / 2;

        int topPadding = 8;
        int bottomPadding = 8;
        int currentY = panelY + topPadding + TITLE_HEIGHT + TITLE_GAP;

        int createButtonY = panelY + panelH - bottomPadding - BUTTON_HEIGHT;
        int remainingHeight = createButtonY - currentY - GAP;

        int minSingleStep = 20;
        int minGroupStep = 32;
        int minTotalHeight = minSingleStep * 4 + minGroupStep * 4;

        float scale = (float) remainingHeight / minTotalHeight;
        int singleStep = Math.max(minSingleStep, (int) (minSingleStep * scale));
        int groupStep = Math.max(minGroupStep, (int) (minGroupStep * scale));

        nameField = addEditBox(fieldLeft, currentY, fieldWidth, "Название...", savedName);
        currentY += singleStep;

        iconButton = addRenderableWidget(Button.builder(
                Component.literal("Иконка: " + getIconId()),
                button -> minecraft.setScreen(new ItemPickerScreen(this, item -> selectedIcon = item))
        ).bounds(fieldLeft, currentY, fieldWidth, FIELD_HEIGHT).build());
        currentY += singleStep;

        getParentTitle();
        parentButton = addRenderableWidget(Button.builder(
                Component.literal("Parent: " + (selectedParent.isEmpty() ? "нет" : selectedParent)),
                button -> {
                    AdvancementsScreen advScreen = new AdvancementsScreen(
                            minecraft.player.connection.getAdvancements(),
                            this
                    );
                    minecraft.setScreen(advScreen);
                    ((IAdvancementScreenCustom) advScreen).advWaypoint_setSelectModeStringToWrite(id -> selectedParentId = id);


                }

        ).bounds(fieldLeft, currentY, fieldWidth, FIELD_HEIGHT).build());
        currentY += singleStep;

        int coordsWidth = COORD_FIELD_WIDTH * 3 + GAP * 2;
        int rowLeft = centerX - (coordsWidth + GAP + FILL_BUTTON_WIDTH) / 2;
        int fillButtonLeft = rowLeft + coordsWidth + GAP;

        String currentDimension = getCurrentDimension();
        coordFields = new EditBox[4][3];

        for (int i = 0; i < 4; i++) {
            int fieldY = currentY + 12;

            coordFields[i][0] = addCoordField(rowLeft, fieldY, "X");
            coordFields[i][1] = addCoordField(rowLeft + COORD_FIELD_WIDTH + GAP, fieldY, "Y");
            coordFields[i][2] = addCoordField(rowLeft + 2 * (COORD_FIELD_WIDTH + GAP), fieldY, "Z");

            boolean isCurrentDimension = currentDimension.equals(DIMENSION_KEYS[i]);
            if (i == 1 && currentDimension.equals("minecraft:the_nether")) {
                isCurrentDimension = true;
            }

            final int dimensionIndex = i;
            Button fillButton = addRenderableWidget(Button.builder(
                    Component.literal("Текущие"),
                    button -> fillWithPlayerCoords(coordFields[dimensionIndex])
            ).bounds(fillButtonLeft, fieldY, FILL_BUTTON_WIDTH, FIELD_HEIGHT).build());
            fillButton.active = isCurrentDimension;

            currentY += groupStep;
        }

        restoreCoords(coordFields[0], savedOverworld);
        restoreCoords(coordFields[1], savedNetherRoof);
        restoreCoords(coordFields[2], savedNether);
        restoreCoords(coordFields[3], savedEnd);

        descriptionField = addEditBox(fieldLeft, currentY, fieldWidth, "Доп. описание...", savedDescription);
        descriptionField.setMaxLength(512);

        addRenderableWidget(Button.builder(Component.literal("§aСоздать"), button -> {
            jsonGenerator.generateAndSave(
                    nameField.getValue().trim(), coordFields, getIconId(),
                    selectedParentId, descriptionField.getValue().trim()
            );
            assert minecraft != null;
            minecraft.setScreen(null);
        }).bounds(centerX - 50, createButtonY, 100, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, panelX, panelY, 0.0F, 0.0F, panelW, panelH, panelW, panelH);

        int centerX = panelX + panelW / 2;
        int totalRowWidth = COORD_FIELD_WIDTH * 3 + GAP * 2 + GAP + FILL_BUTTON_WIDTH;
        int rowLeft = centerX - totalRowWidth / 2;

        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(font, title, centerX - font.width(title) / 2, panelY + 8, LABEL_COLOR, false);

        iconButton.setMessage(Component.literal("Иконка: " + getIconId()));
        graphics.renderItem(new ItemStack(selectedIcon), iconButton.getX() - 20, iconButton.getY() + 1);

        for (int i = 0; i < DIMENSION_LABELS.length; i++) {
            int labelY = coordFields[i][0].getY() - 11;
            graphics.drawString(font, DIMENSION_LABELS[i], rowLeft, labelY, LABEL_COLOR, false);
        }
    }

    @Override
    public void removed() {
        savedName = getFieldValue(nameField);
        savedDescription = getFieldValue(descriptionField);
        savedOverworld = extractCoords(coordFields[0]);
        savedNetherRoof = extractCoords(coordFields[1]);
        savedNether = extractCoords(coordFields[2]);
        savedEnd = extractCoords(coordFields[3]);
    }


    private EditBox addEditBox(int x, int y, int width, String hint, String initialValue) {
        EditBox box = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.literal(""));
        box.setMaxLength(128);
        box.setHint(Component.literal(hint));
        box.setValue(initialValue);
        return addRenderableWidget(box);
    }

    private EditBox addCoordField(int x, int y, String hint) {
        EditBox box = new EditBox(font, x, y, COORD_FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
        box.setMaxLength(10);
        box.setHint(Component.literal(hint));
        return addRenderableWidget(box);
    }

    private void restoreCoords(EditBox[] fields, String[] saved) {
        fields[0].setValue(saved[0]);
        fields[1].setValue(saved[1]);
        fields[2].setValue(saved[2]);
    }

    private String[] extractCoords(EditBox[] fields) {
        return new String[]{
                getFieldValue(fields[0]),
                getFieldValue(fields[1]),
                getFieldValue(fields[2])
        };
    }

    private String getFieldValue(EditBox box) {
        return box != null ? box.getValue() : "";
    }

    private String getCurrentDimension() {
        if (minecraft == null || minecraft.level == null) return "";
        return minecraft.level.dimension().location().toString();
    }

    private void fillWithPlayerCoords(EditBox[] fields) {
        if (minecraft == null || minecraft.player == null) return;
        fields[0].setValue(String.valueOf((int) minecraft.player.getX()));
        fields[1].setValue(String.valueOf((int) minecraft.player.getY()));
        fields[2].setValue(String.valueOf((int) minecraft.player.getZ()));
    }

    private String getIconId() {
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(selectedIcon);
        return location != null ? location.toString() : "minecraft:stone";
    }

    private void getParentTitle(){
        if(this.selectedParentId == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientAdvancements manager = mc.player.connection.getAdvancements();
        AdvancementTree tree = manager.getTree();
        AdvancementNode node = tree.get(selectedParentId);

        if (node != null) {
            node.holder().value().display().ifPresent(display -> {
                String title = display.getTitle().getString();
                this.selectedParent = title;
                String desc = display.getDescription().getString();
                ItemStack icon = display.getIcon();
                float x = display.getX();
                float y = display.getY();

                AdvancementWaypoints.LOGGER.info("{}, {}", x, y);
            });

        }
    }



}