package com.example.scout26;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared slot policy for every Scout26 bag-storage surface. */
public final class BagStorageSlot extends Slot {
	public BagStorageSlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return BagStorageRules.canStore(stack) && this.container.canPlaceItem(this.getContainerSlot(), stack);
	}

	@Override
	public void set(ItemStack stack) {
		if (stack.isEmpty() || this.mayPlace(stack)) {
			super.set(stack);
		}
	}

	@Override
	public boolean mayPickup(Player player) {
		return this.container.stillValid(player);
	}

	@Override
	public boolean isActive() {
		return !(this.container instanceof IntegratedBagContainer integrated)
			|| integrated.isSlotActive(this.getContainerSlot());
	}
}
