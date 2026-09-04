package io.github.cmartell22.scoutremastered;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class P5HardeningTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void trinketsRebuildCopyInvalidatesOldBackingWithoutAliasingContents() {
		ItemStack original = new ItemStack(ModItems.SATCHEL);
		new BagContainer(original).setItem(0, new ItemStack(Items.DIAMOND, 7));
		AtomicReference<ItemStack> liveSlot = new AtomicReference<>(original);
		EquippedBagHandle oldHandle = captureSatchel(liveSlot);
		BagContainer oldContainer = new BagContainer(oldHandle);
		PackMenu oldMenu = serverMenu(oldHandle);

		ItemStack rebuilt = original.copy();
		liveSlot.set(rebuilt);

		assertFalse(oldHandle.isValid());
		assertFalse(oldMenu.hasValidServerBacking());
		assertTrue(oldContainer.removeItem(0, 2).isEmpty());
		assertEquals(7, original.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertEquals(7, rebuilt.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());

		EquippedBagHandle rebuiltHandle = captureSatchel(liveSlot);
		assertEquals(2, new BagContainer(rebuiltHandle).removeItem(0, 2).getCount());
		assertEquals(7, original.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertEquals(5, rebuilt.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
	}

	@Test
	void separateServerMenusCannotCrossMutatePhysicalBags() {
		ItemStack firstBag = new ItemStack(ModItems.SATCHEL);
		ItemStack secondBag = new ItemStack(ModItems.SATCHEL);
		AtomicReference<ItemStack> firstSlot = new AtomicReference<>(firstBag);
		AtomicReference<ItemStack> secondSlot = new AtomicReference<>(secondBag);
		EquippedBagHandle firstHandle = captureSatchel(firstSlot);
		EquippedBagHandle secondHandle = captureSatchel(secondSlot);
		PackMenu firstMenu = serverMenu(firstHandle);
		PackMenu secondMenu = serverMenu(secondHandle);

		firstMenu.getSlot(0).set(new ItemStack(Items.EMERALD, 5));
		secondMenu.getSlot(0).set(new ItemStack(Items.APPLE, 3));
		assertEquals(5, firstBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertEquals(3, secondBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());

		firstSlot.set(secondBag);
		assertFalse(firstMenu.hasValidServerBacking());
		firstMenu.getSlot(0).set(new ItemStack(Items.DIAMOND, 1));
		assertSame(Items.EMERALD, firstBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getItem());
		assertSame(Items.APPLE, secondBag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getItem());
	}

	@Test
	void throwStyleSlotRemovalPersistsBeforeMenuClose() {
		ItemStack bag = new ItemStack(ModItems.SATCHEL);
		new BagContainer(bag).setItem(0, new ItemStack(Items.DIAMOND, 5));
		AtomicReference<ItemStack> liveSlot = new AtomicReference<>(bag);
		PackMenu menu = serverMenu(captureSatchel(liveSlot));

		assertEquals(1, menu.getSlot(0).safeTake(1, Integer.MAX_VALUE, null).getCount());
		assertEquals(4, bag.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertEquals(4, menu.getSlot(0).safeTake(64, Integer.MAX_VALUE, null).getCount());
		assertTrue(bag.get(ModDataComponents.BAG_CONTENTS).isEmpty());
	}

	@Test
	void forgedContentCarrierIsRejectedByEveryBagBoundary() {
		ItemStack carrier = new ItemStack(Items.GOLD_INGOT);
		carrier.set(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY);
		ItemStack bag = new ItemStack(ModItems.SATCHEL);
		BagContainer container = new BagContainer(bag);

		assertFalse(BagStorageRules.canStore(carrier));
		assertFalse(container.canPlaceItem(0, carrier));
		assertThrows(IllegalArgumentException.class, () -> container.setItem(0, carrier));
		assertTrue(BagContents.fromItems(java.util.List.of(carrier), 9).isEmpty());

		Inventory inventory = new Inventory(null, null);
		PackMenu menu = new PackMenu(4, inventory, new PackMenuData(9, 0, 0));
		inventory.setItem(9, carrier);
		assertFalse(menu.getSlot(0).mayPlace(carrier));
		menu.getSlot(0).set(carrier);
		assertTrue(menu.getSlot(0).getItem().isEmpty());
		assertTrue(menu.quickMoveStack(null, menu.bagSlotCount()).isEmpty());
		assertSame(carrier, inventory.getItem(9));
	}

	@Test
	void bagMetadataSurvivesImmediateMutationsAndPhysicalCopiesDiverge() {
		ItemStack bag = new ItemStack(ModItems.UPGRADED_POUCH);
		bag.set(DataComponents.CUSTOM_NAME, Component.literal("Survey Kit"));
		BagContainer container = new BagContainer(bag);
		container.setItem(4, new ItemStack(Items.APPLE, 6));

		assertEquals("Survey Kit", bag.get(DataComponents.CUSTOM_NAME).getString());
		ItemStack movedCopy = bag.copy();
		assertEquals("Survey Kit", movedCopy.get(DataComponents.CUSTOM_NAME).getString());
		assertEquals(6, movedCopy.get(ModDataComponents.BAG_CONTENTS).getStack(4).getCount());

		assertEquals(2, new BagContainer(movedCopy).removeItem(4, 2).getCount());
		assertEquals(6, bag.get(ModDataComponents.BAG_CONTENTS).getStack(4).getCount());
		assertEquals(4, movedCopy.get(ModDataComponents.BAG_CONTENTS).getStack(4).getCount());
	}

	private static PackMenu serverMenu(EquippedBagHandle satchel) {
		return PackMenu.createServer(
			1,
			new Inventory(null, null),
			new TrinketsIntegration.EquippedBags(Optional.of(satchel), Optional.empty(), Optional.empty())
		);
	}

	private static EquippedBagHandle captureSatchel(AtomicReference<ItemStack> liveSlot) {
		return EquippedBagHandle.capture(
			TrinketsIntegration.SATCHEL_SLOT,
			TrinketsIntegration.SATCHEL_INDEX,
			BagEquipmentRole.SATCHEL,
			liveSlot::get
		).orElseThrow();
	}
}
