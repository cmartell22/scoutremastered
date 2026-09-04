package io.github.cmartell22.scoutremastered;

/** Fixed P7-B role order and maxima from ADR-012. */
public enum IntegratedInventoryRole {
	SATCHEL(BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX, 18),
	LEFT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX, 6),
	RIGHT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX, 6);

	private final BagEquipmentRole equipmentRole;
	private final String slotId;
	private final int equipmentIndex;
	private final int maximumCapacity;

	IntegratedInventoryRole(BagEquipmentRole equipmentRole, String slotId, int equipmentIndex, int maximumCapacity) {
		this.equipmentRole = equipmentRole;
		this.slotId = slotId;
		this.equipmentIndex = equipmentIndex;
		this.maximumCapacity = maximumCapacity;
	}

	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}

	public String slotId() {
		return this.slotId;
	}

	public int equipmentIndex() {
		return this.equipmentIndex;
	}

	public int maximumCapacity() {
		return this.maximumCapacity;
	}
}
