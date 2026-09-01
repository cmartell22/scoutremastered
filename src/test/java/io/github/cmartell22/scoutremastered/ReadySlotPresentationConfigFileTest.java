package io.github.cmartell22.scoutremastered;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Category;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfigFile.Status;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadySlotPresentationConfigFileTest {
	private static final Path DEFAULTS = Path.of(
		"src/client/resources/assets/scoutremastered/config/ready-slots-default.json"
	);

	@TempDir
	Path temporaryDirectory;

	@Test
	void missingAndCorruptFilesReturnTheCompleteFallbackWithoutRewriting() throws IOException {
		ReadySlotPresentationConfig fallback = defaults();
		Path external = this.temporaryDirectory.resolve("scoutremastered-ready-slots.json");

		var missing = ReadySlotPresentationConfigFile.load(external, fallback);
		assertEquals(Status.MISSING, missing.status());
		assertSame(fallback, missing.config());

		String corrupt = "{ definitely-not-valid-json";
		Files.writeString(external, corrupt);
		var invalid = ReadySlotPresentationConfigFile.load(external, fallback);
		assertEquals(Status.INVALID, invalid.status());
		assertSame(fallback, invalid.config());
		assertNotNull(invalid.failure());
		assertEquals(corrupt, Files.readString(external));
	}

	@Test
	void validExternalFileReplacesTheFallbackAsOneCompleteValue() throws IOException {
		ReadySlotPresentationConfig fallback = defaults();
		JsonObject externalJson = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		externalJson.getAsJsonObject("render_policy").getAsJsonArray("enabled_categories").remove(1);
		Path external = this.temporaryDirectory.resolve("scoutremastered-ready-slots.json");
		Files.writeString(external, externalJson.toString());

		var loaded = ReadySlotPresentationConfigFile.load(external, fallback);
		assertEquals(Status.EXTERNAL, loaded.status());
		assertNotSame(fallback, loaded.config());
		assertFalse(loaded.config().categoryEnabled(Category.BOW));
		assertTrue(fallback.categoryEnabled(Category.BOW));
	}

	@Test
	void firstRunCreationCopiesTheBaselineAndNeverOverwrites() throws IOException {
		Path external = this.temporaryDirectory.resolve("nested").resolve("scoutremastered-ready-slots.json");
		String baselineJson = Files.readString(DEFAULTS);

		ReadySlotPresentationConfigFile.createDefault(external, baselineJson);
		assertEquals(baselineJson, Files.readString(external));
		assertThrows(
			FileAlreadyExistsException.class,
			() -> ReadySlotPresentationConfigFile.createDefault(external, "replacement")
		);
		assertEquals(baselineJson, Files.readString(external));
	}

	@Test
	void rs7aSaveReplacesTheWholeFileAndLeavesNoTemporaryArtifact() throws IOException {
		Path external = this.temporaryDirectory.resolve("nested").resolve("scoutremastered-ready-slots.json");
		Files.createDirectories(external.getParent());
		Files.writeString(external, "old contents");
		ReadySlotPresentationConfig edited = defaults()
			.withCategoryEnabled(Category.BOW, false)
			.withWhitelistedItem("example:wand", Category.HANDHELD);

		ReadySlotPresentationConfigFile.save(external, edited);

		assertEquals(edited.toJson(), Files.readString(external));
		var loaded = ReadySlotPresentationConfigFile.load(external, defaults());
		assertEquals(Status.EXTERNAL, loaded.status());
		assertFalse(loaded.config().categoryEnabled(Category.BOW));
		assertEquals(Category.HANDHELD, loaded.config().whitelistedCategory("example:wand").orElseThrow());
		try (var files = Files.list(external.getParent())) {
			assertEquals(List.of(external), files.toList());
		}
	}

	private static ReadySlotPresentationConfig defaults() throws IOException {
		return ReadySlotPresentationConfig.parse(new StringReader(Files.readString(DEFAULTS)));
	}
}
