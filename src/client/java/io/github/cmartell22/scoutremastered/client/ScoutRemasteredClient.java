package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.ModMenus;
import io.github.cmartell22.scoutremastered.ModItems;
import io.github.cmartell22.scoutremastered.OpenPackPayload;
import com.mojang.blaze3d.platform.InputConstants;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
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
public final class ScoutRemasteredClient implements ClientModInitializer {
	private static final KeyMapping OPEN_PACK = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.scoutremastered.open_pack",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_B,
		KeyMapping.Category.INVENTORY
	));

	@Override
	public void onInitializeClient() {
		IntegratedInventoryConfig.load();
		ReadySlotConfig.load();
		IntegratedInventoryClientNetworking.initialize();
		IntegratedInventoryScreenEvents.initialize();
		ReadySlotKeyMappings.initialize();
		MenuScreens.register(ModMenus.PACK, PackScreen::new);
		TrinketRendererRegistry.registerRenderer(ModItems.SATCHEL, BagTrinketRenderer.INSTANCE);
		TrinketRendererRegistry.registerRenderer(ModItems.UPGRADED_SATCHEL, BagTrinketRenderer.INSTANCE);
		TrinketRendererRegistry.registerRenderer(ModItems.POUCH, BagTrinketRenderer.INSTANCE);
		TrinketRendererRegistry.registerRenderer(ModItems.UPGRADED_POUCH, BagTrinketRenderer.INSTANCE);
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
