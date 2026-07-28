package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.dialogs.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class Commands {
   public static void initialize() {
      ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, access) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("advancementwaypoints").executes((context) -> {
            ((FabricClientCommandSource)context.getSource()).getClient().gui.setScreen(new MainMenuScreen());
            return 1;
         })).then(ClientCommands.literal("reload").executes((ctx) -> {
            AdvancementWaypointsClient.reloadAdvancements();
            return 1;
         }))));
      ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, access) -> dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal("navigate").then(ClientCommands.argument("x", IntegerArgumentType.integer()).then(((RequiredArgumentBuilder)ClientCommands.argument("y|z", IntegerArgumentType.integer()).then(ClientCommands.argument("z", IntegerArgumentType.integer()).executes((ctx) -> {
            int x = IntegerArgumentType.getInteger(ctx, "x");
            int y = IntegerArgumentType.getInteger(ctx, "y|z");
            int z = IntegerArgumentType.getInteger(ctx, "z");
            return navigateCommand(x, y, z);
         }))).executes((ctx) -> {
            int x = IntegerArgumentType.getInteger(ctx, "x");
            int z = IntegerArgumentType.getInteger(ctx, "y|z");
            return navigateCommand(x, 0, z);
         })))));
   }

   private static int navigateCommand(int x, int y, int z) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null) {
         return 0;
      } else {
         Navigator.Dimension currentDim = Navigator.Dimension.from(player.level().dimension());
         if (currentDim == null) {
            currentDim = Navigator.Dimension.OVERWORLD;
         }

         BlockPos overworldPos;
         BlockPos netherPos;
         BlockPos endPos;
         if (currentDim == Navigator.Dimension.NETHER) {
            netherPos = new BlockPos(x, y, z);
            overworldPos = new BlockPos(x * 8, y, z * 8);
            endPos = new BlockPos(x * 8, y, z * 8);
         } else if (currentDim == Navigator.Dimension.OVERWORLD) {
            overworldPos = new BlockPos(x, y, z);
            netherPos = new BlockPos((int)Math.round((double)x / (double)8.0F), y, (int)Math.round((double)z / (double)8.0F));
            endPos = new BlockPos(x, y, z);
         } else {
            endPos = new BlockPos(x, y, z);
            overworldPos = new BlockPos(x, y, z);
            netherPos = new BlockPos((int)Math.round((double)x / (double)8.0F), y, (int)Math.round((double)z / (double)8.0F));
         }

         Navigator nav = Navigator.getInstance();
         nav.clearAll();
         nav.setCurrentId((Identifier)null);
         nav.setTargets(Navigator.Dimension.OVERWORLD, List.of(overworldPos));
         nav.setTargets(Navigator.Dimension.NETHER, List.of(netherPos));
         nav.setTargets(Navigator.Dimension.END, List.of(endPos));
         return 1;
      }
   }
}
