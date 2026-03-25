package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.advancementMixinHelpers.ICustomAdvancementApplier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

public class Command {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("waypointreload").executes(ctx -> {
                    reloadAdvancements();
                    return 1;
                }))
        );
    }

    public static void reloadAdvancements() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ((ICustomAdvancementApplier) mc.player.connection.getAdvancements())
                .advWaypoint_injectCustomAdvancements();
    }
}