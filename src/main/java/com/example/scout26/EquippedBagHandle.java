package com.example.scout26;

import eu.pb4.trinkets.api.TrinketSlotReference;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

/**
 * A fail-closed locator for one physical bag in one Trinkets slot.
 *
 * <p>Resolution re-queries the live slot and requires the exact captured ItemStack object. If the
 * slot inventory is rebuilt or its stack is replaced, this handle becomes stale instead of writing
 * into either the old stack or an unrelated replacement.</p>
 */
public final class EquippedBagHandle {
	private final String slotId;
	private final int slotIndex;
	private final BagEquipmentRole equipmentRole;
	private final Supplier<ItemStack> currentStackResolver;
	private final ItemStack expectedStack;

	private EquippedBagHandle(
		String slotId,
		int slotIndex,
		BagEquipmentRole equipmentRole,
		Supplier<ItemStack> currentStackResolver,
		ItemStack expectedStack
	) {
		this.slotId = slotId;
		this.slotIndex = slotIndex;
		this.equipmentRole = equipmentRole;
		this.currentStackResolver = currentStackResolver;
		this.expectedStack = expectedStack;
	}

	static Optional<EquippedBagHandle> capture(
		String slotId,
		int slotIndex,
		BagEquipmentRole equipmentRole,
		Supplier<ItemStack> currentStackResolver
	) {
		Objects.requireNonNull(slotId, "slotId");
		Objects.requireNonNull(equipmentRole, "equipmentRole");
		Objects.requireNonNull(currentStackResolver, "currentStackResolver");
		if (slotIndex < 0 || !TrinketsIntegration.isAllowedSlot(equipmentRole, slotId)) {
			return Optional.empty();
		}

		ItemStack currentStack = Objects.requireNonNullElse(currentStackResolver.get(), ItemStack.EMPTY);
		if (!TrinketsIntegration.isBagForRole(currentStack, equipmentRole)) {
			return Optional.empty();
		}
		return Optional.of(new EquippedBagHandle(
			slotId,
			slotIndex,
			equipmentRole,
			currentStackResolver,
			currentStack
		));
	}

	public Optional<ItemStack> resolve() {
		ItemStack currentStack = Objects.requireNonNullElse(this.currentStackResolver.get(), ItemStack.EMPTY);
		if (currentStack != this.expectedStack
			|| !TrinketsIntegration.isBagForRole(currentStack, this.equipmentRole)) {
			return Optional.empty();
		}
		return Optional.of(currentStack);
	}

	public boolean isValid() {
		return this.resolve().isPresent();
	}

	public TrinketSlotReference reference() {
		return new TrinketSlotReference(this.slotId, this.slotIndex);
	}

	public String slotId() {
		return this.slotId;
	}

	public int slotIndex() {
		return this.slotIndex;
	}

	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}
}
