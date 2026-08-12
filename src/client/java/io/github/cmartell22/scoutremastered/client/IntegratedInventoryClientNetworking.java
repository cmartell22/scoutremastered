package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.IntegratedInventoryAckPayload;
import io.github.cmartell22.scoutremastered.IntegratedInventoryData;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import io.github.cmartell22.scoutremastered.IntegratedInventoryReadyPayload;
import io.github.cmartell22.scoutremastered.IntegratedInventoryRefreshTracker;
import io.github.cmartell22.scoutremastered.OpenIntegratedInventoryPayload;
import io.github.cmartell22.scoutremastered.TrinketsIntegration;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Activates only the client prediction mirrors that match server-derived capacities. */
final class IntegratedInventoryClientNetworking {
	private static final IntegratedInventoryRefreshTracker REFRESH_TRACKER = new IntegratedInventoryRefreshTracker();
	private static boolean horizontalResizePending;

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
			IntegratedInventoryData expected = menu.scoutremastered$clientLayoutData();
			IntegratedInventoryData active = menu.scoutremastered$integratedInventoryData();
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
				IntegratedInventoryData previousLayout = menu.scoutremastered$clientLayoutData();
				horizontalResizePending = previousLayout.leftPouchCapacity() != data.leftPouchCapacity()
					|| previousLayout.rightPouchCapacity() != data.rightPouchCapacity();
				TrinketsIntegration.EquippedBags equippedBags = TrinketsIntegration.findEquippedBags(context.player());
				if (menu.scoutremastered$hasActiveIntegratedInventory()) {
					// A refresh ACK precedes the full-state packet. Rebind without clearing the existing
					// mirror so the already-rendered bag cannot blink out for that intervening frame.
					menu.scoutremastered$finalizeClientBindings(equippedBags, data);
				} else {
					menu.scoutremastered$activateClient(equippedBags, data);
				}
				IntegratedInventoryPanels.reposition((InventoryScreen)context.client().screen);
			} else {
				if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scoutremastered$deactivateIntegratedInventory();
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
				menu.scoutremastered$finalizeClientBindings(equippedBags, data);
				REFRESH_TRACKER.completeRequest(
					data.equals(menu.scoutremastered$integratedInventoryData())
						&& data.equals(IntegratedInventoryData.from(equippedBags))
				);
				InventoryScreen screen = (InventoryScreen)context.client().screen;
				// A normal open was seeded before init and needs no second layout pass. Rebuild only when
				// the acknowledgement corrected that seed or an in-screen equipment change altered width.
				if (horizontalResizePending
					&& screen instanceof TrinketScreen trinketScreen
					&& trinketScreen.trinkets$isRecipeBookOpen()
					&& !trinketScreen.trinkets$isNarrow()) {
					screen.resize(screen.width, screen.height);
				}
				horizontalResizePending = false;
			} else {
				if (context.player().inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scoutremastered$deactivateIntegratedInventory();
				}
				endSession();
			}
		});
	}

	static void beginSession() {
		horizontalResizePending = false;
		REFRESH_TRACKER.beginSession();
	}

	static void endSession() {
		horizontalResizePending = false;
		REFRESH_TRACKER.endSession();
	}
}
