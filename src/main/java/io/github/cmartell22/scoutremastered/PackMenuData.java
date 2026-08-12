package io.github.cmartell22.scoutremastered;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Server-to-client layout metadata for the unified pack menu.
 *
 * <p>These capacities create matching client-side synchronized slots only. The server derives its
 * containers again from live equipped ItemStacks and never accepts this data from the client.</p>
 */
public record PackMenuData(int satchelCapacity, int leftPouchCapacity, int rightPouchCapacity) {
	public static final int MAX_SATCHEL_CAPACITY = 18;
	public static final int MAX_POUCH_CAPACITY = 6;
	public static final StreamCodec<ByteBuf, PackMenuData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		PackMenuData::satchelCapacity,
		ByteBufCodecs.VAR_INT,
		PackMenuData::leftPouchCapacity,
		ByteBufCodecs.VAR_INT,
		PackMenuData::rightPouchCapacity,
		PackMenuData::new
	);

	public PackMenuData {
		validateCapacity("satchel", satchelCapacity, MAX_SATCHEL_CAPACITY);
		validateCapacity("left pouch", leftPouchCapacity, MAX_POUCH_CAPACITY);
		validateCapacity("right pouch", rightPouchCapacity, MAX_POUCH_CAPACITY);
	}

	public static PackMenuData from(TrinketsIntegration.EquippedBags bags) {
		return new PackMenuData(
			capacity(bags.satchel()),
			capacity(bags.leftPouch()),
			capacity(bags.rightPouch())
		);
	}

	public int bagSlotCount() {
		return this.satchelCapacity + this.leftPouchCapacity + this.rightPouchCapacity;
	}

	public boolean hasAnyBag() {
		return this.bagSlotCount() > 0;
	}

	private static int capacity(java.util.Optional<EquippedBagHandle> handle) {
		return handle.map(EquippedBagHandle::resolve)
			.flatMap(resolved -> resolved)
			.map(ItemStack::getItem)
			.filter(BagItem.class::isInstance)
			.map(BagItem.class::cast)
			.map(BagItem::capacity)
			.orElse(0);
	}

	private static void validateCapacity(String name, int capacity, int maximum) {
		if (capacity < 0 || capacity > maximum) {
			throw new IllegalArgumentException(name + " capacity " + capacity + " outside 0.." + maximum);
		}
	}
}
