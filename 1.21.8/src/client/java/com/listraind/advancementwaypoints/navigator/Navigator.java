package com.listraind.advancementwaypoints.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Navigator {

    private static Navigator INSTANCE;
    private final Map<Dimension, List<BlockPos>> targets = new EnumMap<>(Dimension.class);
    private ResourceLocation currId = null;

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

    private Navigator() {}

    public static Navigator getInstance() {
        if (INSTANCE == null) INSTANCE = new Navigator();
        return INSTANCE;
    }

    public void initHud() {
        NavigatorHud.getInstance().register();
    }

    public void setTargets(Dimension dim, List<BlockPos> list) {
        if (list == null || list.isEmpty()) targets.remove(dim);
        else targets.put(dim, new ArrayList<>(list));
    }

    @Nullable
    public BlockPos getNearest(Dimension dim, BlockPos from) {
        List<BlockPos> list = targets.get(dim);
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        BlockPos nearest = null;
        double min = Double.MAX_VALUE;
        for (BlockPos p : list) {
            double d = p.distSqr(from);
            if (d < min) { min = d; nearest = p; }
        }
        return nearest;
    }

    public boolean hasAnyTarget() { return !targets.isEmpty(); }
    public void clearAll() { targets.clear(); }
    public void setCurrentId(ResourceLocation id) {
        currId = id;
    }
    public ResourceLocation getCurrentId() {
        return currId;
    }
}