package com.listraind.advancementwaypoints.gui.context;

import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.gui.base.PopupMenu;
import com.listraind.advancementwaypoints.navigator.Navigator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class TargetSelectionMenu {
   private final PopupMenu menu = new PopupMenu();
   private Action action;
   private Identifier advancementId;
   private Map<Navigator.Dimension, List<BlockPos>> targets;

   public void show(int mouseX, int mouseY, Identifier advancementId, Map<Navigator.Dimension, List<BlockPos>> targets, Action action, Runnable onHideCallback) {
      this.advancementId = advancementId;
      this.targets = targets;
      this.action = action;
      this.menu.clear();
      this.menu.setOnHideCallback(onHideCallback);
      this.menu.addTextButton(Component.translatable("advwp.context.nearest"), () -> this.onEntrySelected((Navigator.Dimension)null, (BlockPos)null, true));
      if (targets != null) {
         for(Navigator.Dimension dim : Navigator.Dimension.values()) {
            List<BlockPos> list = (List)targets.get(dim);
            if (list != null && !list.isEmpty()) {
               byte var10000;
               switch (dim) {
                  case OVERWORLD -> var10000 = 0;
                  case NETHER -> var10000 = 2;
                  case END -> var10000 = 3;
                  default -> throw new MatchException((String)null, (Throwable)null);
               }

               int slot = var10000;
               String dimName = getLocalizedDimName(slot);

               for(int i = 0; i < list.size(); ++i) {
                  String labelText = list.size() > 1 ? dimName + " " + (i + 1) : dimName;
                  BlockPos finalPos = (BlockPos)list.get(i);
                  this.menu.addTextButton(Component.literal(labelText), () -> this.onEntrySelected(dim, finalPos, false));
               }
            }
         }
      }

      this.menu.show(mouseX, mouseY);
   }

   public boolean isVisible() {
      return this.menu.isVisible();
   }

   public boolean isMouseOver(double mx, double my) {
      return this.menu.isMouseOver(mx, my);
   }

   public void hide() {
      this.menu.hide();
   }

   public void render(GuiGraphicsExtractor g, int mx, int my, float pt) {
      this.menu.render(g, mx, my, pt);
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      return this.menu.mouseClicked(mx, my, btn);
   }

   private void onEntrySelected(Navigator.Dimension dimension, BlockPos pos, boolean isNearest) {
      if (isNearest) {
         if (this.action == TargetSelectionMenu.Action.NAVIGATE) {
            this.executeNavigateAll();
         } else if (this.action == TargetSelectionMenu.Action.TELEPORT) {
            this.executeTeleportNearest();
         }
      } else if (this.action == TargetSelectionMenu.Action.NAVIGATE) {
         Navigator nav = Navigator.getInstance();
         nav.clearAll();
         nav.setCurrentId(this.advancementId);
         nav.setTargets(dimension, List.of(pos));
      } else if (this.action == TargetSelectionMenu.Action.TELEPORT) {
         Minecraft minecraft = Minecraft.getInstance();
         teleportToPos(minecraft, dimension, pos);
      }

   }

   private void executeNavigateAll() {
      Navigator nav = Navigator.getInstance();
      nav.clearAll();
      nav.setCurrentId(this.advancementId);
      if (this.targets != null) {
         this.targets.forEach((dim, posList) -> {
            if (posList != null) {
               nav.setTargets(dim, posList);
            }

         });
      }

   }

   private void executeTeleportNearest() {
      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player != null && this.targets != null) {
         Navigator.Dimension dimension = Navigator.Dimension.from(player.level().dimension());
         if (dimension != null) {
            List<BlockPos> positions = (List)this.targets.get(dimension);
            if (positions != null && !positions.isEmpty()) {
               BlockPos target = Navigator.nearestOf(positions, player.blockPosition());
               if (target != null) {
                  teleportToPos(minecraft, dimension, target);
               }
            }
         }
      }
   }

   private static String getLocalizedDimName(int slot) {
      try {
         return Component.translatable(CoordParser.DIM_LABEL_KEYS[slot]).getString();
      } catch (Exception var2) {
         String var10000;
         switch (slot) {
            case 0 -> var10000 = "Верхний мир";
            case 1 -> var10000 = "Крыша ада";
            case 2 -> var10000 = "Ад";
            case 3 -> var10000 = "Энд";
            default -> var10000 = "Мир";
         }

         return var10000;
      }
   }

   private static void teleportToPos(Minecraft minecraft, Navigator.Dimension targetDim, BlockPos target) {
      if (minecraft.player != null) {
         Navigator.Dimension currentDim = Navigator.Dimension.from(minecraft.player.level().dimension());
         if (currentDim == targetDim) {
            ClientPacketListener var10000 = minecraft.player.connection;
            int var10001 = target.getX();
            var10000.sendCommand("tp " + var10001 + " " + target.getY() + " " + target.getZ());
         } else {
            String var5;
            switch (targetDim) {
               case OVERWORLD -> var5 = "minecraft:overworld";
               case NETHER -> var5 = "minecraft:the_nether";
               case END -> var5 = "minecraft:the_end";
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            String dimKey = var5;
            minecraft.player.connection.sendCommand("execute in " + dimKey + " run tp " + target.getX() + " " + target.getY() + " " + target.getZ());
         }

         minecraft.gui.setScreen((Screen)null);
      }
   }

   public static enum Action {
      NAVIGATE,
      TELEPORT;

      private static Action[] $values() {
         return new Action[]{NAVIGATE, TELEPORT};
      }
   }
}
