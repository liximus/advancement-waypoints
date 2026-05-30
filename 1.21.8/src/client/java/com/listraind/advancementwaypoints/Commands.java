package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class Commands {
    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(
                        ClientCommandManager.literal("advancementwaypoints")
                                .executes(context -> {
                                    context.getSource().getClient().setScreen(new MainMenuScreen());
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                                    AdvancementWaypointsClient.reloadAdvancements();
                                    return 1;
                                }))
                )
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(
                        ClientCommandManager.literal("navigate")
                                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("y|z", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y|z");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            return navigateCommand(x, y, z);
                                                        })
                                                )
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int z = IntegerArgumentType.getInteger(ctx, "y|z");
                                                    return navigateCommand(x, 0, z);
                                                })
                                        )
                                )
                )
        );
    }

    private static int navigateCommand(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return 0;

        Navigator.Dimension dim = Navigator.Dimension.from(player.level().dimension());
        if (dim == null) return 0;

        Navigator nav = Navigator.getInstance();
        nav.clearAll();
        nav.setCurrentId(null);
        nav.setTargets(dim, List.of(new BlockPos(x, y, z)));

        return 1;
    }
}
