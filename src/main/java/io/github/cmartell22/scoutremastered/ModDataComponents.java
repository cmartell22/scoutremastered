package io.github.cmartell22.scoutremastered;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * ScoutRemastered data component registrations.
 */
public final class ModDataComponents {
	public static final DataComponentType<BagContents> BAG_CONTENTS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "bag_contents"),
		DataComponentType.<BagContents>builder()
			.persistent(BagContents.CODEC)
			.networkSynchronized(BagContents.STREAM_CODEC)
			.cacheEncoding()
			.build()
	);

	private ModDataComponents() {
	}

	public static void initialize() {
		ScoutRemasteredMod.LOGGER.debug("ScoutRemastered data components registered");
	}
}
