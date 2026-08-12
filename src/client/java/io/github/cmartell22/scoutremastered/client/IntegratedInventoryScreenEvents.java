package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.CloseIntegratedInventoryPayload;
import io.github.cmartell22.scoutremastered.IntegratedInventoryData;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import io.github.cmartell22.scoutremastered.OpenIntegratedInventoryPayload;
import io.github.cmartell22.scoutremastered.TrinketsIntegration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Fabric ScreenEvents lifecycle and panel rendering required by ADR-012. */
final class IntegratedInventoryScreenEvents {
	private static final Set<Screen> SESSIONS_STARTED = Collections.newSetFromMap(new WeakHashMap<>());

	private IntegratedInventoryScreenEvents() {
	}

	static void initialize() {
		ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventoryScreen)
				|| !IntegratedInventoryConfig.enabled()
				|| client.player == null
				|| client.player.hasInfiniteMaterials()
				|| !(inventoryScreen.getMenu() instanceof IntegratedInventoryMenu menu)
				|| menu.scoutremastered$hasActiveIntegratedInventory()) {
				return;
			}
			// Equipped Trinkets are already synchronized before the screen opens. Seed only their
			// dimensions so vanilla lays out its first frame correctly; the server still exclusively
			// authorizes activation and sends every bag stack.
			menu.scoutremastered$previewClientLayout(IntegratedInventoryData.from(
				TrinketsIntegration.findEquippedBags(client.player)
			));
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventoryScreen)
				|| !IntegratedInventoryConfig.enabled()
				|| client.player == null
				|| client.player.hasInfiniteMaterials()) {
				return;
			}
			// Reapply combined-surface centering after a window or GUI-scale resize.
			IntegratedInventoryPanels.reposition(inventoryScreen);
			// Fabric recreates every per-screen event object before both init and resize. Always
			// register against the current objects, while sending the session-open intent only once.
			ScreenEvents.afterBackground(screen).register((ignored, graphics, mouseX, mouseY, tickDelta) ->
				IntegratedInventoryPanels.render(inventoryScreen, graphics)
			);
			ScreenEvents.remove(screen).register(ignored -> {
				IntegratedInventoryClientNetworking.endSession();
				if (client.player != null && client.player.inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scoutremastered$deactivateIntegratedInventory();
				}
				if (client.getConnection() != null && ClientPlayNetworking.canSend(CloseIntegratedInventoryPayload.TYPE)) {
					ClientPlayNetworking.send(CloseIntegratedInventoryPayload.INSTANCE);
				}
			});
			if (!SESSIONS_STARTED.add(screen)) {
				return;
			}
			if (ClientPlayNetworking.canSend(OpenIntegratedInventoryPayload.TYPE)) {
				IntegratedInventoryClientNetworking.beginSession();
				ClientPlayNetworking.send(OpenIntegratedInventoryPayload.INSTANCE);
			}
		});
	}
}
