package com.listraind.advancementwaypoints.gui.context;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.PopupMenu;
import com.listraind.advancementwaypoints.gui.dialogs.ConfirmDeleteScreen;
import com.listraind.advancementwaypoints.gui.dialogs.CreateWaypointScreen;
import com.listraind.advancementwaypoints.gui.dialogs.EditWaypointScreen;
import com.listraind.advancementwaypoints.gui.dialogs.TabVisibilityScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;

import static com.listraind.advancementwaypoints.AdvancementWaypoints.MOD_ID;

public class AdvancementContextMenu {

    private static final Identifier CREATE_ICON = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wpmenu/create.png");
    private static final Identifier EDIT_ICON = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wpmenu/edit.png");
    private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/wpmenu/delete.png");

    private final PopupMenu menu = new PopupMenu();
    private TargetSelectionMenu targetSelectionMenu;

    private Screen lastScreen;
    private Identifier advancementId;
    private Map<Navigator.Dimension, List<BlockPos>> targets;

    public void show(int mouseX, int mouseY, Identifier id, Map<Navigator.Dimension, List<BlockPos>> targets) {
        this.advancementId = id;
        this.targets = targets;
        if (targetSelectionMenu != null) targetSelectionMenu.hide();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) return;

        boolean hasTpPerms = minecraft.player != null && minecraft.player.canUseGameMasterBlocks();
        boolean isCustom = id.toString().startsWith("advwaypoints:");
        int targetCount = countTotalTargets();

        menu.clear();

        if (targetCount > 0) {
            if (com.listraind.advancementwaypoints.config.ModConfig.getInstance().isEnableNavigation()) {
                menu.addTextButton(Component.translatable("advwp.context.navigate"), () -> {
                    if (targetCount <= 1) {
                        onNavigate();
                    } else {
                        openTargetSelectionMenu(TargetSelectionMenu.Action.NAVIGATE, mouseX, mouseY);
                    }
                });
            }

            if (hasTpPerms) {
                menu.addTextButton(Component.translatable("advwp.context.teleport"), () -> {
                    Navigator.Dimension currentDim = (minecraft.player != null) ? Navigator.Dimension.from(minecraft.player.level().dimension()) : null;
                    List<BlockPos> currentDimPos = (targets != null && currentDim != null) ? targets.get(currentDim) : null;
                    boolean singleInCurrentDim = (targetCount == 1) && (currentDimPos != null && currentDimPos.size() == 1);

                    if (singleInCurrentDim) {
                        onTeleport();
                    } else {
                        openTargetSelectionMenu(TargetSelectionMenu.Action.TELEPORT, mouseX, mouseY);
                    }
                });
            }
        }

        boolean isBranchHidden = WaypointStorage.isBranchHidden(id.toString());
        boolean hasChildren = WaypointStorage.hasChildren(id);
        if (hasChildren || isBranchHidden) {
            Component branchButtonLabel = Component.translatable(
                    isBranchHidden ? "advwp.context.show_branch" : "advwp.context.hide_branch"
            );
            menu.addTextButton(branchButtonLabel, () -> {
                WaypointStorage.toggleBranchHidden(id.toString());
                hide();
            });
        }

        menu.addSquareButton(null, Component.translatable("advwp.context.sq_none"), null, false);
        menu.addSquareButton(CREATE_ICON, Component.translatable("advwp.context.sq_new"), () -> onSquareAction(1), true);
        menu.addSquareButton(EDIT_ICON, Component.translatable("advwp.context.sq_edit"), () -> onSquareAction(2), isCustom);
        menu.addSquareButton(DELETE_ICON, Component.translatable("advwp.context.sq_del"), () -> onSquareAction(3), isCustom);

