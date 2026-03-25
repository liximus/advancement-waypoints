package com.listraind.advancementwaypoints.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class ArrowModule {

    private static ArrowModule INSTANCE;

    private final Map<Dimension, BlockPos> targets = new EnumMap<>(Dimension.class);

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
        if (target == null) {
            targets.remove(dimension);
        } else {
            targets.put(dimension, target);
        }
    }

    @Nullable
    public BlockPos getTarget(Dimension dimension) {
        return targets.get(dimension);
    }

    public boolean hasAnyTarget() {
        return !targets.isEmpty();
    }

    public void clearAll() {
        targets.clear();
    }
}