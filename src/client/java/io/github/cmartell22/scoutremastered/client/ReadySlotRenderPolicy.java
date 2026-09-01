package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Category;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import org.jspecify.annotations.Nullable;

/** Read-only visibility and transform category selection for ready-slot item stacks. */
final class ReadySlotRenderPolicy {
	private ReadySlotRenderPolicy() {
	}

	static @Nullable Category category(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		ReadySlotPresentationConfig config = ReadySlotConfig.current();
		String itemId = itemId(stack);
		if (config.itemBlacklist().stream().anyMatch(selector -> selectorMatches(stack, itemId, selector))) {
			return null;
		}
		Category explicitlyWhitelisted = config.whitelistedCategory(itemId).orElse(null);
		if (explicitlyWhitelisted != null) {
			return explicitlyWhitelisted;
		}
		Category tagWhitelisted = config.itemWhitelist().entrySet().stream()
			.filter(entry -> ReadySlotPresentationConfig.isTagSelector(entry.getKey()))
			.sorted(java.util.Map.Entry.comparingByKey())
			.filter(entry -> selectorMatches(stack, itemId, entry.getKey()))
			.map(java.util.Map.Entry::getValue)
			.findFirst()
			.orElse(null);
		if (tagWhitelisted != null) {
			return tagWhitelisted;
		}
		Category builtIn = builtInCategory(stack);
		return builtIn != null && config.categoryEnabled(builtIn) ? builtIn : null;
	}

	static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static boolean selectorMatches(ItemStack stack, String itemId, String selector) {
		if (!ReadySlotPresentationConfig.isTagSelector(selector)) {
			return itemId.equals(selector);
		}
		Identifier identifier = Identifier.tryParse(selector.substring(1));
		return identifier != null && stack.is(TagKey.create(Registries.ITEM, identifier));
	}

	static @Nullable Category builtInCategory(ItemStack stack) {
		if (stack.getItem() instanceof BowItem) {
			return Category.BOW;
		}
		if (stack.getItem() instanceof CrossbowItem) {
			return Category.CROSSBOW;
		}
		if (stack.getItem() instanceof ShieldItem) {
			return Category.SHIELD;
		}
		if (stack.getItem() instanceof TridentItem) {
			return Category.TRIDENT;
		}
		if (stack.is(ItemTags.SWORDS)) {
			return Category.SWORD;
		}
		if (stack.is(ItemTags.AXES)) {
			return Category.AXE;
		}
		if (stack.is(ItemTags.PICKAXES)) {
			return Category.PICKAXE;
		}
		if (stack.is(ItemTags.SHOVELS)) {
			return Category.SHOVEL;
		}
		if (stack.is(ItemTags.HOES)) {
			return Category.HOE;
		}
		if (stack.is(ItemTags.SPEARS)) {
			return Category.SPEAR;
		}
		return null;
	}
}
