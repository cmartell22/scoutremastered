package com.example.scout26.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Temporary client-only bootstrap entrypoint. It intentionally has no common/server references.
 */
public final class Scout26Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Client features are introduced in later phases.
	}
}

