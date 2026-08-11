package com.example.scout26.client;

import com.example.scout26.IntegratedInventoryAckPayload;
import com.example.scout26.IntegratedInventoryData;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.TrinketsIntegration;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Activates only the client prediction mirrors that match server-derived capacities. */
final class IntegratedInventoryClientNetworking {
	private IntegratedInventoryClientNetworking() {
	}

	static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(IntegratedInventoryAckPayload.TYPE, (payload, context) -> {
			if (IntegratedInventoryConfig.enabled()
				&& context.client().screen instanceof InventoryScreen
				&& !context.player().hasInfiniteMaterials()
				&& context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				IntegratedInventoryData data = payload.data();
				menu.scout26$activateClient(TrinketsIntegration.findEquippedBags(context.player()), data);
				InventoryScreen screen = (InventoryScreen)context.client().screen;
				// Rebuild only for the late-acknowledgement case that changes horizontal placement: an
				// already-open wide recipe book plus an active left pouch. Other layouts are already positioned.
				if (data.leftPouchCapacity() > 0
					&& screen instanceof TrinketScreen trinketScreen
					&& trinketScreen.trinkets$isRecipeBookOpen()
					&& !trinketScreen.trinkets$isNarrow()) {
					screen.resize(screen.width, screen.height);
				}
			} else if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				menu.scout26$deactivateIntegratedInventory();
			}
		});
	}
}
