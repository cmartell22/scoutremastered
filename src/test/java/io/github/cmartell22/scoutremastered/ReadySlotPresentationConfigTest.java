package io.github.cmartell22.scoutremastered;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Category;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Position;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Transform;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadySlotPresentationConfigTest {
	private static final Path DEFAULTS = Path.of(
		"src/client/resources/assets/scoutremastered/config/ready-slots-default.json"
	);

	@Test
	void bundledDefaultsPreserveTheAcceptedRs5Transforms() throws IOException {
		ReadySlotPresentationConfig config = defaults();

		Transform left = config.resolve(Position.LEFT_HIP, Category.HANDHELD, "minecraft:diamond_sword");
		assertTransform(left, 0.27F, 0.72F, -0.20F, 0.0F, 0.0F, 180.0F, 0.40F);
		Transform right = config.resolve(Position.RIGHT_HIP, Category.TRIDENT, "minecraft:trident");
		assertTransform(right, -0.27F, 0.72F, -0.20F, 0.0F, 0.0F, 180.0F, 0.52F);
		Transform back = config.resolve(Position.BACK, Category.TRIDENT, "minecraft:trident");
		assertTransform(back, 0.0F, 0.12F, 0.44F, 0.0F, 180.0F, 180.0F, 0.88F);

		for (Category category : Category.values()) {
			assertTrue(config.categoryEnabled(category));
		}
	}

	@Test
	void itemPatchOverridesCategoryPatchWhichOverridesTheBase() throws IOException {
		JsonObject root = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		JsonObject categoryPatch = root.getAsJsonObject("category_overrides")
			.getAsJsonObject("handheld")
			.getAsJsonObject("left_hip");
		categoryPatch.addProperty("translate_y", 1.25F);
		categoryPatch.addProperty("scale", 0.50F);

		JsonObject itemPatch = new JsonObject();
		itemPatch.addProperty("translate_y", 1.50F);
		itemPatch.addProperty("rotate_x", 25.0F);
		itemPatch.addProperty("scale", 0.60F);
		JsonObject itemPositions = new JsonObject();
		itemPositions.add("left_hip", itemPatch);
		root.getAsJsonObject("item_overrides").add("minecraft:diamond_sword", itemPositions);

		ReadySlotPresentationConfig config = parse(root.toString());
		Transform categoryOnly = config.resolve(Position.LEFT_HIP, Category.HANDHELD, "minecraft:iron_sword");
		assertTransform(categoryOnly, 0.27F, 1.25F, -0.20F, 0.0F, 0.0F, 180.0F, 0.50F);
		Transform item = config.resolve(Position.LEFT_HIP, Category.HANDHELD, "minecraft:diamond_sword");
		assertTransform(item, 0.27F, 1.50F, -0.20F, 25.0F, 0.0F, 180.0F, 0.60F);
	}

	@Test
	void visibilityInputsRemainExplicitAndDeterministic() throws IOException {
		JsonObject root = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		JsonObject policy = root.getAsJsonObject("render_policy");
		policy.getAsJsonObject("item_whitelist").addProperty("minecraft:stick", "handheld");
		policy.getAsJsonArray("item_blacklist").add("minecraft:stick");

		ReadySlotPresentationConfig config = parse(root.toString());
		assertEquals(Category.HANDHELD, config.whitelistedCategory("minecraft:stick").orElseThrow());
		assertTrue(config.itemBlacklisted("minecraft:stick"));
		assertFalse(config.itemBlacklisted("minecraft:diamond_sword"));
	}

	@Test
	void rs7aCopyOnEditPreservesTheOpeningValueAndResolutionPrecedence() throws IOException {
		ReadySlotPresentationConfig opening = defaults();
		Transform editedTransform = new Transform(1.25F, -0.5F, 0.75F, 45.0F, -90.0F, 180.0F, 2.0F);

		ReadySlotPresentationConfig edited = opening
			.withCategoryEnabled(Category.BOW, false)
			.withWhitelistedItem("example:wand", Category.HANDHELD)
			.withBlacklistedItem("example:hidden")
			.withBaseTransform(Position.LEFT_HIP, editedTransform)
			.withCategoryTransform(Category.HANDHELD, Position.RIGHT_HIP, editedTransform)
			.withItemTransform("example:wand", Position.BACK, editedTransform);

		assertTrue(opening.categoryEnabled(Category.BOW));
		assertFalse(edited.categoryEnabled(Category.BOW));
		assertTrue(opening.whitelistedCategory("example:wand").isEmpty());
		assertEquals(Category.HANDHELD, edited.whitelistedCategory("example:wand").orElseThrow());
		assertFalse(opening.itemBlacklisted("example:hidden"));
		assertTrue(edited.itemBlacklisted("example:hidden"));
		assertEquals(editedTransform, edited.baseTransform(Position.LEFT_HIP));
		assertEquals(editedTransform, edited.resolveCategory(Position.RIGHT_HIP, Category.HANDHELD));
		assertEquals(editedTransform, edited.resolve(Position.BACK, Category.HANDHELD, "example:wand"));
		assertEquals(0.27F, opening.baseTransform(Position.LEFT_HIP).translateX(), 0.0001F);
	}

	@Test
	void rs7aSerializationIsDeterministicStableAndRoundTrips() throws IOException {
		ReadySlotPresentationConfig edited = defaults()
			.withWhitelistedItem("zeta:last", Category.TRIDENT)
			.withWhitelistedItem("alpha:first", Category.BOW)
			.withBlacklistedItem("zeta:hidden")
			.withBlacklistedItem("alpha:hidden")
			.withItemTransform(
				"zeta:last",
				Position.BACK,
				new Transform(-4.0F, 4.0F, 0.0F, -360.0F, 360.0F, 0.0F, 8.0F)
			);

		String first = edited.toJson();
		String second = edited.toJson();
		ReadySlotPresentationConfig reparsed = parse(first);
		assertEquals(first, second);
		assertEquals(first, reparsed.toJson());
		assertTrue(first.indexOf("alpha:first") < first.indexOf("zeta:last"));
		assertEquals(edited.resolve(Position.BACK, Category.TRIDENT, "zeta:last"),
			reparsed.resolve(Position.BACK, Category.TRIDENT, "zeta:last"));
	}

	@Test
	void rs7aBaseEditsPropagateThroughMaskingOverrides() throws IOException {
		ReadySlotPresentationConfig opening = defaults().withItemTransform(
			"example:wand",
			Position.LEFT_HIP,
			new Transform(1.0F, 0.5F, -0.1F, 10.0F, 20.0F, 30.0F, 0.5F)
		);
		Transform previousBase = opening.baseTransform(Position.LEFT_HIP);
		Transform updatedBase = new Transform(
			0.57F,
			previousBase.translateY(),
			previousBase.translateZ(),
			previousBase.rotateX(),
			previousBase.rotateY(),
			previousBase.rotateZ(),
			2.0F
		);

		ReadySlotPresentationConfig edited = opening.withBaseTransformPropagatingOverrides(
			Position.LEFT_HIP,
			updatedBase
		);

		assertEquals(updatedBase, edited.baseTransform(Position.LEFT_HIP));
		assertEquals(0.8F, edited.resolveCategory(Position.LEFT_HIP, Category.HANDHELD).scale(), 0.0001F);
		assertEquals(0.84F, edited.resolveCategory(Position.LEFT_HIP, Category.BOW).scale(), 0.0001F);
		assertEquals(0.4F, opening.resolveCategory(Position.LEFT_HIP, Category.HANDHELD).scale(), 0.0001F);
		Transform item = edited.resolve(Position.LEFT_HIP, Category.HANDHELD, "example:wand");
		assertEquals(1.30F, item.translateX(), 0.0001F);
		assertEquals(1.0F, item.scale(), 0.0001F);
		assertEquals(0.42F, edited.resolveCategory(Position.RIGHT_HIP, Category.BOW).scale(), 0.0001F);
	}

	@Test
	void publicBoundsPermitModelTuningButRejectAbsurdValues() throws IOException {
		assertEquals(-4.0F, ReadySlotPresentationConfig.MIN_TRANSLATION);
		assertEquals(4.0F, ReadySlotPresentationConfig.MAX_TRANSLATION);
		assertEquals(-360.0F, ReadySlotPresentationConfig.MIN_ROTATION);
		assertEquals(360.0F, ReadySlotPresentationConfig.MAX_ROTATION);
		assertEquals(0.01F, ReadySlotPresentationConfig.MIN_SCALE);
		assertEquals(8.0F, ReadySlotPresentationConfig.MAX_SCALE);

		JsonObject root = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		JsonObject left = root.getAsJsonObject("base_transforms").getAsJsonObject("left_hip");
		left.addProperty("translate_x", ReadySlotPresentationConfig.MAX_TRANSLATION);
		left.addProperty("rotate_y", ReadySlotPresentationConfig.MIN_ROTATION);
		left.addProperty("scale", ReadySlotPresentationConfig.MAX_SCALE);
		root.getAsJsonObject("category_overrides")
			.getAsJsonObject("handheld")
			.getAsJsonObject("left_hip")
			.addProperty("scale", ReadySlotPresentationConfig.MAX_SCALE);
		Transform atBounds = parse(root.toString()).resolve(
			Position.LEFT_HIP,
			Category.HANDHELD,
			"minecraft:diamond_sword"
		);
		assertEquals(ReadySlotPresentationConfig.MAX_TRANSLATION, atBounds.translateX());
		assertEquals(ReadySlotPresentationConfig.MIN_ROTATION, atBounds.rotateY());
		assertEquals(ReadySlotPresentationConfig.MAX_SCALE, atBounds.scale());

		root = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		left = root.getAsJsonObject("base_transforms").getAsJsonObject("left_hip");
		left.addProperty("translate_x", 5000.0F);
		String absurdTranslation = root.toString();
		assertThrows(IllegalArgumentException.class, () -> parse(absurdTranslation));

		root = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		left = root.getAsJsonObject("base_transforms").getAsJsonObject("left_hip");
		left.addProperty("scale", 0.0F);
		String zeroScale = root.toString();
		assertThrows(IllegalArgumentException.class, () -> parse(zeroScale));
	}

	@Test
	void corruptSchemaAndEmptyPatchesAreRejectedAsAWhole() throws IOException {
		assertThrows(RuntimeException.class, () -> parse("{not-json"));

		JsonObject wrongSchema = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		wrongSchema.addProperty("schema_version", 2);
		assertThrows(RuntimeException.class, () -> parse(wrongSchema.toString()));

		JsonObject emptyPatch = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		JsonObject positions = new JsonObject();
		positions.add("back", new JsonObject());
		emptyPatch.getAsJsonObject("item_overrides").add("minecraft:diamond_sword", positions);
		assertThrows(IllegalArgumentException.class, () -> parse(emptyPatch.toString()));

		JsonObject misspelledField = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		misspelledField.getAsJsonObject("category_overrides")
			.getAsJsonObject("handheld")
			.getAsJsonObject("left_hip")
			.addProperty("translate_zz", 0.5F);
		assertThrows(RuntimeException.class, () -> parse(misspelledField.toString()));

		JsonObject emptyItemOverride = JsonParser.parseString(Files.readString(DEFAULTS)).getAsJsonObject();
		emptyItemOverride.getAsJsonObject("item_overrides").add("minecraft:diamond_sword", new JsonObject());
		assertThrows(RuntimeException.class, () -> parse(emptyItemOverride.toString()));
	}

	private static ReadySlotPresentationConfig defaults() throws IOException {
		return parse(Files.readString(DEFAULTS));
	}

	private static ReadySlotPresentationConfig parse(String json) {
		return ReadySlotPresentationConfig.parse(new StringReader(json));
	}

	private static void assertTransform(
		Transform actual,
		float x,
		float y,
		float z,
		float rotateX,
		float rotateY,
		float rotateZ,
		float scale
	) {
		assertEquals(x, actual.translateX(), 0.0001F);
		assertEquals(y, actual.translateY(), 0.0001F);
		assertEquals(z, actual.translateZ(), 0.0001F);
		assertEquals(rotateX, actual.rotateX(), 0.0001F);
		assertEquals(rotateY, actual.rotateY(), 0.0001F);
		assertEquals(rotateZ, actual.rotateZ(), 0.0001F);
		assertEquals(scale, actual.scale(), 0.0001F);
	}
}
