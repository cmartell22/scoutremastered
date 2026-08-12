package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.ScoutRemasteredMod;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Persisted client flag. The accepted P7-B prototype is deliberately disabled by default. */
public final class IntegratedInventoryConfig {
	private static final String ENABLED_KEY = "integrated_inventory_enabled";
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("scoutremastered-client.properties");
	private static boolean enabled;

	private IntegratedInventoryConfig() {
	}

	public static void load() {
		Properties properties = new Properties();
		if (Files.isRegularFile(PATH)) {
			try (InputStream input = Files.newInputStream(PATH)) {
				properties.load(input);
			} catch (IOException exception) {
				ScoutRemasteredMod.LOGGER.warn("Could not read {}; Integrated Inventory remains disabled", PATH, exception);
			}
		}
		enabled = Boolean.parseBoolean(properties.getProperty(ENABLED_KEY, "false"));
		if (!Files.exists(PATH)) {
			properties.setProperty(ENABLED_KEY, "false");
			try {
				Files.createDirectories(PATH.getParent());
				try (OutputStream output = Files.newOutputStream(PATH)) {
					properties.store(output, "ScoutRemastered client options; restart the client after changing this file.");
				}
			} catch (IOException exception) {
				ScoutRemasteredMod.LOGGER.warn("Could not create default client config {}", PATH, exception);
			}
		}
	}

	public static boolean enabled() {
		return enabled;
	}
}
