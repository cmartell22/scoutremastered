package io.github.cmartell22.scoutremastered;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One fixed integrated-inventory role binding.
 *
 * <p>Server bindings delegate immediately to {@link BagContainer}. Client bindings are only a
 * synchronized prediction mirror. Both retain the exact captured {@link EquippedBagHandle}.</p>
 */
public final class IntegratedBagContainer implements Container {
	private final IntegratedInventoryRole role;
	private final NonNullList<ItemStack> clientItems;
	private EquippedBagHandle handle;
	private BagContainer serverContainer;
	private BooleanSupplier sessionValid = () -> false;
	private int activeCapacity;

	public IntegratedBagContainer(IntegratedInventoryRole role) {
		this.role = Objects.requireNonNull(role, "role");
		this.clientItems = NonNullList.withSize(role.maximumCapacity(), ItemStack.EMPTY);
	}

	public boolean bindServer(EquippedBagHandle handle, BooleanSupplier sessionValid) {
		this.deactivate();
		if (!this.accepts(handle)) {
			return false;
		}
		BagContainer container = new BagContainer(handle);
		this.handle = handle;
		this.serverContainer = container;
		this.sessionValid = Objects.requireNonNull(sessionValid, "sessionValid");
		this.activeCapacity = container.capacity();
		return this.isBindingValid();
	}

	public boolean bindClient(EquippedBagHandle handle, int serverCapacity, BooleanSupplier sessionValid) {
		this.deactivate();
		return this.rebindClient(handle, serverCapacity, sessionValid);
	}

	/** Replaces only the stale exact handle after a full sync, retaining its synchronized slot mirror. */
	public boolean rebindClient(EquippedBagHandle handle, int serverCapacity, BooleanSupplier sessionValid) {
		if (!this.accepts(handle) || serverCapacity <= 0 || serverCapacity > this.role.maximumCapacity()) {
			this.deactivate();
			return false;
		}
		ItemStack stack = handle.resolve().orElse(ItemStack.EMPTY);
		if (!(stack.getItem() instanceof BagItem bagItem) || bagItem.capacity() != serverCapacity) {
			this.deactivate();
			return false;
		}
		this.handle = handle;
		this.serverContainer = null;
		this.sessionValid = Objects.requireNonNull(sessionValid, "sessionValid");
		this.activeCapacity = serverCapacity;
		return this.isBindingValid();
	}

	public void deactivate() {
		this.handle = null;
		this.serverContainer = null;
		this.sessionValid = () -> false;
		this.activeCapacity = 0;
		this.clientItems.clear();
	}

	public int activeCapacity() {
		return this.isBindingValid() ? this.activeCapacity : 0;
	}

	public boolean isSlotActive(int slot) {
		return this.isPresentationBindingUsable() && slot >= 0 && slot < this.activeCapacity;
	}

	public boolean isBindingValid() {
		return this.handle != null && this.handle.isValid() && this.sessionValid.getAsBoolean();
	}

	@Override
	public int getContainerSize() {
		return this.role.maximumCapacity();
	}

	@Override
	public boolean isEmpty() {
		if (!this.isPresentationBindingUsable()) {
			return true;
		}
		for (int slot = 0; slot < this.activeCapacity; slot++) {
			if (!this.getItem(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (!this.isSlotActive(slot)) {
			return ItemStack.EMPTY;
		}
		return this.serverContainer != null ? this.serverContainer.getItem(slot) : this.clientItems.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (!this.isSlotActive(slot) || amount <= 0) {
			return ItemStack.EMPTY;
		}
		if (this.serverContainer != null) {
			return this.serverContainer.removeItem(slot, amount);
		}
		return ContainerHelper.removeItem(this.clientItems, slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (!this.isSlotActive(slot)) {
			return ItemStack.EMPTY;
		}
		if (this.serverContainer != null) {
			return this.serverContainer.removeItemNoUpdate(slot);
		}
		return ContainerHelper.takeItem(this.clientItems, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!this.isSlotActive(slot) || !stack.isEmpty() && !BagStorageRules.canStore(stack)) {
			return;
		}
		if (this.serverContainer != null) {
			this.serverContainer.setItem(slot, stack);
		} else {
			ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
			if (!stored.isEmpty()) {
				stored.limitSize(this.getMaxStackSize(stored));
			}
			this.clientItems.set(slot, stored);
		}
	}

	@Override
	public void setChanged() {
		if (this.isBindingValid() && this.serverContainer != null) {
			this.serverContainer.setChanged();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.isPresentationBindingUsable()
			&& (this.serverContainer == null || this.serverContainer.stillValid(player));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return this.isSlotActive(slot)
			&& BagStorageRules.canStore(stack)
			&& (this.serverContainer == null || this.serverContainer.canPlaceItem(slot, stack));
	}

	@Override
	public void clearContent() {
		if (!this.isPresentationBindingUsable()) {
			return;
		}
		if (this.serverContainer != null) {
			this.serverContainer.clearContent();
		} else {
			this.clientItems.clear();
		}
	}

	/**
	 * Keeps a client prediction mirror visible while an equipment-component update invalidates its
	 * captured handle and the server refreshes it. Authoritative containers still require the exact
	 * captured stack identity and therefore continue to fail closed.
	 */
	private boolean isPresentationBindingUsable() {
		return this.handle != null
			&& this.sessionValid.getAsBoolean()
			&& (this.serverContainer == null || this.handle.isValid());
	}

	private boolean accepts(EquippedBagHandle candidate) {
		return candidate != null
			&& candidate.equipmentRole() == this.role.equipmentRole()
			&& candidate.slotIndex() == this.role.equipmentIndex()
			&& candidate.resolve()
				.filter(stack -> stack.getItem() instanceof BagItem bagItem
					&& bagItem.capacity() <= this.role.maximumCapacity())
				.isPresent();
	}
}
