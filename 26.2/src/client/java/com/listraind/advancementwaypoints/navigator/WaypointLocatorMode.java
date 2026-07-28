package com.listraind.advancementwaypoints.navigator;

import com.listraind.advancementwaypoints.config.ModConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import org.jetbrains.annotations.Nullable;

public class WaypointLocatorMode {

    private static WaypointLocatorMode INSTANCE;
    private final List<TrackedWaypoint> activeWaypoints = new ArrayList<>();

    private WaypointLocatorMode() {}

    public static WaypointLocatorMode getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WaypointLocatorMode();
        }
        return INSTANCE;
    }

    public static UUID getUuidForIndex(int index) {
        return UUID.nameUUIDFromBytes(("advancementwaypoints:nav_target_" + index).getBytes());
    }

    public static boolean isModWaypoint(@Nullable UUID uuid) {
        if (uuid == null) return false;
        for (int i = 0; i < 64; i++) {
            if (getUuidForIndex(i).equals(uuid)) return true;
        }
        return false;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            removeAllWaypoints(mc);
            return;
        }

        ModConfig config = ModConfig.getInstance();
        if (!config.isEnableNavigation() || config.getHudMode() != ModConfig.HudMode.LOCATOR) {
            removeAllWaypoints(mc);
            return;
        }

        Navigator nav = Navigator.getInstance();
        if (!nav.hasAnyTarget()) {
            removeAllWaypoints(mc);
            return;
        }

        Navigator.Dimension dim = Navigator.Dimension.from(mc.player.level().dimension());
        ClientWaypointManager manager = getManager(mc);
        if (dim == null || manager == null) {
            removeAllWaypoints(mc);
            return;
        }

        List<BlockPos> posList = nav.getTargetsForDimension(dim);
        if (posList == null || posList.isEmpty()) {
            updatePortalOnly(manager, mc.player.blockPosition());
            return;
        }

        updateDimensionWaypoints(manager, posList);
    }

    private void updateDimensionWaypoints(ClientWaypointManager manager, List<BlockPos> posList) {
        int needed = posList.size();

        while (activeWaypoints.size() > needed) {
            int lastIndex = activeWaypoints.size() - 1;
            manager.untrackWaypoint(activeWaypoints.remove(lastIndex));
        }

        Waypoint.Icon icon = new Waypoint.Icon();
        icon.style = WaypointStyleAssets.DEFAULT;
        icon.color = java.util.Optional.empty();

        for (int i = 0; i < needed; i++) {
            BlockPos pos = posList.get(i);
            UUID uuid = getUuidForIndex(i);
            Vec3i vec = new Vec3i(pos.getX(), pos.getY(), pos.getZ());
            TrackedWaypoint wp = TrackedWaypoint.setPosition(uuid, icon, vec);

            if (i < activeWaypoints.size()) {
                manager.updateWaypoint(wp);
                activeWaypoints.set(i, wp);
            } else {
                manager.trackWaypoint(wp);
                activeWaypoints.add(wp);
            }
        }
    }

    private void updatePortalOnly(ClientWaypointManager manager, BlockPos playerPos) {
        while (activeWaypoints.size() > 1) {
            int lastIndex = activeWaypoints.size() - 1;
            manager.untrackWaypoint(activeWaypoints.remove(lastIndex));
        }

        UUID uuid = getUuidForIndex(0);
        Waypoint.Icon icon = new Waypoint.Icon();
        icon.style = WaypointStyleAssets.DEFAULT;

        TrackedWaypoint wp = TrackedWaypoint.setAzimuth(uuid, icon, 0.0f);

        if (activeWaypoints.isEmpty()) {
            manager.trackWaypoint(wp);
            activeWaypoints.add(wp);
        } else {
            manager.updateWaypoint(wp);
            activeWaypoints.set(0, wp);
        }
    }

    public void removeAllWaypoints(Minecraft mc) {
        ClientWaypointManager manager = getManager(mc);
        if (manager != null) {
            for (TrackedWaypoint wp : activeWaypoints) {
                manager.untrackWaypoint(wp);
            }
        }
        activeWaypoints.clear();
    }

    public void removeWaypoint() {
        removeAllWaypoints(Minecraft.getInstance());
    }

    private static @Nullable ClientWaypointManager getManager(Minecraft mc) {
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return null;
        return conn.getWaypointManager();
    }

    public static @Nullable ItemStack getCurrentTargetIcon() {
        Navigator nav = Navigator.getInstance();
        Identifier id = nav.getCurrentId();
        if (id == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return null;
        AdvancementHolder holder = mc.player.connection.getAdvancements().get(id);
        if (holder != null && holder.value().display().isPresent()) {
            return holder.value().display().get().getIcon().create();
        }
        return null;
    }
}
