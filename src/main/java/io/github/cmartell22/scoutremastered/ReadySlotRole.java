package io.github.cmartell22.scoutremastered;

import eu.pb4.trinkets.api.DefaultTrinketSlots;
import java.util.Optional;

/** Stable gameplay roles for the three bag-local ready slots. */
public enum ReadySlotRole {
	LEFT_POUCH(BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.LEFT_POUCH_INDEX),
	RIGHT_POUCH(BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.RIGHT_POUCH_INDEX),
	SATCHEL(BagEquipmentRole.SATCHEL, DefaultTrinketSlots.CHEST_BACK, TrinketsIntegration.SATCHEL_INDEX);

	private final BagEquipmentRole equipmentRole;
	private final String slotId;
	private final int slotIndex;

	ReadySlotRole(BagEquipmentRole equipmentRole, String slotId, int slotIndex) {
		this.equipmentRole = equipmentRole;
		this.slotId = slotId;
		this.slotIndex = slotIndex;
	}

	Optional<EquippedBagHandle> select(TrinketsIntegration.EquippedBags bags) {
		return switch (this) {
			case LEFT_POUCH -> bags.leftPouch();
			case RIGHT_POUCH -> bags.rightPouch();
			case SATCHEL -> bags.satchel();
		};
	}

	boolean matches(EquippedBagHandle handle) {
		return handle.equipmentRole() == this.equipmentRole
			&& handle.slotId().equals(this.slotId)
			&& handle.slotIndex() == this.slotIndex;
	}
}
