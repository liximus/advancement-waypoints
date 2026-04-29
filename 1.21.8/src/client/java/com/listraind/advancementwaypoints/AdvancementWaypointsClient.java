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
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

public class AdvancementWaypointsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Navigator.getInstance().initHud();
        // Debug: indicate client-side init
        if (AdvancementWaypoints.LOGGER != null) AdvancementWaypoints.LOGGER.info("Advancement Waypoints client initialized");

        KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "открыть меню",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "Advancement Waypoints"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                client.execute(() -> {
                    client.setScreen(new MainMenuScreen());
                });
                if (AdvancementWaypoints.LOGGER != null) AdvancementWaypoints.LOGGER.info("Opened MainMenuScreen via keybind");
            }
        });

        Commands.initialize();



        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "reload_listener");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        DarkModeChecker.setModDarkMode();
                    }
                });
    }

    public static void reloadAdvancements() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ((IAdvancementInjector) mc.player.connection.getAdvancements()).advWaypoint_inject();
    }
}
