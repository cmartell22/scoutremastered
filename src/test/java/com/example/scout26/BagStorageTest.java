package com.example.scout26;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BagStorageTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void capacitiesAndDefaultComponentsComeFromBagItems() {
		assertEquals(9, ModItems.SATCHEL.capacity());
		assertEquals(18, ModItems.UPGRADED_SATCHEL.capacity());
		assertEquals(3, ModItems.POUCH.capacity());
		assertEquals(6, ModItems.UPGRADED_POUCH.capacity());
		assertEquals(1, new ItemStack(ModItems.SATCHEL).getMaxStackSize());
		assertEquals(BagContents.EMPTY, new ItemStack(ModItems.SATCHEL).get(ModDataComponents.BAG_CONTENTS));
	}

	@Test
	void componentSnapshotsDoNotAliasMutableItemStacks() {
		ItemStack source = new ItemStack(Items.DIAMOND, 12);
		BagContents contents = BagContents.fromItems(List.of(source), 9);

		source.setCount(1);
		assertEquals(12, contents.getStack(0).getCount());

		ItemStack extracted = contents.getStack(0);
		extracted.setCount(2);
		assertEquals(12, contents.getStack(0).getCount());
	}

	@Test
	void containerMutationsPersistAndInputStacksAreCopied() {
		ItemStack bag = new ItemStack(ModItems.POUCH);
		BagContainer container = new BagContainer(bag);
		ItemStack source = new ItemStack(Items.DIAMOND, 32);

		container.setItem(0, source);
		source.setCount(1);
		assertEquals(32, bag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());

		container.getItem(0).shrink(2);
		container.setChanged();
		assertEquals(30, bag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());

		ItemStack removed = container.removeItem(0, 5);
		assertEquals(5, removed.getCount());
		assertEquals(25, bag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
	}

	@Test
	void vanillaBagCopiesShareOnlyImmutableValuesAndDivergeOnMutation() {
		ItemStack originalBag = new ItemStack(ModItems.SATCHEL);
		BagContainer originalContainer = new BagContainer(originalBag);
		originalContainer.setItem(0, new ItemStack(Items.EMERALD, 7));

		ItemStack copiedBag = originalBag.copy();
		assertSame(
			originalBag.get(ModDataComponents.BAG_CONTENTS),
			copiedBag.get(ModDataComponents.BAG_CONTENTS)
		);

		BagContainer copiedContainer = new BagContainer(copiedBag);
		copiedContainer.removeItem(0, 2);

		assertEquals(7, originalBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertEquals(5, copiedBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertNotSame(
			originalBag.get(ModDataComponents.BAG_CONTENTS),
			copiedBag.get(ModDataComponents.BAG_CONTENTS)
		);
	}

	@Test
	void nestedBagsAreRejectedAtEveryWriteBoundary() {
		ItemStack outerBag = new ItemStack(ModItems.SATCHEL);
		ItemStack innerBag = new ItemStack(ModItems.POUCH);
		BagContainer container = new BagContainer(outerBag);

		assertFalse(container.canPlaceItem(0, innerBag));
		assertThrows(IllegalArgumentException.class, () -> container.setItem(0, innerBag));
		assertTrue(BagContents.fromItems(List.of(innerBag), 9).isEmpty());
	}

	@Test
	void corruptAndOutOfCapacityEntriesNormalizeWithoutCrashing() {
		String json = """
			{
			  "format_version": 1,
			  "entries": [
			    {"slot": -1, "item": {"id": "minecraft:emerald"}},
			    {"slot": 1, "item": {"id": "minecraft:diamond", "count": 3}},
			    {"slot": 2, "item": {"id": "scout26:satchel"}},
			    {"slot": 5, "item": {"id": "minecraft:gold_ingot", "count": 4}},
			    {"slot": 300, "item": {"id": "minecraft:apple"}}
			  ]
			}
			""";
		BagContents decoded = BagContents.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
		assertEquals(2, decoded.entryCount());

		ItemStack pouch = new ItemStack(ModItems.POUCH);
		pouch.set(ModDataComponents.BAG_CONTENTS, decoded);
		BagContainer container = new BagContainer(pouch);

		assertEquals(3, container.capacity());
		assertTrue(container.getItem(0).isEmpty());
		assertEquals(3, container.getItem(1).getCount());
		assertEquals(1, pouch.get(ModDataComponents.BAG_CONTENTS).entryCount());
	}

	@Test
	void oversizedEntriesClampToTheItemsActualMaximum() {
		ItemStack oversized = new ItemStack(Items.DIAMOND, 99);
		BagContents fromRuntimeStack = BagContents.fromItems(List.of(oversized), 9);
		assertEquals(64, fromRuntimeStack.getStack(0).getCount());

		String json = """
			{
			  "format_version": 1,
			  "entries": [
			    {"slot": 0, "item": {"id": "minecraft:diamond", "count": 99}}
			  ]
			}
			""";
		BagContents decoded = BagContents.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
		assertEquals(64, decoded.getStack(0).getCount());

		ItemStack bag = new ItemStack(ModItems.SATCHEL);
		bag.set(ModDataComponents.BAG_CONTENTS, decoded);
		assertEquals(64, new BagContainer(bag).getItem(0).getCount());
	}

	@Test
	void streamDecodeClampsHugeCountsAndRejectsNonPositiveCounts() {
		RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
		try {
			ByteBufCodecs.VAR_INT.encode(buffer, BagContents.FORMAT_VERSION);
			ByteBufCodecs.VAR_INT.encode(buffer, 2);

			ByteBufCodecs.VAR_INT.encode(buffer, 0);
			ItemStackTemplate.STREAM_CODEC.encode(buffer, new ItemStackTemplate(Items.DIAMOND, Integer.MAX_VALUE));

			ByteBufCodecs.VAR_INT.encode(buffer, 1);
			ItemStackTemplate.STREAM_CODEC.encode(buffer, new ItemStackTemplate(Items.EMERALD, -1));

			BagContents decoded = BagContents.STREAM_CODEC.decode(buffer);
			assertEquals(1, decoded.entryCount());
			assertEquals(64, decoded.getStack(0).getCount());
			assertTrue(decoded.getStack(1).isEmpty());
		} finally {
			buffer.release();
		}
	}

	@Test
	void unknownFormatVersionsFailClosedToEmpty() {
		String json = """
			{"format_version": 999, "entries": [{"slot": 0, "item": {"id": "minecraft:diamond"}}]}
			""";
		BagContents decoded = BagContents.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
		assertEquals(BagContents.EMPTY, decoded);
	}

	@Test
	void itemStackCodecRoundTripPersistsBagContents() {
		ItemStack bag = new ItemStack(ModItems.UPGRADED_POUCH);
		BagContainer container = new BagContainer(bag);
		container.setItem(4, new ItemStack(Items.APPLE, 6));

		RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
		Tag encoded = ItemStack.CODEC.encodeStart(ops, bag).getOrThrow();
		ItemStack decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();

		assertSame(ModItems.UPGRADED_POUCH, decoded.getItem());
		assertEquals(6, decoded.get(ModDataComponents.BAG_CONTENTS).getStack(4).getCount());
	}

	@Test
	void streamCodecRoundTripSynchronizesBagContents() {
		BagContents original = BagContents.fromItems(List.of(new ItemStack(Items.APPLE, 4)), 9);
		RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
		try {
			assertFalse(ModDataComponents.BAG_CONTENTS.isTransient());
			ModDataComponents.BAG_CONTENTS.streamCodec().encode(buffer, original);
			BagContents decoded = ModDataComponents.BAG_CONTENTS.streamCodec().decode(buffer);
			assertEquals(original, decoded);
		} finally {
			buffer.release();
		}
	}
}
