package com.example.scout26;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Logical-server container adapter over one live physical bag ItemStack.
 *
 * <p>The mutable working list is never stored in the component. Every mutation replaces the bag's
 * immutable {@link BagContents} value immediately.</p>
 */
public final class BagContainer implements Container {
	private final ItemStack bagStack;
	private final BagItem bagItem;
	private final NonNullList<ItemStack> items;

	public BagContainer(ItemStack bagStack) {
		if (bagStack.isEmpty() || !(bagStack.getItem() instanceof BagItem bagItem)) {
			throw new IllegalArgumentException("BagContainer requires a non-empty BagItem stack");
		}

		this.bagStack = bagStack;
		this.bagItem = bagItem;
		BagContents stored = bagStack.getOrDefault(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY);
		BagContents normalized = stored.normalized(bagItem.capacity());
		this.items = normalized.copyItems(bagItem.capacity());
		if (!normalized.equals(stored)) {
			this.bagStack.set(ModDataComponents.BAG_CONTENTS, normalized);
		}
	}

	@Override
	public int getContainerSize() {
		return this.bagItem.capacity();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (!this.isValidSlot(slot) || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
		if (!removed.isEmpty()) {
			this.persist();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (!this.isValidSlot(slot)) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = ContainerHelper.takeItem(this.items, slot);
		if (!removed.isEmpty()) {
			this.persist();
		}
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.checkSlot(slot);
		if (!stack.isEmpty() && !BagStorageRules.canStore(stack)) {
			throw new IllegalArgumentException("Bag items and stacks carrying bag contents cannot be nested");
		}

		ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!stored.isEmpty()) {
			stored.limitSize(this.getMaxStackSize(stored));
		}
		this.items.set(slot, stored);
		this.persist();
	}

	@Override
	public void setChanged() {
		this.persist();
	}

	@Override
	public boolean stillValid(Player player) {
		return !this.bagStack.isEmpty() && this.bagStack.getItem() == this.bagItem;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return this.isValidSlot(slot) && BagStorageRules.canStore(stack);
	}

	@Override
	public void clearContent() {
		this.items.clear();
		this.persist();
	}

	public int capacity() {
		return this.bagItem.capacity();
	}

	private void persist() {
		for (int slot = 0; slot < this.items.size(); slot++) {
			ItemStack stack = this.items.get(slot);
			if (stack.isEmpty() || !BagStorageRules.canStore(stack)) {
				this.items.set(slot, ItemStack.EMPTY);
			} else {
				stack.limitSize(this.getMaxStackSize(stack));
				if (stack.isEmpty()) {
					this.items.set(slot, ItemStack.EMPTY);
				}
			}
		}
		this.bagStack.set(
			ModDataComponents.BAG_CONTENTS,
			BagContents.fromItems(this.items, this.bagItem.capacity())
		);
	}

	private boolean isValidSlot(int slot) {
		return slot >= 0 && slot < this.items.size();
	}

	private void checkSlot(int slot) {
		if (!this.isValidSlot(slot)) {
			throw new IndexOutOfBoundsException("Bag slot " + slot + " outside capacity " + this.items.size());
		}
	}
}
