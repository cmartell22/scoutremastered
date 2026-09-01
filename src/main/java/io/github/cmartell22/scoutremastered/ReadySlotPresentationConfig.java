package io.github.cmartell22.scoutremastered;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable, side-effect-free Ready Slots presentation settings.
 *
 * <p>This value is safe to parse and test from common code, but only the client entrypoint loads or
 * applies it. It contains no inventory, networking, bag ownership, or persistent gameplay state.</p>
 */
public final class ReadySlotPresentationConfig {
	public static final int SCHEMA_VERSION = 1;

	/** Shared parser/RS7A control bounds; generous near the model but reject absurd values. */
	public static final float MIN_TRANSLATION = -4.0F;
	public static final float MAX_TRANSLATION = 4.0F;
	public static final float MIN_ROTATION = -360.0F;
	public static final float MAX_ROTATION = 360.0F;
	public static final float MIN_SCALE = 0.01F;
	public static final float MAX_SCALE = 8.0F;
	private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private final Set<Category> enabledCategories;
	private final Map<String, Category> itemWhitelist;
	private final Set<String> itemBlacklist;
	private final Map<Position, Transform> baseTransforms;
	private final Map<Category, Map<Position, TransformPatch>> categoryOverrides;
	private final Map<String, Map<Position, TransformPatch>> itemOverrides;

	private ReadySlotPresentationConfig(
		Set<Category> enabledCategories,
		Map<String, Category> itemWhitelist,
		Set<String> itemBlacklist,
		Map<Position, Transform> baseTransforms,
		Map<Category, Map<Position, TransformPatch>> categoryOverrides,
		Map<String, Map<Position, TransformPatch>> itemOverrides
	) {
		this.enabledCategories = immutableEnumSet(enabledCategories);
		this.itemWhitelist = Collections.unmodifiableMap(new LinkedHashMap<>(itemWhitelist));
		this.itemBlacklist = Collections.unmodifiableSet(new LinkedHashSet<>(itemBlacklist));
		this.baseTransforms = immutableEnumMap(Position.class, baseTransforms);
		this.categoryOverrides = immutableNestedEnumMap(Category.class, categoryOverrides);
		this.itemOverrides = immutableItemOverrides(itemOverrides);
	}

	public static ReadySlotPresentationConfig parse(Reader reader) {
		JsonElement rootElement = JsonParser.parseReader(reader);
		JsonObject root = requireObject(rootElement, "root");
		requireOnlyKeys(
			root,
			"root",
			Set.of("schema_version", "render_policy", "base_transforms", "category_overrides", "item_overrides")
		);
		int schemaVersion = requireInt(root, "schema_version", "root");
		if (schemaVersion != SCHEMA_VERSION) {
			throw error("Unsupported schema_version " + schemaVersion + "; expected " + SCHEMA_VERSION);
		}

		JsonObject renderPolicy = requireObject(root, "render_policy", "root");
		requireOnlyKeys(
			renderPolicy,
			"render_policy",
			Set.of("enabled_categories", "item_whitelist", "item_blacklist")
		);
		Set<Category> enabledCategories = parseCategories(
			requireArray(renderPolicy, "enabled_categories", "render_policy"),
			"render_policy.enabled_categories"
		);
		Map<String, Category> itemWhitelist = parseWhitelist(
			requireObject(renderPolicy, "item_whitelist", "render_policy")
		);
		Set<String> itemBlacklist = parseItemIds(
			requireArray(renderPolicy, "item_blacklist", "render_policy"),
			"render_policy.item_blacklist"
		);

		Map<Position, Transform> baseTransforms = parseBaseTransforms(
			requireObject(root, "base_transforms", "root")
		);
		Map<Category, Map<Position, TransformPatch>> categoryOverrides = parseCategoryOverrides(
			requireObject(root, "category_overrides", "root")
		);
		Map<String, Map<Position, TransformPatch>> itemOverrides = parseItemOverrides(
			requireObject(root, "item_overrides", "root")
		);

		return new ReadySlotPresentationConfig(
			enabledCategories,
			itemWhitelist,
			itemBlacklist,
			baseTransforms,
			categoryOverrides,
			itemOverrides
		);
	}

