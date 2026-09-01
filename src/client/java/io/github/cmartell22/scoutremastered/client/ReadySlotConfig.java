package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfigFile;
import io.github.cmartell22.scoutremastered.ScoutRemasteredMod;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Client-only loader for Ready Slots presentation settings. */
final class ReadySlotConfig {
	private static final String DEFAULT_RESOURCE = "/assets/scoutremastered/config/ready-slots-default.json";
	private static final String EXTERNAL_FILENAME = "scoutremastered-ready-slots.json";
	private static volatile ReadySlotPresentationConfig current = ReadySlotPresentationConfig.disabled();

	private ReadySlotConfig() {
	}

	static void load() {
		Bundled bundled = loadBundled();
		current = bundled.config();
		if (bundled.json() == null) {
			return;
		}

		Path path = FabricLoader.getInstance().getConfigDir().resolve(EXTERNAL_FILENAME);
		ReadySlotPresentationConfigFile.LoadResult result = ReadySlotPresentationConfigFile.load(path, bundled.config());
		switch (result.status()) {
			case EXTERNAL -> {
				current = result.config();
				ScoutRemasteredMod.LOGGER.info("Loaded Ready Slots presentation config {}", path);
			}
			case MISSING -> writeDefault(path, bundled.json());
			case NOT_REGULAR -> ScoutRemasteredMod.LOGGER.warn(
				"Ready Slots config {} is not a regular file; using bundled defaults",
				path
			);
			case INVALID -> ScoutRemasteredMod.LOGGER.warn(
				"Could not read valid Ready Slots config {}; using bundled defaults and leaving the file untouched",
				path,
				result.failure()
			);
		}
	}

	static ReadySlotPresentationConfig current() {
		return current;
	}

	private static Bundled loadBundled() {
		try (InputStream input = ReadySlotConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
			if (input == null) {
				ScoutRemasteredMod.LOGGER.error(
					"Bundled Ready Slots config {} is missing; Ready Slots rendering is disabled",
					DEFAULT_RESOURCE
				);
				return new Bundled(ReadySlotPresentationConfig.disabled(), null);
			}
			String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			try (Reader reader = new StringReader(json)) {
				return new Bundled(ReadySlotPresentationConfig.parse(reader), json);
			}
		} catch (IOException | RuntimeException exception) {
			ScoutRemasteredMod.LOGGER.error(
				"Bundled Ready Slots config {} is invalid; Ready Slots rendering is disabled",
				DEFAULT_RESOURCE,
				exception
			);
			return new Bundled(ReadySlotPresentationConfig.disabled(), null);
		}
	}

	private static void writeDefault(Path path, String json) {
		try {
			ReadySlotPresentationConfigFile.createDefault(path, json);
			ScoutRemasteredMod.LOGGER.info("Created default Ready Slots presentation config {}", path);
		} catch (IOException exception) {
			ScoutRemasteredMod.LOGGER.warn(
				"Could not create default Ready Slots config {}; bundled defaults remain active",
				path,
				exception
			);
		}
	}

	private record Bundled(ReadySlotPresentationConfig config, String json) {
	}
}
