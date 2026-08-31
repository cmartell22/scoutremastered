package io.github.cmartell22.scoutremastered;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

final class MinecraftTestBootstrap {
	private static boolean initialized;

	private MinecraftTestBootstrap() {
	}

	static synchronized void initialize() {
		if (initialized) {
			return;
		}

		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		bindItemComponents(Items.DIAMOND, 64, false);
		bindItemComponents(Items.EMERALD, 64, false);
		bindItemComponents(Items.GOLD_INGOT, 64, false);
		bindItemComponents(Items.APPLE, 64, false);
		bindDamageableItemComponents(Items.IRON_SWORD, 250);
		bindItemComponents(ModItems.SATCHEL, 1, true);
		bindItemComponents(ModItems.UPGRADED_SATCHEL, 1, true);
		bindItemComponents(ModItems.POUCH, 1, true);
		bindItemComponents(ModItems.UPGRADED_POUCH, 1, true);
		initialized = true;
	}

	@SuppressWarnings("deprecation")
	private static void bindItemComponents(Item item, int maxStackSize, boolean bag) {
		DataComponentMap.Builder components = DataComponentMap.builder()
			.addAll(DataComponents.COMMON_ITEM_COMPONENTS)
			.set(DataComponents.MAX_STACK_SIZE, maxStackSize)
			.set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()))
			.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item));
		if (bag) {
			components.set(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY);
		}
		item.builtInRegistryHolder().bindComponents(components.build());
	}

	@SuppressWarnings("deprecation")
	private static void bindDamageableItemComponents(Item item, int maxDamage) {
		DataComponentMap components = DataComponentMap.builder()
			.addAll(DataComponents.COMMON_ITEM_COMPONENTS)
			.set(DataComponents.MAX_STACK_SIZE, 1)
			.set(DataComponents.MAX_DAMAGE, maxDamage)
			.set(DataComponents.DAMAGE, 0)
			.set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()))
			.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item))
			.build();
		item.builtInRegistryHolder().bindComponents(components);
	}
}
