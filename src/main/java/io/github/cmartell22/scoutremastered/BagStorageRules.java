package io.github.cmartell22.scoutremastered;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Central fail-closed eligibility rules for every bag storage write and decode path.
 */
public final class BagStorageRules {
	private BagStorageRules() {
	}

	public static boolean canStore(ItemStack stack) {
		return !stack.isEmpty()
			&& !(stack.getItem() instanceof BagItem)
			&& !stack.has(ModDataComponents.BAG_CONTENTS);
	}

	static boolean canStore(ItemStackTemplate stack) {
		return !(stack.item().value() instanceof BagItem)
			&& stack.get(ModDataComponents.BAG_CONTENTS) == null;
	}
}
