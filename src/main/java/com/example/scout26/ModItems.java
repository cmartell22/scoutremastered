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
 * P1 item definitions. These are plain, non-storage items until P2 adds bag contents.
 */
public final class ModItems {
	public static final Item SATCHEL = register("satchel", Item::new, new Item.Properties().stacksTo(1));
	public static final Item UPGRADED_SATCHEL = register("upgraded_satchel", Item::new, new Item.Properties().stacksTo(1));
	public static final Item POUCH = register("pouch", Item::new, new Item.Properties().stacksTo(1));
	public static final Item UPGRADED_POUCH = register("upgraded_pouch", Item::new, new Item.Properties().stacksTo(1));

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

	private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Scout26Mod.MOD_ID, path));
		return Registry.register(BuiltInRegistries.ITEM, itemKey, factory.apply(properties.setId(itemKey)));
	}
}
