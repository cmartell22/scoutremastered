package com.example.scout26.client;

import com.example.scout26.IntegratedInventoryAckPayload;
import com.example.scout26.IntegratedInventoryData;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.IntegratedInventoryReadyPayload;
import com.example.scout26.IntegratedInventoryRefreshTracker;
import com.example.scout26.OpenIntegratedInventoryPayload;
import com.example.scout26.TrinketsIntegration;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Activates only the client prediction mirrors that match server-derived capacities. */
final class IntegratedInventoryClientNetworking {
	private static final IntegratedInventoryRefreshTracker REFRESH_TRACKER = new IntegratedInventoryRefreshTracker();

	private IntegratedInventoryClientNetworking() {
	}

	static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!(client.screen instanceof InventoryScreen)
				|| client.player == null
				|| client.player.hasInfiniteMaterials()
				|| !(client.player.inventoryMenu instanceof IntegratedInventoryMenu menu)
				|| !ClientPlayNetworking.canSend(OpenIntegratedInventoryPayload.TYPE)) {
				return;
			}
			IntegratedInventoryData expected = menu.scout26$clientLayoutData();
			IntegratedInventoryData active = menu.scout26$integratedInventoryData();
			IntegratedInventoryData equipped = IntegratedInventoryData.from(
				TrinketsIntegration.findEquippedBags(client.player)
			);
			if (REFRESH_TRACKER.shouldRequest(expected, active, equipped)) {
				ClientPlayNetworking.send(OpenIntegratedInventoryPayload.INSTANCE);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(IntegratedInventoryAckPayload.TYPE, (payload, context) -> {
			if (IntegratedInventoryConfig.enabled()
				&& context.client().screen instanceof InventoryScreen
				&& !context.player().hasInfiniteMaterials()
				&& context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				IntegratedInventoryData data = payload.data();
				menu.scout26$activateClient(TrinketsIntegration.findEquippedBags(context.player()), data);
			} else {
				if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scout26$deactivateIntegratedInventory();
				}
				endSession();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(IntegratedInventoryReadyPayload.TYPE, (payload, context) -> {
			if (IntegratedInventoryConfig.enabled()
				&& context.client().screen instanceof InventoryScreen
				&& !context.player().hasInfiniteMaterials()
				&& context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
				IntegratedInventoryData data = payload.data();
				TrinketsIntegration.EquippedBags equippedBags = TrinketsIntegration.findEquippedBags(context.player());
				menu.scout26$finalizeClientBindings(equippedBags, data);
				REFRESH_TRACKER.completeRequest(
					data.equals(menu.scout26$integratedInventoryData())
						&& data.equals(IntegratedInventoryData.from(equippedBags))
				);
				InventoryScreen screen = (InventoryScreen)context.client().screen;
				// Rebuild only for the late-acknowledgement case that changes horizontal placement: an
				// already-open wide recipe book plus an active left pouch. Other layouts are already positioned.
				if (data.leftPouchCapacity() > 0
					&& screen instanceof TrinketScreen trinketScreen
					&& trinketScreen.trinkets$isRecipeBookOpen()
					&& !trinketScreen.trinkets$isNarrow()) {
					screen.resize(screen.width, screen.height);
				}
			} else {
				if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scout26$deactivateIntegratedInventory();
				}
				endSession();
			}
		});
	}

	static void beginSession() {
		REFRESH_TRACKER.beginSession();
	}

	static void endSession() {
		REFRESH_TRACKER.endSession();
	}
}
