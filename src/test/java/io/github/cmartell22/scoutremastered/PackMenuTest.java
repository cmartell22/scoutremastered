package io.github.cmartell22.scoutremastered;

import eu.pb4.trinkets.api.DefaultTrinketSlots;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

final class PackMenuTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void openingDataUsesStableRoleOrderAndRoundTrips() {
		EquippedBagHandle satchel = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_SATCHEL)), BagEquipmentRole.SATCHEL, 0);
		EquippedBagHandle left = capture(new AtomicReference<>(new ItemStack(ModItems.POUCH)), BagEquipmentRole.POUCH, 0);
		EquippedBagHandle right = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_POUCH)), BagEquipmentRole.POUCH, 1);
		PackMenuData data = PackMenuData.from(new TrinketsIntegration.EquippedBags(
			Optional.of(satchel),
			Optional.of(left),
			Optional.of(right)
		));

		assertEquals(new PackMenuData(18, 3, 6), data);
		assertEquals(27, data.bagSlotCount());
		var buffer = Unpooled.buffer();
		try {
			PackMenuData.STREAM_CODEC.encode(buffer, data);
			assertEquals(data, PackMenuData.STREAM_CODEC.decode(buffer));
		} finally {
			buffer.release();
		}

		assertThrows(IllegalArgumentException.class, () -> new PackMenuData(19, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new PackMenuData(0, -1, 0));
	}

	@Test
	void menuSlotsAreSatchelLeftRightThenPlayerInventoryAndHotbar() {
		Inventory inventory = new Inventory(null, null);
		PackMenu menu = new PackMenu(1, inventory, new PackMenuData(18, 3, 6));

		assertEquals(27, menu.bagSlotCount());
		assertEquals(0, menu.getSlot(0).getContainerSlot());
		assertEquals(8, menu.getSlot(0).x);
		assertEquals(17, menu.getSlot(17).getContainerSlot());
		assertEquals(PackMenuLayout.LEFT_POUCH_X, menu.getSlot(18).x);
		assertEquals(PackMenuLayout.RIGHT_POUCH_X, menu.getSlot(21).x);
		assertSame(inventory, menu.getSlot(27).container);
		assertEquals(9, menu.getSlot(27).getContainerSlot());
		assertEquals(0, menu.getSlot(54).getContainerSlot());
		assertEquals(219, menu.layout().imageHeight());
	}

	@Test
	void playerShiftClickMergesAcrossAllBagsBeforeUsingFirstEmptySlot() {
		Inventory inventory = new Inventory(null, null);
		PackMenu menu = new PackMenu(2, inventory, new PackMenuData(9, 3, 3));
		menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 60));
		menu.getSlot(9).set(new ItemStack(Items.DIAMOND, 61));
		inventory.setItem(9, new ItemStack(Items.DIAMOND, 10));

		ItemStack moved = menu.quickMoveStack(null, menu.bagSlotCount());

		assertEquals(10, moved.getCount());
		assertTrue(inventory.getItem(9).isEmpty());
		assertEquals(64, menu.getSlot(0).getItem().getCount());
		assertEquals(64, menu.getSlot(9).getItem().getCount());
		assertEquals(3, menu.getSlot(1).getItem().getCount());
	}

	@Test
	void playerShiftClickNeverPlacesBagsInsideBags() {
		Inventory inventory = new Inventory(null, null);
		PackMenu menu = new PackMenu(3, inventory, new PackMenuData(9, 3, 3));
		inventory.setItem(9, new ItemStack(ModItems.SATCHEL));

		assertTrue(menu.quickMoveStack(null, menu.bagSlotCount()).isEmpty());
		assertSame(ModItems.SATCHEL, inventory.getItem(9).getItem());
		for (int slot = 0; slot < menu.bagSlotCount(); slot++) {
			assertTrue(menu.getSlot(slot).getItem().isEmpty());
			assertFalse(menu.getSlot(slot).mayPlace(new ItemStack(ModItems.POUCH)));
		}
		menu.getSlot(0).set(new ItemStack(ModItems.POUCH));
		assertTrue(menu.getSlot(0).getItem().isEmpty());
	}

	@Test
	void bagShiftClickUsesMainInventoryBeforeHotbar() {
		Inventory inventory = new Inventory(null, null);
		PackMenu menu = new PackMenu(4, inventory, new PackMenuData(9, 0, 0));
		menu.getSlot(0).set(new ItemStack(Items.APPLE, 5));

		assertEquals(5, menu.quickMoveStack(null, 0).getCount());
		assertEquals(5, inventory.getItem(9).getCount());
		assertTrue(inventory.getItem(0).isEmpty());
	}

	@Test
	void staleHandleGuardsEveryBagContainerMutationAndInvalidatesServerMenu() {
		ItemStack original = new ItemStack(ModItems.SATCHEL);
		AtomicReference<ItemStack> liveSlot = new AtomicReference<>(original);
		EquippedBagHandle handle = capture(liveSlot, BagEquipmentRole.SATCHEL, 0);
		BagContainer container = new BagContainer(handle);
		container.setItem(0, new ItemStack(Items.EMERALD, 7));
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(handle),
			Optional.empty(),
			Optional.empty()
		);
		PackMenu serverMenu = PackMenu.createServer(5, new Inventory(null, null), bags);

		liveSlot.set(new ItemStack(ModItems.SATCHEL));

		assertFalse(container.isLive());
		assertFalse(serverMenu.hasValidServerBacking());
		assertTrue(container.removeItem(0, 7).isEmpty());
		container.setItem(0, new ItemStack(Items.DIAMOND, 1));
		container.clearContent();
		assertEquals(7, original.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertTrue(liveSlot.get().get(ModDataComponents.BAG_CONTENTS).isEmpty());
	}

	@Test
	void openPayloadIsEmptyIntentOnly() {
		assertEquals("scoutremastered:open_pack", OpenPackPayload.TYPE.id().toString());
		var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
			Unpooled.buffer(),
			net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY)
		);
		try {
			OpenPackPayload.STREAM_CODEC.encode(buffer, OpenPackPayload.INSTANCE);
			assertEquals(0, buffer.readableBytes());
			assertEquals(OpenPackPayload.INSTANCE, OpenPackPayload.STREAM_CODEC.decode(buffer));
		} finally {
			buffer.release();
		}
	}

	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		int slotIndex
	) {
		String slotId = role == BagEquipmentRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, slotIndex, role, liveSlot::get).orElseThrow();
	}
}
