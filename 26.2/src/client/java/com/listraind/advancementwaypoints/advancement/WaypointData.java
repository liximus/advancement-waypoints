package com.listraind.advancementwaypoints.advancement;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record WaypointData(
        String id, String icon, String title, String description,
        String frame, String background, String parent,
        float x, float y
) {
    public Identifier resourceLocation() {
        return Identifier.parse(id);
    }

    public ItemStack itemStack() {
        try {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(icon)));
        } catch (Exception e) {
            return new ItemStack(Items.BARRIER);
        }
    }

    public AdvancementType frameType() {
        return switch (frame) {
            case "goal" -> AdvancementType.GOAL;
            case "challenge" -> AdvancementType.CHALLENGE;
            default -> AdvancementType.TASK;
        };
    }
}