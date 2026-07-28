package com.listraind.advancementwaypoints.advancement;

import java.util.Optional;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public record WaypointData(String id, String icon, String title, String description, String frame, String background, String parent, float x, float y) {
   public Identifier resourceLocation() {
      return Identifier.parse(this.id);
   }

   public ItemStack itemStack() {
      if (this.icon != null && !this.icon.isEmpty()) {
         try {
            Identifier id = Identifier.parse(this.icon);
            Optional<Holder.Reference<Item>> itemOpt = BuiltInRegistries.ITEM.get(id);
            if (itemOpt.isPresent() && ((Holder.Reference)itemOpt.get()).value() != Items.AIR) {
               return new ItemStack((ItemLike)((Holder.Reference)itemOpt.get()).value());
            }
         } catch (Exception var3) {
         }
      }

      return new ItemStack(Items.PAPER);
   }

   public AdvancementType frameType() {
      AdvancementType var10000;
      switch (this.frame) {
         case "goal" -> var10000 = AdvancementType.GOAL;
         case "challenge" -> var10000 = AdvancementType.CHALLENGE;
         default -> var10000 = AdvancementType.TASK;
      }

      return var10000;
   }
}
