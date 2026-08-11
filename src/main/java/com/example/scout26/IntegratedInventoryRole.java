package com.example.scout26;

/** Fixed P7-B role order and maxima from ADR-012. */
public enum IntegratedInventoryRole {
	SATCHEL(BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX, 18),
	LEFT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_INDEX, 6),
	RIGHT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_INDEX, 6);

	private final BagEquipmentRole equipmentRole;
	private final int equipmentIndex;
	private final int maximumCapacity;

	IntegratedInventoryRole(BagEquipmentRole equipmentRole, int equipmentIndex, int maximumCapacity) {
		this.equipmentRole = equipmentRole;
		this.equipmentIndex = equipmentIndex;
		this.maximumCapacity = maximumCapacity;
	}

	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}

	public int equipmentIndex() {
		return this.equipmentIndex;
	}

	public int maximumCapacity() {
		return this.maximumCapacity;
	}
}
