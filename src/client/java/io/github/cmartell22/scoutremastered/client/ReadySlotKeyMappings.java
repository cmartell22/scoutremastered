package io.github.cmartell22.scoutremastered.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cmartell22.scoutremastered.ReadySlotRole;
import io.github.cmartell22.scoutremastered.SwapReadySlotPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Configurable ready-slot swap controls plus the client-only RS7A presentation editor. */
public final class ReadySlotKeyMappings {
	private static final int UNBOUND = InputConstants.UNKNOWN.getValue();
	private static final KeyMapping SWAP_LEFT = register("key.scoutremastered.swap_left_ready");
	private static final KeyMapping SWAP_RIGHT = register("key.scoutremastered.swap_right_ready");
	private static final KeyMapping SWAP_SATCHEL = register("key.scoutremastered.swap_back_ready");
	private static final KeyMapping OPEN_EDITOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.scoutremastered.open_ready_slots_editor",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_O,
		KeyMapping.Category.INVENTORY
	));

	private ReadySlotKeyMappings() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			sendClicks(client, SWAP_LEFT, ReadySlotRole.LEFT_POUCH);
			sendClicks(client, SWAP_RIGHT, ReadySlotRole.RIGHT_POUCH);
			sendClicks(client, SWAP_SATCHEL, ReadySlotRole.SATCHEL);
			while (OPEN_EDITOR.consumeClick()) {
				if (client.player != null && client.level != null && client.screen == null) {
					client.setScreen(new ReadySlotConfigScreen());
				}
			}
		});
	}

	private static KeyMapping register(String translationKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
			translationKey,
			InputConstants.Type.KEYSYM,
			UNBOUND,
			KeyMapping.Category.INVENTORY
		));
	}

	private static void sendClicks(Minecraft client, KeyMapping key, ReadySlotRole role) {
		while (key.consumeClick()) {
			// Deliberately permit another menu to be open. The server remains authoritative and
			// ReadySlotSwapService synchronizes through the always-present container ID 0 menu.
			if (client.player != null
				&& client.level != null
				&& ClientPlayNetworking.canSend(SwapReadySlotPayload.TYPE)) {
				ClientPlayNetworking.send(new SwapReadySlotPayload(role));
			}
		}
	}
}
