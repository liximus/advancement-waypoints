package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class AdvancementWaypointsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Navigator.getInstance().initHud();

        KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.waypoints.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.waypoints"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                client.setScreen(new MainMenuScreen());
            }
        });

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
        ((IAdvancementInjector) mc.player.connection.getAdvancements()).advWaypoint_inject();
    }
}