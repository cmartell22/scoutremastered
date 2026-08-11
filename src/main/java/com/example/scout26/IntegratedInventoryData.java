package com.example.scout26;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Server-derived layout metadata; it never carries bag contents. */
public record IntegratedInventoryData(int satchelCapacity, int leftPouchCapacity, int rightPouchCapacity) {
	public static final IntegratedInventoryData EMPTY = new IntegratedInventoryData(0, 0, 0);
	public static final StreamCodec<RegistryFriendlyByteBuf, IntegratedInventoryData> STREAM_CODEC = StreamCodec.of(
		(buffer, data) -> {
			buffer.writeVarInt(data.satchelCapacity);
			buffer.writeVarInt(data.leftPouchCapacity);
			buffer.writeVarInt(data.rightPouchCapacity);
		},
		buffer -> new IntegratedInventoryData(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())
	);

	public IntegratedInventoryData {
		validate(satchelCapacity, IntegratedInventoryRole.SATCHEL);
		validate(leftPouchCapacity, IntegratedInventoryRole.LEFT_POUCH);
		validate(rightPouchCapacity, IntegratedInventoryRole.RIGHT_POUCH);
	}

	public int capacity(IntegratedInventoryRole role) {
		return switch (role) {
			case SATCHEL -> this.satchelCapacity;
			case LEFT_POUCH -> this.leftPouchCapacity;
			case RIGHT_POUCH -> this.rightPouchCapacity;
		};
	}

	public boolean hasAnyBag() {
		return this.satchelCapacity > 0 || this.leftPouchCapacity > 0 || this.rightPouchCapacity > 0;
	}

	public static IntegratedInventoryData from(TrinketsIntegration.EquippedBags bags) {
		return new IntegratedInventoryData(
			capacity(bags.satchel()),
			capacity(bags.leftPouch()),
			capacity(bags.rightPouch())
		);
	}

	private static int capacity(java.util.Optional<EquippedBagHandle> handle) {
		return handle.flatMap(EquippedBagHandle::resolve)
			.filter(stack -> stack.getItem() instanceof BagItem)
			.map(stack -> ((BagItem)stack.getItem()).capacity())
			.orElse(0);
	}

	private static void validate(int capacity, IntegratedInventoryRole role) {
		if (capacity < 0 || capacity > role.maximumCapacity()) {
			throw new IllegalArgumentException(role + " capacity outside 0.." + role.maximumCapacity() + ": " + capacity);
		}
	}
}
