package com.example.scout26.client;

import com.example.scout26.ModMenus;
import com.example.scout26.OpenPackPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only pack screen and configurable open-key bootstrap.
 */
public final class Scout26Client implements ClientModInitializer {
	private static final KeyMapping OPEN_PACK = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.scout26.open_pack",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_B,
		KeyMapping.Category.INVENTORY
	));

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.PACK, PackScreen::new);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_PACK.consumeClick()) {
				if (client.player != null
					&& client.level != null
					&& client.screen == null
					&& ClientPlayNetworking.canSend(OpenPackPayload.TYPE)) {
					ClientPlayNetworking.send(OpenPackPayload.INSTANCE);
				}
			}
		});
	}
}
