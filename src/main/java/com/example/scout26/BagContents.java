package com.example.scout26;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Immutable, sparse bag contents stored as ItemStackTemplate values.
 *
 * <p>Every ItemStack crossing this API is snapshotted on input and recreated on output. This makes
 * component values safe to share when vanilla copies an ItemStack's component map.</p>
 */
public final class BagContents {
	public static final int FORMAT_VERSION = 1;
	public static final int MAX_SERIALIZED_SLOTS = 256;
	public static final BagContents EMPTY = new BagContents(List.of());

	private static final Codec<SlotEntry> SLOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("slot").forGetter(SlotEntry::slot),
		ItemStackTemplate.CODEC.fieldOf("item").forGetter(SlotEntry::item)
	).apply(instance, SlotEntry::new));

	private static final StreamCodec<RegistryFriendlyByteBuf, SlotEntry> SLOT_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		SlotEntry::slot,
		ItemStackTemplate.STREAM_CODEC,
		SlotEntry::item,
		SlotEntry::new
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<SlotEntry>> ENTRIES_STREAM_CODEC =
		SLOT_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SERIALIZED_SLOTS));

	public static final Codec<BagContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("format_version", FORMAT_VERSION).forGetter(contents -> FORMAT_VERSION),
		SLOT_CODEC.sizeLimitedListOf(MAX_SERIALIZED_SLOTS)
			.optionalFieldOf("entries", List.of())
			.forGetter(BagContents::entriesForSerialization)
	).apply(instance, BagContents::fromSerialized));

	public static final StreamCodec<RegistryFriendlyByteBuf, BagContents> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		contents -> FORMAT_VERSION,
		ENTRIES_STREAM_CODEC,
		BagContents::entriesForSerialization,
		BagContents::fromSerialized
	);

	private final List<SlotEntry> entries;
	private final int hashCode;

	private BagContents(List<SlotEntry> rawEntries) {
		Map<Integer, ItemStackTemplate> normalized = new TreeMap<>();
		for (SlotEntry entry : rawEntries) {
			if (entry.slot() >= 0
				&& entry.slot() < MAX_SERIALIZED_SLOTS
				&& BagStorageRules.canStore(entry.item())) {
				normalized.put(entry.slot(), entry.item());
			}
		}

		List<SlotEntry> copiedEntries = new ArrayList<>(normalized.size());
		normalized.forEach((slot, item) -> copiedEntries.add(new SlotEntry(slot, item)));
		this.entries = List.copyOf(copiedEntries);
		this.hashCode = this.entries.hashCode();
	}

	public static BagContents fromItems(List<ItemStack> items, int capacity) {
		int safeCapacity = sanitizeCapacity(capacity);
		List<SlotEntry> entries = new ArrayList<>();
		for (int slot = 0; slot < Math.min(items.size(), safeCapacity); slot++) {
			ItemStack stack = items.get(slot);
			if (BagStorageRules.canStore(stack)) {
				entries.add(new SlotEntry(slot, ItemStackTemplate.fromNonEmptyStack(stack)));
			}
		}
		return entries.isEmpty() ? EMPTY : new BagContents(entries);
	}

	private static BagContents fromSerialized(int formatVersion, List<SlotEntry> entries) {
		if (formatVersion != FORMAT_VERSION || entries.isEmpty()) {
			return EMPTY;
		}
		BagContents contents = new BagContents(entries);
		return contents.entries.isEmpty() ? EMPTY : contents;
	}

	public BagContents normalized(int capacity) {
		int safeCapacity = sanitizeCapacity(capacity);
		if (this.entries.isEmpty()) {
			return EMPTY;
		}
		if (this.entries.get(this.entries.size() - 1).slot() < safeCapacity) {
			return this;
		}

		List<SlotEntry> normalized = this.entries.stream()
			.filter(entry -> entry.slot() < safeCapacity)
			.toList();
		return normalized.isEmpty() ? EMPTY : new BagContents(normalized);
	}

	public ItemStack getStack(int slot) {
		if (slot < 0 || slot >= MAX_SERIALIZED_SLOTS) {
			return ItemStack.EMPTY;
		}
		for (SlotEntry entry : this.entries) {
			if (entry.slot() == slot) {
				return entry.item().create();
			}
			if (entry.slot() > slot) {
				break;
			}
		}
		return ItemStack.EMPTY;
	}

	NonNullList<ItemStack> copyItems(int capacity) {
		int safeCapacity = sanitizeCapacity(capacity);
		NonNullList<ItemStack> result = NonNullList.withSize(safeCapacity, ItemStack.EMPTY);
		for (SlotEntry entry : this.entries) {
			if (entry.slot() >= safeCapacity) {
				break;
			}
			result.set(entry.slot(), entry.item().create());
		}
		return result;
	}

	public int entryCount() {
		return this.entries.size();
	}

	public boolean isEmpty() {
		return this.entries.isEmpty();
	}

	private List<SlotEntry> entriesForSerialization() {
		return this.entries;
	}

	private static int sanitizeCapacity(int capacity) {
		return Math.clamp(capacity, 0, MAX_SERIALIZED_SLOTS);
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof BagContents contents && this.entries.equals(contents.entries);
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return "BagContents" + this.entries;
	}

	private record SlotEntry(int slot, ItemStackTemplate item) {
	}
}
