package com.listraind.advancementwaypoints.navigator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class Navigator {
   private static Navigator INSTANCE;
   private final Map<Dimension, List<BlockPos>> targets = new EnumMap(Dimension.class);
   private Identifier currentId = null;

   private Navigator() {
   }

   public static Navigator getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Navigator();
      }

      return INSTANCE;
   }

   public void initHud() {
      NavigatorHud.getInstance().register();
   }

   public void setTargets(Dimension dim, List<BlockPos> list) {
      if (dim != null) {
         if (list != null && !list.isEmpty()) {
            this.targets.put(dim, new ArrayList(list));
         } else {
            this.targets.remove(dim);
         }

      }
   }

   public @Nullable BlockPos getNearest(Dimension dim, BlockPos from) {
      return from == null ? null : nearestOf((List)this.targets.get(dim), from);
   }

   public static @Nullable BlockPos nearestOf(List<BlockPos> list, BlockPos from) {
      if (from != null && list != null && !list.isEmpty()) {
         if (list.size() == 1) return list.get(0);
         return list.stream()
            .filter(Objects::nonNull)
            .min(Comparator.<BlockPos, Long>comparing(p -> squaredPlanarDist(p, from), Long::compare)
               .thenComparingInt(BlockPos::getX)
               .thenComparingInt(BlockPos::getZ)
               .thenComparingInt(BlockPos::getY))
            .orElse(null);
      } else {
         return null;
      }
   }

   private static long squaredPlanarDist(BlockPos a, BlockPos b) {
      long dx = (long)a.getX() - (long)b.getX();
      long dz = (long)a.getZ() - (long)b.getZ();
      return dx * dx + dz * dz;
   }

   public boolean hasAnyTarget() {
      return !this.targets.isEmpty();
   }

   public void clearAll() {
      this.targets.clear();
   }

   public void setCurrentId(Identifier id) {
      this.currentId = id;
   }

   public Identifier getCurrentId() {
      return this.currentId;
   }

   public static enum Dimension {
      OVERWORLD,
      NETHER,
      END;

      public static @Nullable Dimension from(ResourceKey<Level> key) {
         if (key == Level.OVERWORLD) {
            return OVERWORLD;
         } else if (key == Level.NETHER) {
            return NETHER;
         } else {
            return key == Level.END ? END : null;
         }
      }

      // $FF: synthetic method
      private static Dimension[] $values() {
         return new Dimension[]{OVERWORLD, NETHER, END};
      }
   }
}
