package com.listraind.advancementwaypoints.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Navigator {

    private static Navigator INSTANCE;
    private final Map<Dimension, List<BlockPos>> targets = new EnumMap<>(Dimension.class);
    private Identifier currId = null;

    public enum Dimension {
        OVERWORLD, NETHER, END;

        @Nullable
        public static Dimension from(ResourceKey<Level> key) {
            if (key == Level.OVERWORLD) return OVERWORLD;
            if (key == Level.NETHER) return NETHER;
            if (key == Level.END) return END;
            return null;
        }
    }

    private Navigator() {
    }

    public static Navigator getInstance() {
        if (INSTANCE == null) INSTANCE = new Navigator();
        return INSTANCE;
    }

    public void initHud() {
        NavigatorHud.getInstance().register();
    }

    public void setTargets(Dimension dim, List<BlockPos> list) {
        if (dim == null) return;
        if (list == null || list.isEmpty()) targets.remove(dim);
        else targets.put(dim, new ArrayList<>(list));
    }

    @Nullable
    public BlockPos getNearest(Dimension dim, BlockPos from) {
        if (from == null) return null;
        return nearestOf(targets.get(dim), from);
    }

    @Nullable
    public static BlockPos nearestOf(List<BlockPos> list, BlockPos from) {
        if (from == null) return null;
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        BlockPos nearest = null;
        long min = Long.MAX_VALUE;
        for (BlockPos p : list) {
            if (p == null) continue;
            long dx = (long) p.getX() - from.getX();
            long dz = (long) p.getZ() - from.getZ();
            long distSq = dx * dx + dz * dz;
            if (distSq < min) {
                min = distSq;
                nearest = p;
            } else if (distSq == min) {
                if (nearest == null) {
                    nearest = p;
                } else {
                    if (p.getX() < nearest.getX() ||
                            (p.getX() == nearest.getX() && (p.getZ() < nearest.getZ() ||
                                    (p.getZ() == nearest.getZ() && p.getY() < nearest.getY())))) {
                        nearest = p;
                    }
                }
            }
        }
        return nearest;
    }

    public boolean hasAnyTarget() {
        return !targets.isEmpty();
    }

    public void clearAll() {
        targets.clear();
    }

    public void setCurrentId(Identifier id) {
        currId = id;
    }

    public Identifier getCurrentId() {
        return currId;
    }
}
