package com.example.scout26.client;

import com.example.scout26.CloseIntegratedInventoryPayload;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.OpenIntegratedInventoryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Fabric ScreenEvents lifecycle and panel rendering required by ADR-012. */
final class IntegratedInventoryScreenEvents {
	private static final Set<Screen> INITIALIZED = Collections.newSetFromMap(new WeakHashMap<>());

	private IntegratedInventoryScreenEvents() {
	}

	static void initialize() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventoryScreen)
				|| !IntegratedInventoryConfig.enabled()
				|| client.player == null
				|| client.player.hasInfiniteMaterials()) {
				return;
			}
			// Reapply combined-surface centering after a window or GUI-scale resize.
			IntegratedInventoryPanels.reposition(inventoryScreen);
			if (!INITIALIZED.add(screen)) {
				return;
			}
			ScreenEvents.afterBackground(screen).register((ignored, graphics, mouseX, mouseY, tickDelta) ->
				IntegratedInventoryPanels.render(inventoryScreen, graphics)
			);
			ScreenEvents.remove(screen).register(ignored -> {
				IntegratedInventoryClientNetworking.endSession();
				if (client.player != null && client.player.inventoryMenu instanceof IntegratedInventoryMenu menu) {
					menu.scout26$deactivateIntegratedInventory();
				}
				if (client.getConnection() != null && ClientPlayNetworking.canSend(CloseIntegratedInventoryPayload.TYPE)) {
					ClientPlayNetworking.send(CloseIntegratedInventoryPayload.INSTANCE);
				}
			});
			if (ClientPlayNetworking.canSend(OpenIntegratedInventoryPayload.TYPE)) {
				IntegratedInventoryClientNetworking.beginSession();
				ClientPlayNetworking.send(OpenIntegratedInventoryPayload.INSTANCE);
			}
		});
	}
}
