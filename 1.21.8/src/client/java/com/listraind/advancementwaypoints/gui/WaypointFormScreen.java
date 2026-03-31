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

import java.util.ArrayList;
import java.util.List;

public abstract class WaypointFormScreen extends Screen {

    protected static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "textures/waypointscreenbackground.png");
    protected static final int FH = 18, BH = 20, CFW = 52, GAP = 4, FILL_W = 60;

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
    protected int vw, vh, px, py, pw, ph;
    protected int sep1Y, sep2Y;

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
        int pad = 12, gap = 4, rowH = 36, rowGap = 10;

        boolean root = isRoot();

        int ch = pad + 14 + gap + FH + gap + FH + gap + FH + gap;
        if (root) ch += FH + gap;
        ch += FH + 13;
        if (!root && !coordRows.isEmpty()) ch += coordRows.size() * (rowH + rowGap);
        if (!root) ch += BH + 13;
        ch += BH + pad;

        scale = Math.min(1f, (float)(height - 10) / ch);
        vw = (int)(width / scale);
        vh = (int)(height / scale);
        pw = Math.min(vw - 20, 340);
        ph = ch;
        px = (vw - pw) / 2;
        py = (vh - ph) / 2;

        int cx = px + pw / 2;
        int fw = pw - 40;
        int fl = cx - fw / 2;
        int y = py + pad + 14 + gap;

        nameField = addBox(fl, y, fw, "Название...", savedName);
        y += FH + gap;

        int bw = fw - 25;
        iconButton = addRenderableWidget(Button.builder(Component.literal("Иконка: " + iconId()), b -> {
            saveState();
            setFocused(null);
            minecraft.setScreen(new ItemPickerScreen(this, item -> selectedIcon = item));
        }).bounds(fl, y, bw, FH).build());
        y += FH + gap;

        String pName = selectedParentId != null ? parentName() : "нет§3(вкладка)";
        parentButton = addRenderableWidget(Button.builder(Component.literal("Parent: " + pName), b -> {
            saveState();
            setFocused(null);
            hadParentBefore = selectedParentId != null;
            openParentPicker();
        }).bounds(fl, y, bw, FH).build());
        parentButton.active = !isVanilla;

        Button resetBtn = addRenderableWidget(Button.builder(Component.literal("X"), b -> {
            selectedParentId = null;
            parentButton.setMessage(Component.literal("Parent: нет§3(вкладка)"));
            saveState();
            rebuildWidgets();
        }).bounds(fl + bw + 5, y, 20, FH).build());
        resetBtn.active = !isVanilla;
        y += FH + gap;

        if (root) {
            String bgLabel = savedBackground != null && !savedBackground.isEmpty() ? shortBgName(savedBackground) : "не выбран";
            bgButton = addRenderableWidget(Button.builder(Component.literal("Фон: " + bgLabel), b -> {
                saveState();
                setFocused(null);
                minecraft.setScreen(new ItemPickerScreen(this, item -> {
                    ResourceLocation blockId = BuiltInRegistries.ITEM.getKey(item);
                    if (blockId != null) {
                        savedBackground = blockId.getNamespace() + ":block/" + blockId.getPath();
                        AdvancementWaypoints.LOGGER.info(savedBackground);
                    }
                }, true));
            }).bounds(fl, y, fw, FH).build());
            y += FH + gap;
        } else {
            bgButton = null;
        }

        descField = addBox(fl, y, fw, "Описание...", savedDesc);
        descField.setMaxLength(512);
        y += FH;

        y += 6;
        sep1Y = y;
        y += 7;

        if (!root) {
            int cw = CFW * 3 + GAP * 2;
            int rl = cx - (cw + GAP + FILL_W) / 2;

            for (int i = 0; i < coordRows.size(); i++) {
                CoordRow cr = coordRows.get(i);

                addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> {
                    saveState();
                    coordRows.remove(cr);
                    setFocused(null);
                    rebuildWidgets();
                }).bounds(rl + cw + GAP, y, FILL_W, 16).build());

                int by2 = y + 18;
                cr.bx = addCoord(rl, by2, "X"); cr.bx.setValue(cr.sx);
                cr.by = addCoord(rl + CFW + GAP, by2, "Y"); cr.by.setValue(cr.sy);
                cr.bz = addCoord(rl + 2 * (CFW + GAP), by2, "Z"); cr.bz.setValue(cr.sz);

                boolean current = (cr.dim == currentDim());
                Button fill = addRenderableWidget(Button.builder(Component.literal("Текущие"), b -> {
                    if (minecraft.player == null) return;
                    cr.bx.setValue(String.valueOf((int) minecraft.player.getX()));
                    cr.by.setValue(String.valueOf((int) minecraft.player.getY()));
                    cr.bz.setValue(String.valueOf((int) minecraft.player.getZ()));
                }).bounds(rl + cw + GAP, by2 - 2, FILL_W, 20).build());
                fill.active = current;

                y += rowH + rowGap;
            }

            Button addCoordsBtn = addRenderableWidget(Button.builder(Component.literal("Добавить координаты"), b -> {
                saveState();
                setFocused(null);
                minecraft.setScreen(new DimensionPickerScreen(this));
            }).bounds(fl, y, fw, BH).build());
            addCoordsBtn.active = !isVanilla;
            y += BH;

            y += 6;
            sep2Y = y;
            y += 7;
        } else {
            sep2Y = sep1Y;
        }

        initActions(cx, y);
    }

    protected abstract void initActions(int cx, int y);

    @Override
    public boolean mouseClicked(double mx, double my, int b) { return super.mouseClicked(mx / scale, my / scale, b); }
    @Override
    public boolean mouseReleased(double mx, double my, int b) { return super.mouseReleased(mx / scale, my / scale, b); }
    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return super.mouseDragged(mx / scale, my / scale, b, dx / scale, dy / scale); }
    @Override
    public void mouseMoved(double mx, double my) { super.mouseMoved(mx / scale, my / scale); }
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) { return super.mouseScrolled(mx / scale, my / scale, sx, sy); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);
        int smx = (int)(mx / scale), smy = (int)(my / scale);

        g.blit(RenderPipelines.GUI_TEXTURED, BG, px, py, 0f, 0f, pw, ph, pw, ph);
        super.render(g, smx, smy, d);

        g.drawString(font, title, px + pw / 2 - font.width(title) / 2, py + 8, 0xFF222222, false);
        iconButton.setMessage(Component.literal("Иконка: " + iconId()));
        if (bgButton != null) {
            String bgLabel = savedBackground != null && !savedBackground.isEmpty() ? shortBgName(savedBackground) : "не выбран";
            bgButton.setMessage(Component.literal("Фон: " + bgLabel));
        }
        g.renderItem(new ItemStack(selectedIcon), iconButton.getX() + iconButton.getWidth() + 7, iconButton.getY() + 1);
        g.fill(px + 15, sep1Y, px + pw - 15, sep1Y + 1, 0xFF777777);
        if (sep2Y != sep1Y) {
            g.fill(px + 15, sep2Y, px + pw - 15, sep2Y + 1, 0xFF777777);
        }

        if (!isRoot()) {
            int cw = CFW * 3 + GAP * 2;
            int rl = px + pw / 2 - (cw + GAP + FILL_W) / 2;
            for (CoordRow cr : coordRows) {
                if (cr.bx != null) {
                    g.drawString(font, CoordParser.DIM_LABELS[cr.dim], rl, cr.bx.getY() - 11, 0xFFFFFFFF, false);
                }
            }
        }
        g.pose().popMatrix();
    }

    @Override
    public void removed() { saveState(); }

    public void addDimRow(int dim) {
        saveState();
        CoordRow cr = new CoordRow(dim);
        if (dim == currentDim() && minecraft != null && minecraft.player != null) {
            cr.sx = String.valueOf((int) minecraft.player.getX());
            cr.sy = String.valueOf((int) minecraft.player.getY());
            cr.sz = String.valueOf((int) minecraft.player.getZ());
        }
        coordRows.add(cr);
    }

    public void onParentSelected(ResourceLocation newParent) {
        selectedParentId = newParent;
        if (!hadParentBefore && coordRows.isEmpty()) {
            CoordRow cr = new CoordRow(currentDim());
            if (minecraft != null && minecraft.player != null) {
                cr.sx = String.valueOf((int) minecraft.player.getX());
                cr.sy = String.valueOf((int) minecraft.player.getY());
                cr.sz = String.valueOf((int) minecraft.player.getZ());
            }
            coordRows.add(cr);
        }
    }

    protected void saveState() {
        if (nameField != null) savedName = nameField.getValue();
        if (descField != null) savedDesc = descField.getValue();
        for (CoordRow cr : coordRows) {
            if (cr.bx != null) cr.sx = cr.bx.getValue();
            if (cr.by != null) cr.sy = cr.by.getValue();
            if (cr.bz != null) cr.sz = cr.bz.getValue();
        }
    }

    protected List<CoordParser.DimCoords> collectCoords() {
        List<List<String[]>> perDim = new ArrayList<>();
        for (int i = 0; i < 4; i++) perDim.add(new ArrayList<>());

        for (CoordRow cr : coordRows) {
            String x = cr.bx != null ? cr.bx.getValue().trim() : cr.sx.trim();
            String y = cr.by != null ? cr.by.getValue().trim() : cr.sy.trim();
            String z = cr.bz != null ? cr.bz.getValue().trim() : cr.sz.trim();
            if (!x.isEmpty() || !y.isEmpty() || !z.isEmpty()) {
                perDim.get(cr.dim).add(new String[]{
                        x.isEmpty() ? "0" : x,
                        y.isEmpty() ? "0" : y,
                        z.isEmpty() ? "0" : z
                });
            }
        }

        List<CoordParser.DimCoords> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!perDim.get(i).isEmpty()) result.add(new CoordParser.DimCoords(i, perDim.get(i)));
        }
        return result;
    }

    protected String buildFinalDescription() {
        String desc = isRoot() ? (descField != null ? descField.getValue().trim() : savedDesc) 
                               : CoordParser.buildDescription(selectedParentId != null ? collectCoords() : new ArrayList<>(), descField != null ? descField.getValue().trim() : savedDesc);
        return colorCodes(desc);
    }

    protected String getBackgroundValue() {
        if (savedBackground != null && !savedBackground.isEmpty()) return savedBackground;
        return null;
    }

    protected String iconId() {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(selectedIcon);
        return loc != null ? loc.toString() : "minecraft:stone";
    }

    protected int currentDim() {
        if (minecraft == null || minecraft.level == null) return 0;
        String dim = minecraft.level.dimension().location().toString();
        if (dim.equals("minecraft:the_nether")) {
            return (minecraft.player != null && minecraft.player.getY() >= 127) ? 1 : 2;
        }
        if (dim.equals("minecraft:the_end")) return 3;
        return 0;
    }

    protected String colorCodes(String text) {
        return text != null ? text.replace('&', '§') : text;
    }

    private String parentName() {
        if (selectedParentId == null) return "нет";
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return selectedParentId.toString();
        AdvancementNode node = mc.player.connection.getAdvancements().getTree().get(selectedParentId);
        if (node != null && node.holder().value().display().isPresent()) {
            return node.holder().value().display().get().getTitle().getString();
        }
        return selectedParentId.toString();
    }

    private void openParentPicker() {
        AdvancementsScreen adv = new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this);
        minecraft.setScreen(adv);
        ((IAdvancementScreenCustom) adv).advWaypoint_setSelectMode(id -> onParentSelected(id));
    }

    private String shortBgName(String full) {
        if (full == null) return "?";
        int lastSlash = full.lastIndexOf('/');
        String name = lastSlash >= 0 ? full.substring(lastSlash + 1) : full;
        if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
        return name;
    }

    private EditBox addBox(int x, int y, int w, String hint, String val) {
        EditBox b = new EditBox(font, x, y, w, FH, Component.literal(""));
        b.setMaxLength(256);
        b.setHint(Component.literal(hint));
        b.setValue(val != null ? val : "");
        return addRenderableWidget(b);
    }

    private EditBox addCoord(int x, int y, String hint) {
        EditBox b = new EditBox(font, x, y, CFW, FH, Component.literal(""));
        b.setMaxLength(10);
        b.setHint(Component.literal(hint));
        return addRenderableWidget(b);
    }
}