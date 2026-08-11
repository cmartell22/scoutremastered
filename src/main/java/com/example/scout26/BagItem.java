package com.example.scout26;

import java.util.Objects;
import net.minecraft.world.item.Item;

/**
 * A physical bag item whose storage capacity is defined by the item, never by serialized contents.
 */
public final class BagItem extends Item {
	private final int capacity;
	private final BagEquipmentRole equipmentRole;

	public BagItem(Properties properties, int capacity, BagEquipmentRole equipmentRole) {
		super(properties);
		if (capacity < 1 || capacity > BagContents.MAX_SERIALIZED_SLOTS) {
			throw new IllegalArgumentException("Bag capacity must be between 1 and " + BagContents.MAX_SERIALIZED_SLOTS);
		}
		this.capacity = capacity;
		this.equipmentRole = Objects.requireNonNull(equipmentRole, "equipmentRole");
	}

	public int capacity() {
		return this.capacity;
	}

	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}
}
