package com.example.scout26;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary P0 bootstrap entrypoint. Gameplay and storage logic begin in later phases.
 */
public final class Scout26Mod implements ModInitializer {
	public static final String MOD_ID = "scout26";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Scout26 bootstrap initialized");
	}
}

