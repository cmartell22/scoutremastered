package com.example.scout26;

import net.minecraft.world.entity.player.Player;

/** Narrow API implemented only by the ADR-012 {@code InventoryMenu} Mixin. */
public interface IntegratedInventoryMenu {
	void scout26$activateServer(TrinketsIntegration.EquippedBags bags);

	void scout26$activateClient(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data);

	void scout26$finalizeClientBindings(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data);

	void scout26$deactivateIntegratedInventory();

	IntegratedInventoryData scout26$integratedInventoryData();

	boolean scout26$hasActiveIntegratedInventory();

	boolean scout26$isIntegratedSessionValid(Player player);
}
