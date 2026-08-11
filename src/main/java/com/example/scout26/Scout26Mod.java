package com.example.scout26;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint for the temporary Scout26 project.
 */
public final class Scout26Mod implements ModInitializer {
	public static final String MOD_ID = "scout26";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModDataComponents.initialize();
		ModItems.initialize();
		ModMenus.initialize();
		TrinketsIntegration.initialize();
		PackNetworking.initialize();
		LOGGER.info("Scout26 initialized");
	}
}
