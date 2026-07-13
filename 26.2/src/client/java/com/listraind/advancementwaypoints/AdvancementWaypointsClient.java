package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.gui.MainMenuScreen;
import com.listraind.advancementwaypoints.navigator.Navigator;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import static com.listraind.advancementwaypoints.AdvancementWaypoints.MOD_ID;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

public class AdvancementWaypointsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Navigator.getInstance().initHud();
        if (AdvancementWaypoints.LOGGER != null)
            AdvancementWaypoints.LOGGER.info("Advancement Waypoints client initialized");

        KeyMapping.Category keyCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "key_category"));

        KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "advwp.key.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                keyCategory
        ));

        KeyMapping clearNavKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "advwp.key.clear_nav",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                keyCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                client.execute(() -> {
                    client.gui.setScreen(new MainMenuScreen());
                });
                if (AdvancementWaypoints.LOGGER != null)
                    AdvancementWaypoints.LOGGER.info("Opened MainMenuScreen via keybind");
            }
            while (clearNavKey.consumeClick()) {
                Navigator nav = Navigator.getInstance();
                nav.clearAll();
                nav.setCurrentId(null);
            }
        });

        Commands.initialize();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WaypointStorage.setLastParent(null);
            Navigator nav = Navigator.getInstance();
            nav.clearAll();
            nav.setCurrentId(null);
        });


        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.fromNamespaceAndPath(AdvancementWaypoints.MOD_ID, "reload_listener");
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
