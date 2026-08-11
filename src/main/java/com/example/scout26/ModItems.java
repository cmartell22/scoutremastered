package com.example.scout26;

import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Bag item definitions and their item-derived capacities.
 */
public final class ModItems {
	public static final BagItem SATCHEL = registerBag("satchel", 9, BagEquipmentRole.SATCHEL);
	public static final BagItem UPGRADED_SATCHEL = registerBag("upgraded_satchel", 18, BagEquipmentRole.SATCHEL);
	public static final BagItem POUCH = registerBag("pouch", 3, BagEquipmentRole.POUCH);
	public static final BagItem UPGRADED_POUCH = registerBag("upgraded_pouch", 6, BagEquipmentRole.POUCH);

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(SATCHEL);
			entries.accept(UPGRADED_SATCHEL);
			entries.accept(POUCH);
			entries.accept(UPGRADED_POUCH);
		});
	}

	private static BagItem registerBag(String path, int capacity, BagEquipmentRole equipmentRole) {
		Item.Properties properties = new Item.Properties()
			.stacksTo(1)
			.component(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY);
		return register(path, itemProperties -> new BagItem(itemProperties, capacity, equipmentRole), properties);
	}

	private static <T extends Item> T register(String path, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Scout26Mod.MOD_ID, path));
		return Registry.register(BuiltInRegistries.ITEM, itemKey, factory.apply(properties.setId(itemKey)));
	}
}
