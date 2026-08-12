package io.github.cmartell22.scoutremastered;

import net.minecraft.world.item.ItemStack;

/** Shared deterministic player-to-bag quick-move policy. */
public final class BagQuickMove {
	private BagQuickMove() {
	}

	public static boolean moveToBags(ItemStack stack, int startSlot, int endSlot, SlotMover mover) {
		return BagStorageRules.canStore(stack) && mover.move(stack, startSlot, endSlot, false);
	}

	@FunctionalInterface
	public interface SlotMover {
		boolean move(ItemStack stack, int startSlot, int endSlot, boolean backwards);
	}
}
