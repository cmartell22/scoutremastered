package io.github.cmartell22.scoutremastered;

import eu.pb4.trinkets.api.DefaultTrinketSlots;
import java.util.Optional;

/** Stable gameplay roles for the three bag-local ready slots. */
public enum ReadySlotRole {
	LEFT_POUCH(0, BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.LEFT_POUCH_INDEX),
	RIGHT_POUCH(1, BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.RIGHT_POUCH_INDEX),
	SATCHEL(2, BagEquipmentRole.SATCHEL, DefaultTrinketSlots.CHEST_BACK, TrinketsIntegration.SATCHEL_INDEX);

	private final int networkId;
	private final BagEquipmentRole equipmentRole;
	private final String slotId;
	private final int slotIndex;

	ReadySlotRole(int networkId, BagEquipmentRole equipmentRole, String slotId, int slotIndex) {
		this.networkId = networkId;
		this.equipmentRole = equipmentRole;
		this.slotId = slotId;
		this.slotIndex = slotIndex;
	}

	public int networkId() {
		return this.networkId;
	}

	/** Decodes only the three locked protocol IDs; malformed IDs never fall back to a role. */
	public static ReadySlotRole fromNetworkId(int networkId) {
		return switch (networkId) {
			case 0 -> LEFT_POUCH;
			case 1 -> RIGHT_POUCH;
			case 2 -> SATCHEL;
			default -> throw new IllegalArgumentException("Unknown ready-slot role network ID: " + networkId);
		};
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
