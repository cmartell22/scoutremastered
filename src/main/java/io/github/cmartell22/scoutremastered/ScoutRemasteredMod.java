package io.github.cmartell22.scoutremastered;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint for Scout Remastered.
 */
public final class ScoutRemasteredMod implements ModInitializer {
	public static final String MOD_ID = "scoutremastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModDataComponents.initialize();
		ModItems.initialize();
		ModMenus.initialize();
		TrinketsIntegration.initialize();
		PackNetworking.initialize();
		ReadySlotNetworking.initialize();
		IntegratedInventoryNetworking.initialize();
		LOGGER.info("ScoutRemastered initialized");
	}
}
