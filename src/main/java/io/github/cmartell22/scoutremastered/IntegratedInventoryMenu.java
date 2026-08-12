package io.github.cmartell22.scoutremastered;

import net.minecraft.world.entity.player.Player;

/** Narrow API implemented only by the ADR-012 {@code InventoryMenu} Mixin. */
public interface IntegratedInventoryMenu {
	void scoutremastered$activateServer(TrinketsIntegration.EquippedBags bags);

	void scoutremastered$activateClient(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data);

	void scoutremastered$previewClientLayout(IntegratedInventoryData data);

	void scoutremastered$finalizeClientBindings(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data);

	void scoutremastered$deactivateIntegratedInventory();

	IntegratedInventoryData scoutremastered$integratedInventoryData();

	IntegratedInventoryData scoutremastered$clientLayoutData();

	boolean scoutremastered$hasActiveIntegratedInventory();

	boolean scoutremastered$isIntegratedSessionValid(Player player);
}
