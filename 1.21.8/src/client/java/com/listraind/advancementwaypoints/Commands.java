package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

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
    }
}
