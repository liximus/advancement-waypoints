package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.gui.AddWaypointScreen;
import com.listraind.advancementwaypoints.gui.BaseWaypointScreen;
import com.listraind.advancementwaypoints.navigator.ArrowModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class AdvancementWaypointsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Command.register();
		ArrowModule.getInstance().init();

		KeyMapping openWaypointKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.waypoints.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.waypoints"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openWaypointKey.consumeClick()) {
				client.setScreen(new BaseWaypointScreen());
			}
		});
	}

	public record ParsedAdvancement(
			String id, String icon, String title, String description,
			String frame, String background, String parent,
			float x, float y
	) {
		public ResourceLocation resourceLocation() { return ResourceLocation.parse(id); }

		public ItemStack itemStack() {
			try {
				return new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(icon)));
			} catch (Exception e) { return new ItemStack(Items.BARRIER); }
		}

		public AdvancementType frameType() {
			return switch (frame) {
				case "goal" -> AdvancementType.GOAL;
				case "challenge" -> AdvancementType.CHALLENGE;
				default -> AdvancementType.TASK;
			};
		}
	}
}