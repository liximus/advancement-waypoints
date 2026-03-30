package com.listraind.advancementwaypoints.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ArrowModule {

    private static ArrowModule INSTANCE;

    private final Map<Dimension, List<BlockPos>> targets = new EnumMap<>(Dimension.class);

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

    private ArrowModule() {}

    public static ArrowModule getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ArrowModule();
        }
        return INSTANCE;
    }

    public void init() {
        ArrowRenderer.getInstance().register();
    }

    public void setTarget(Dimension dimension, @Nullable BlockPos target) {
        targets.remove(dimension);
        if (target != null) {
            List<BlockPos> list = new ArrayList<>();
            list.add(target);
            targets.put(dimension, list);
        }
    }

    public void setTargets(Dimension dimension, List<BlockPos> targetsList) {
        if (targetsList == null || targetsList.isEmpty()) {
            targets.remove(dimension);
        } else {
            targets.put(dimension, new ArrayList<>(targetsList));
        }
    }

    @Nullable
    public BlockPos getTarget(Dimension dimension) {
        List<BlockPos> list = targets.get(dimension);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Nullable
    public List<BlockPos> getTargets(Dimension dimension) {
        return targets.get(dimension);
    }

    @Nullable
    public BlockPos getNearestTarget(Dimension dimension, BlockPos playerPos) {
        List<BlockPos> list = targets.get(dimension);
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);

        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        for (BlockPos pos : list) {
            double dx = pos.getX() - playerPos.getX();
            double dz = pos.getZ() - playerPos.getZ();
            double dist = dx * dx + dz * dz;
            if (dist < minDist) {
                minDist = dist;
                nearest = pos;
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
}