	/** Fail-closed emergency value used only if the bundled baseline itself is unavailable or invalid. */
	public static ReadySlotPresentationConfig disabled() {
		Map<Position, Transform> transforms = new EnumMap<>(Position.class);
		for (Position position : Position.values()) {
			transforms.put(position, new Transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F));
		}
		return new ReadySlotPresentationConfig(
			EnumSet.noneOf(Category.class),
			Map.of(),
			Set.of(),
			transforms,
			Map.of(),
			Map.of()
		);
	}

	public boolean categoryEnabled(Category category) {
		return this.enabledCategories.contains(category);
	}

	public Optional<Category> whitelistedCategory(String itemId) {
		return Optional.ofNullable(this.itemWhitelist.get(itemId));
	}

	public boolean itemBlacklisted(String itemId) {
		return this.itemBlacklist.contains(itemId);
	}

	/** Resolves base position, then category patch, then item patch. */
	public Transform resolve(Position position, Category category, String itemId) {
		Transform resolved = this.baseTransforms.get(position);
		Map<Position, TransformPatch> categoryPatches = this.categoryOverrides.get(category);
		if (categoryPatches != null) {
			TransformPatch patch = categoryPatches.get(position);
			if (patch != null) {
				resolved = patch.apply(resolved);
			}
		}
		Map<Position, TransformPatch> itemPatches = this.itemOverrides.get(itemId);
		if (itemPatches != null) {
			TransformPatch patch = itemPatches.get(position);
			if (patch != null) {
				resolved = patch.apply(resolved);
			}
		}
		return resolved;
	}

	private static Map<Position, Transform> parseBaseTransforms(JsonObject object) {
		requireOnlyKeys(object, "base_transforms", positionIds());
		Map<Position, Transform> result = new EnumMap<>(Position.class);
		for (Position position : Position.values()) {
			JsonObject transform = requireObject(object, position.id(), "base_transforms");
			result.put(position, parseTransform(transform, "base_transforms." + position.id()));
		}
		return result;
	}

	private static Map<Category, Map<Position, TransformPatch>> parseCategoryOverrides(JsonObject object) {
		Map<Category, Map<Position, TransformPatch>> result = new EnumMap<>(Category.class);
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			Category category = Category.fromId(entry.getKey());
			result.put(category, parsePositionPatches(
				requireObject(entry.getValue(), "category_overrides." + entry.getKey()),
				"category_overrides." + entry.getKey()
			));
		}
		return result;
	}

	private static Map<String, Map<Position, TransformPatch>> parseItemOverrides(JsonObject object) {
		Map<String, Map<Position, TransformPatch>> result = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			String itemId = requireItemId(entry.getKey(), "item_overrides");
			result.put(itemId, parsePositionPatches(
				requireObject(entry.getValue(), "item_overrides." + itemId),
				"item_overrides." + itemId
			));
		}
		return result;
	}

	private static Map<Position, TransformPatch> parsePositionPatches(JsonObject object, String path) {
		if (object.isEmpty()) {
			throw error(path + " must contain at least one position override");
		}
		Map<Position, TransformPatch> result = new EnumMap<>(Position.class);
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			Position position = Position.fromId(entry.getKey());
			result.put(position, parsePatch(
				requireObject(entry.getValue(), path + "." + entry.getKey()),
				path + "." + entry.getKey()
			));
		}
		return result;
	}

	private static Map<String, Category> parseWhitelist(JsonObject object) {
		Map<String, Category> result = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			String itemId = requireItemId(entry.getKey(), "render_policy.item_whitelist");
			result.put(itemId, Category.fromId(requireString(entry.getValue(), "item_whitelist." + itemId)));
		}
		return result;
	}

	private static Set<Category> parseCategories(JsonArray array, String path) {
		Set<Category> result = EnumSet.noneOf(Category.class);
		for (int index = 0; index < array.size(); index++) {
			result.add(Category.fromId(requireString(array.get(index), path + "[" + index + "]")));
		}
		return result;
	}

	private static Set<String> parseItemIds(JsonArray array, String path) {
		Set<String> result = new LinkedHashSet<>();
		for (int index = 0; index < array.size(); index++) {
			result.add(requireItemId(requireString(array.get(index), path + "[" + index + "]"), path));
		}
		return result;
	}

	private static Transform parseTransform(JsonObject object, String path) {
		requireOnlyKeys(object, path, transformFieldNames());
		return new Transform(
			requireFloat(object, "translate_x", path),
			requireFloat(object, "translate_y", path),
			requireFloat(object, "translate_z", path),
			requireFloat(object, "rotate_x", path),
			requireFloat(object, "rotate_y", path),
			requireFloat(object, "rotate_z", path),
			requireFloat(object, "scale", path)
		);
	}

	private static TransformPatch parsePatch(JsonObject object, String path) {
		requireOnlyKeys(object, path, transformFieldNames());
		return new TransformPatch(
			optionalFloat(object, "translate_x", path),
			optionalFloat(object, "translate_y", path),
			optionalFloat(object, "translate_z", path),
			optionalFloat(object, "rotate_x", path),
			optionalFloat(object, "rotate_y", path),
			optionalFloat(object, "rotate_z", path),
			optionalFloat(object, "scale", path)
		);
	}

	private static JsonObject requireObject(JsonObject parent, String key, String path) {
		if (!parent.has(key)) {
			throw error("Missing object " + path + "." + key);
		}
		return requireObject(parent.get(key), path + "." + key);
	}

	private static JsonObject requireObject(JsonElement element, String path) {
		if (element == null || !element.isJsonObject()) {
			throw error(path + " must be an object");
		}
		return element.getAsJsonObject();
	}

	private static JsonArray requireArray(JsonObject parent, String key, String path) {
		JsonElement element = parent.get(key);
		if (element == null || !element.isJsonArray()) {
			throw error(path + "." + key + " must be an array");
		}
		return element.getAsJsonArray();
	}

	private static int requireInt(JsonObject parent, String key, String path) {
		JsonElement element = parent.get(key);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw error(path + "." + key + " must be an integer");
		}
		try {
			return new BigDecimal(element.getAsString()).intValueExact();
		} catch (ArithmeticException | NumberFormatException exception) {
			throw error(path + "." + key + " must be an integer", exception);
		}
	}

	private static String requireString(JsonElement element, String path) {
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw error(path + " must be a string");
		}
		return element.getAsString();
	}

	private static float requireFloat(JsonObject parent, String key, String path) {
		Float value = optionalFloat(parent, key, path);
		if (value == null) {
			throw error("Missing number " + path + "." + key);
		}
		return value;
	}

	private static Float optionalFloat(JsonObject parent, String key, String path) {
		JsonElement element = parent.get(key);
		if (element == null) {
			return null;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw error(path + "." + key + " must be a number");
		}
		float value = element.getAsFloat();
		if (!Float.isFinite(value)) {
			throw error(path + "." + key + " must be finite");
		}
		return value;
	}

	private static String requireItemId(String itemId, String path) {
		if (!ITEM_ID.matcher(itemId).matches()) {
			throw error(path + " contains invalid item id " + itemId);
		}
		return itemId;
	}

	private static void requireOnlyKeys(JsonObject object, String path, Set<String> allowedKeys) {
		for (String key : object.keySet()) {
			if (!allowedKeys.contains(key)) {
				throw error(path + " contains unknown field " + key);
			}
		}
	}

	private static Set<String> positionIds() {
		Set<String> ids = new LinkedHashSet<>();
		for (Position position : Position.values()) {
			ids.add(position.id());
		}
		return ids;
	}

	private static Set<String> transformFieldNames() {
		return Set.of(
			"translate_x",
			"translate_y",
			"translate_z",
			"rotate_x",
			"rotate_y",
			"rotate_z",
			"scale"
		);
	}

	private static JsonParseException error(String message) {
		return new JsonParseException(message);
	}

	private static JsonParseException error(String message, Throwable cause) {
		return new JsonParseException(message, cause);
	}

	private static <E extends Enum<E>, V> Map<E, V> immutableEnumMap(Class<E> keyType, Map<E, V> source) {
		Map<E, V> copy = new EnumMap<>(keyType);
		copy.putAll(source);
		return Collections.unmodifiableMap(copy);
	}

	private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> source) {
		if (source.isEmpty()) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(EnumSet.copyOf(source));
	}

	private static <E extends Enum<E>> Map<E, Map<Position, TransformPatch>> immutableNestedEnumMap(
		Class<E> keyType,
		Map<E, Map<Position, TransformPatch>> source
	) {
		Map<E, Map<Position, TransformPatch>> copy = new EnumMap<>(keyType);
		for (Map.Entry<E, Map<Position, TransformPatch>> entry : source.entrySet()) {
			copy.put(entry.getKey(), immutableEnumMap(Position.class, entry.getValue()));
		}
		return Collections.unmodifiableMap(copy);
	}

	private static Map<String, Map<Position, TransformPatch>> immutableItemOverrides(
		Map<String, Map<Position, TransformPatch>> source
	) {
		Map<String, Map<Position, TransformPatch>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Map<Position, TransformPatch>> entry : source.entrySet()) {
			copy.put(entry.getKey(), immutableEnumMap(Position.class, entry.getValue()));
		}
		return Collections.unmodifiableMap(copy);
	}

	public enum Category {
		HANDHELD("handheld"),
		BOW("bow"),
		CROSSBOW("crossbow"),
		SHIELD("shield"),
		TRIDENT("trident");

		private final String id;

		Category(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		static Category fromId(String id) {
			for (Category category : values()) {
				if (category.id.equals(id)) {
					return category;
				}
			}
			throw error("Unknown ready-slot category " + id);
		}
	}

	public enum Position {
		LEFT_HIP("left_hip"),
		RIGHT_HIP("right_hip"),
		BACK("back");

		private final String id;

		Position(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		static Position fromId(String id) {
			for (Position position : values()) {
				if (position.id.equals(id)) {
					return position;
				}
			}
			throw error("Unknown ready-slot position " + id);
		}
	}

	public record Transform(
		float translateX,
		float translateY,
		float translateZ,
		float rotateX,
		float rotateY,
		float rotateZ,
		float scale
	) {
		public Transform {
			validateTranslation(translateX, "translate_x");
			validateTranslation(translateY, "translate_y");
			validateTranslation(translateZ, "translate_z");
			validateRotation(rotateX, "rotate_x");
			validateRotation(rotateY, "rotate_y");
			validateRotation(rotateZ, "rotate_z");
			validateScale(scale);
		}
	}

	public record TransformPatch(
		Float translateX,
		Float translateY,
		Float translateZ,
		Float rotateX,
		Float rotateY,
		Float rotateZ,
		Float scale
	) {
		public TransformPatch {
			validateOptionalTranslation(translateX, "translate_x");
			validateOptionalTranslation(translateY, "translate_y");
			validateOptionalTranslation(translateZ, "translate_z");
			validateOptionalRotation(rotateX, "rotate_x");
			validateOptionalRotation(rotateY, "rotate_y");
			validateOptionalRotation(rotateZ, "rotate_z");
			if (scale != null) {
				validateScale(scale);
			}
			if (translateX == null && translateY == null && translateZ == null
				&& rotateX == null && rotateY == null && rotateZ == null && scale == null) {
				throw new IllegalArgumentException("Transform patch must contain at least one field");
			}
		}

		Transform apply(Transform base) {
			return new Transform(
				this.translateX != null ? this.translateX : base.translateX(),
				this.translateY != null ? this.translateY : base.translateY(),
				this.translateZ != null ? this.translateZ : base.translateZ(),
				this.rotateX != null ? this.rotateX : base.rotateX(),
				this.rotateY != null ? this.rotateY : base.rotateY(),
				this.rotateZ != null ? this.rotateZ : base.rotateZ(),
				this.scale != null ? this.scale : base.scale()
			);
		}
	}

	private static void validateOptionalTranslation(Float value, String name) {
		if (value != null) {
			validateTranslation(value, name);
		}
	}

	private static void validateOptionalRotation(Float value, String name) {
		if (value != null) {
			validateRotation(value, name);
		}
	}

	private static void validateTranslation(float value, String name) {
		if (!Float.isFinite(value) || value < MIN_TRANSLATION || value > MAX_TRANSLATION) {
			throw new IllegalArgumentException(
				name + " must be finite and within " + MIN_TRANSLATION + ".." + MAX_TRANSLATION
			);
		}
	}

	private static void validateRotation(float value, String name) {
		if (!Float.isFinite(value) || value < MIN_ROTATION || value > MAX_ROTATION) {
			throw new IllegalArgumentException(
				name + " must be finite and within " + MIN_ROTATION + ".." + MAX_ROTATION
			);
		}
	}

	private static void validateScale(float value) {
		if (!Float.isFinite(value) || value < MIN_SCALE || value > MAX_SCALE) {
			throw new IllegalArgumentException("scale must be finite and within " + MIN_SCALE + ".." + MAX_SCALE);
		}
	}
}
