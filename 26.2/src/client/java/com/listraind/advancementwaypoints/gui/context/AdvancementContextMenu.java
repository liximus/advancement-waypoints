package com.listraind.advancementwaypoints.gui.context;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.base.PopupMenu;
import com.listraind.advancementwaypoints.gui.dialogs.ConfirmDeleteScreen;
import com.listraind.advancementwaypoints.gui.dialogs.CreateWaypointScreen;
import com.listraind.advancementwaypoints.gui.dialogs.EditWaypointScreen;
import com.listraind.advancementwaypoints.gui.dialogs.TabVisibilityScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class AdvancementContextMenu {
   private static final Identifier CREATE_ICON = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/gui/sprites/wpmenu/create.png");
   private static final Identifier EDIT_ICON = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/gui/sprites/wpmenu/edit.png");
   private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath("advancement-waypoints", "textures/gui/sprites/wpmenu/delete.png");
   private final PopupMenu menu = new PopupMenu();
   private TargetSelectionMenu targetSelectionMenu;
   private Screen lastScreen;
   private Identifier advancementId;
   private Map<Navigator.Dimension, List<BlockPos>> targets;

   public void show(int mouseX, int mouseY, Identifier id, Map<Navigator.Dimension, List<BlockPos>> targets) {
      this.advancementId = id;
      this.targets = targets;
      if (this.targetSelectionMenu != null) {
         this.targetSelectionMenu.hide();
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() != null) {
         boolean hasTpPerms = minecraft.player != null && minecraft.player.canUseGameMasterBlocks();
         boolean isCustom = id.toString().startsWith("advwaypoints:");
         int targetCount = this.countTotalTargets();
         this.menu.clear();
         if (isCustom && targetCount > 0) {
            this.menu.addTextButton(Component.translatable("advwp.context.navigate"), () -> {
               if (targetCount <= 1) {
                  this.onNavigate();
               } else {
                  this.openTargetSelectionMenu(TargetSelectionMenu.Action.NAVIGATE, mouseX, mouseY);
               }

            });
            if (hasTpPerms) {
               this.menu.addTextButton(Component.translatable("advwp.context.teleport"), () -> {
                  Navigator.Dimension currentDim = minecraft.player != null ? Navigator.Dimension.from(minecraft.player.level().dimension()) : null;
                  List<BlockPos> currentDimPos = targets != null && currentDim != null ? (List)targets.get(currentDim) : null;
                  boolean singleInCurrentDim = targetCount == 1 && currentDimPos != null && currentDimPos.size() == 1;
                  if (singleInCurrentDim) {
                     this.onTeleport();
                  } else {
                     this.openTargetSelectionMenu(TargetSelectionMenu.Action.TELEPORT, mouseX, mouseY);
                  }

               });
            }
         }

         boolean isBranchHidden = WaypointStorage.isBranchHidden(id.toString());
         boolean hasChildren = WaypointStorage.hasChildren(id);
         if (hasChildren || isBranchHidden) {
            Component branchButtonLabel = Component.translatable(isBranchHidden ? "advwp.context.show_branch" : "advwp.context.hide_branch");
            this.menu.addTextButton(branchButtonLabel, () -> {
               WaypointStorage.toggleBranchHidden(id.toString());
               this.hide();
            });
         }

         this.menu.addSquareButton((Identifier)null, Component.translatable("advwp.context.sq_none"), (PopupMenu.MenuAction)null, false);
         this.menu.addSquareButton(CREATE_ICON, Component.translatable("advwp.context.sq_new"), () -> this.onSquareAction(1), true);
         this.menu.addSquareButton(EDIT_ICON, Component.translatable("advwp.context.sq_edit"), () -> this.onSquareAction(2), isCustom);
         this.menu.addSquareButton(DELETE_ICON, Component.translatable("advwp.context.sq_del"), () -> this.onSquareAction(3), isCustom);
         this.menu.show(mouseX, mouseY);
      }
   }

   public void showTabMenu(int mouseX, int mouseY) {
      this.advancementId = null;
      this.targets = null;
      if (this.targetSelectionMenu != null) {
         this.targetSelectionMenu.hide();
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() != null) {
         this.menu.clear();
         this.menu.addTextButton(Component.translatable("advwp.context.create_tab"), () -> {
            CreateWaypointScreen screen = new CreateWaypointScreen(true);
            screen.onCloseAction = () -> this.reopenAdvancementsScreen(minecraft);
            minecraft.gui.setScreen(screen);
         });
         this.menu.addTextButton(Component.translatable("advwp.context.tab_visibility"), () -> {
            TabVisibilityScreen screen = new TabVisibilityScreen(this.lastScreen);
            minecraft.gui.setScreen(screen);
         });
         this.menu.show(mouseX, mouseY);
      }
   }

   public void setLastScreen(Screen screen) {
      this.lastScreen = screen;
   }

   public void hide() {
      this.menu.hide();
      if (this.targetSelectionMenu != null) {
         this.targetSelectionMenu.hide();
      }

   }

   public boolean isVisible() {
      return this.menu.isVisible() || this.targetSelectionMenu != null && this.targetSelectionMenu.isVisible();
   }

   public boolean isMouseOver(double mx, double my) {
      return this.targetSelectionMenu != null && this.targetSelectionMenu.isVisible() ? this.targetSelectionMenu.isMouseOver(mx, my) : this.menu.isMouseOver(mx, my);
   }

   public void render(GuiGraphicsExtractor g, int mx, int my, float pt) {
      if (this.targetSelectionMenu != null && this.targetSelectionMenu.isVisible()) {
         this.targetSelectionMenu.render(g, mx, my, pt);
      } else {
         this.menu.render(g, mx, my, pt);
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      return this.targetSelectionMenu != null && this.targetSelectionMenu.isVisible() ? this.targetSelectionMenu.mouseClicked(mx, my, btn) : this.menu.mouseClicked(mx, my, btn);
   }

   private void openTargetSelectionMenu(TargetSelectionMenu.Action action, int mouseX, int mouseY) {
      if (this.targetSelectionMenu == null) {
         this.targetSelectionMenu = new TargetSelectionMenu();
      }

      this.targetSelectionMenu.show(mouseX, mouseY, this.advancementId, this.targets, action, () -> this.menu.hide());
   }

   private int countTotalTargets() {
      if (this.targets == null) {
         return 0;
      } else {
         int count = 0;

         for(List<BlockPos> list : this.targets.values()) {
            if (list != null) {
               count += list.size();
            }
         }

         return count;
      }
   }

   private void onNavigate() {
      Navigator nav = Navigator.getInstance();
      nav.setCurrentId(this.advancementId);
      nav.clearAll();
      if (this.targets != null) {
         this.targets.forEach((dim, posList) -> {
            if (posList != null) {
               nav.setTargets(dim, posList);
            }

         });
      }

   }

   private void onTeleport() {
      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player != null && this.targets != null) {
         Navigator.Dimension dimension = Navigator.Dimension.from(player.level().dimension());
         if (dimension != null) {
            List<BlockPos> positions = (List)this.targets.get(dimension);
            if (positions != null && !positions.isEmpty()) {
               BlockPos target = Navigator.nearestOf(positions, player.blockPosition());
               if (target != null) {
                  teleportTo(minecraft, target);
               }
            }
         }
      }
   }

   private void onSquareAction(int index) {
      Minecraft minecraft = Minecraft.getInstance();
      switch (index) {
         case 1:
            WaypointStorage.setLastParent(this.advancementId);
            CreateWaypointScreen createScreen = new CreateWaypointScreen();
            createScreen.onCloseAction = () -> this.reopenAdvancementsScreen(minecraft);
            minecraft.gui.setScreen(createScreen);
            break;
         case 2:
            JsonObject data = WaypointStorage.getWaypointOrVanilla(this.advancementId);
            EditWaypointScreen editScreen = new EditWaypointScreen(data);
            editScreen.onCloseAction = () -> this.reopenAdvancementsScreen(minecraft);
            minecraft.gui.setScreen(editScreen);
            break;
         case 3:
            minecraft.gui.setScreen(new ConfirmDeleteScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this.lastScreen), () -> {
               WaypointStorage.deleteWaypoint(this.advancementId.toString());
               this.reopenAdvancementsScreen(minecraft);
            }));
      }

   }

   private void reopenAdvancementsScreen(Minecraft minecraft) {
      if (minecraft.player != null) {
         minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this.lastScreen));
      }
   }

   private static void teleportTo(Minecraft minecraft, BlockPos target) {
      ClientPacketListener var10000 = minecraft.player.connection;
      int var10001 = target.getX();
      var10000.sendCommand("tp " + var10001 + " " + target.getY() + " " + target.getZ());
      minecraft.gui.setScreen((Screen)null);
   }
}