        menu.show(mouseX, mouseY);
    }

    public void showTabMenu(int mouseX, int mouseY) {
        this.advancementId = null;
        this.targets = null;
        if (targetSelectionMenu != null) targetSelectionMenu.hide();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) return;

        menu.clear();
        menu.addTextButton(Component.translatable("advwp.context.create_tab"), () -> {
            CreateWaypointScreen screen = new CreateWaypointScreen(true);
            screen.onCloseAction = () -> reopenAdvancementsScreen(minecraft);
            minecraft.gui.setScreen(screen);
        });

        menu.addTextButton(Component.translatable("advwp.context.tab_visibility"), () -> {
            TabVisibilityScreen screen = new TabVisibilityScreen(lastScreen);
            minecraft.gui.setScreen(screen);
        });

        menu.show(mouseX, mouseY);
    }

    public void setLastScreen(Screen screen) {
        lastScreen = screen;
    }

    public void hide() {
        menu.hide();
        if (targetSelectionMenu != null) targetSelectionMenu.hide();
    }

    public boolean isVisible() {
        return menu.isVisible() || (targetSelectionMenu != null && targetSelectionMenu.isVisible());
    }

    public boolean isMouseOver(double mx, double my) {
        if (targetSelectionMenu != null && targetSelectionMenu.isVisible()) {
            return targetSelectionMenu.isMouseOver(mx, my);
        }
        return menu.isMouseOver(mx, my);
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (targetSelectionMenu != null && targetSelectionMenu.isVisible()) {
            targetSelectionMenu.render(g, mx, my, pt);
            return;
        }
        menu.render(g, mx, my, pt);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (targetSelectionMenu != null && targetSelectionMenu.isVisible()) {
            return targetSelectionMenu.mouseClicked(mx, my, btn);
        }
        return menu.mouseClicked(mx, my, btn);
    }

    private void openTargetSelectionMenu(TargetSelectionMenu.Action action, int mouseX, int mouseY) {
        if (targetSelectionMenu == null) {
            targetSelectionMenu = new TargetSelectionMenu();
        }
        targetSelectionMenu.show(mouseX, mouseY, advancementId, targets, action, () -> menu.hide());
    }

    private int countTotalTargets() {
        if (targets == null) return 0;
        int count = 0;
        for (List<BlockPos> list : targets.values()) {
            if (list != null) count += list.size();
        }
        return count;
    }

    private void onNavigate() {
        Navigator nav = Navigator.getInstance();
        nav.setCurrentId(advancementId);
        nav.clearAll();
        if (targets != null) {
            targets.forEach((dim, posList) -> {
                if (posList != null) nav.setTargets(dim, posList);
            });
        }
    }

    private void onTeleport() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || targets == null) return;
        Navigator.Dimension dimension = Navigator.Dimension.from(player.level().dimension());
        if (dimension == null) return;
        List<BlockPos> positions = targets.get(dimension);
        if (positions == null || positions.isEmpty()) return;
        BlockPos target = Navigator.nearestOf(positions, player.blockPosition());
        if (target == null) return;
        teleportTo(minecraft, target);
    }

    private void onSquareAction(int index) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (index) {
            case 1 -> {
                WaypointStorage.setLastParent(advancementId);
                CreateWaypointScreen screen = new CreateWaypointScreen();
                screen.onCloseAction = () -> reopenAdvancementsScreen(minecraft);
                minecraft.gui.setScreen(screen);
            }
            case 2 -> {
                JsonObject data = WaypointStorage.getWaypointOrVanilla(advancementId);
                EditWaypointScreen screen = new EditWaypointScreen(data);
                screen.onCloseAction = () -> reopenAdvancementsScreen(minecraft);
                minecraft.gui.setScreen(screen);
            }
            case 3 -> {
                minecraft.gui.setScreen(new ConfirmDeleteScreen(
                        new AdvancementsScreen(minecraft.player.connection.getAdvancements(), lastScreen),
                        () -> {
                            WaypointStorage.deleteWaypoint(advancementId.toString());
                            reopenAdvancementsScreen(minecraft);
                        }
                ));
            }
        }
    }

    private void reopenAdvancementsScreen(Minecraft minecraft) {
        if (minecraft.player == null) return;
        minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), lastScreen));
    }

    private static void teleportTo(Minecraft minecraft, BlockPos target) {
        minecraft.player.connection.sendCommand("tp " + target.getX() + " " + target.getY() + " " + target.getZ());
        minecraft.gui.setScreen(null);
    }
}
