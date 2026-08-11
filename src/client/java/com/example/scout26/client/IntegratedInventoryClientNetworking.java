package com.example.scout26.client;

import com.example.scout26.IntegratedInventoryAckPayload;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.TrinketsIntegration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Activates only the client prediction mirrors that match server-derived capacities. */
final class IntegratedInventoryClientNetworking {
	private IntegratedInventoryClientNetworking() {
	}

	static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(IntegratedInventoryAckPayload.TYPE, (payload, context) -> {
			if (context.client().screen instanceof InventoryScreen
				&& !context.player().hasInfiniteMaterials()
				&& context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				menu.scout26$activateClient(TrinketsIntegration.findEquippedBags(context.player()), payload.data());
			} else if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				menu.scout26$deactivateIntegratedInventory();
			}
		});
	}
}
