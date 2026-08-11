package com.example.scout26;

import eu.pb4.trinkets.api.DefaultTrinketSlots;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntegratedInventoryTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void fixedRangesAreBilateralAndTotalExactlySeventySixSlots() {
		assertEquals(46, IntegratedInventoryLayout.SATCHEL_START);
		assertEquals(64, IntegratedInventoryLayout.SATCHEL_END);
		assertEquals(64, IntegratedInventoryLayout.LEFT_POUCH_START);
		assertEquals(70, IntegratedInventoryLayout.LEFT_POUCH_END);
		assertEquals(70, IntegratedInventoryLayout.RIGHT_POUCH_START);
		assertEquals(76, IntegratedInventoryLayout.RIGHT_POUCH_END);
		assertEquals(76, IntegratedInventoryLayout.TOTAL_SLOT_COUNT);
		for (IntegratedInventoryRole role : IntegratedInventoryRole.values()) {
			assertEquals(role.maximumCapacity(), IntegratedInventoryLayout.menuEnd(role) - IntegratedInventoryLayout.menuStart(role));
		}
	}

	@Test
	void classicLayoutUsesCapacityPrefixesAndExternalPanels() {
		assertEquals(8, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.SATCHEL, 0));
		assertEquals(152, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.SATCHEL, 17));
		assertEquals(174, IntegratedInventoryLayout.slotY(IntegratedInventoryRole.SATCHEL, 0));
		assertEquals(192, IntegratedInventoryLayout.slotY(IntegratedInventoryRole.SATCHEL, 17));
		assertEquals(-20, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.LEFT_POUCH, 0));
		assertEquals(-38, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.LEFT_POUCH, 5));
		assertEquals(180, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.RIGHT_POUCH, 0));
		assertEquals(198, IntegratedInventoryLayout.slotX(IntegratedInventoryRole.RIGHT_POUCH, 5));
		assertEquals(24, IntegratedInventoryLayout.leftPanelWidth(3));
		assertEquals(42, IntegratedInventoryLayout.rightPanelWidth(6));
		assertEquals(42, IntegratedInventoryLayout.satchelPanelHeight(18));
	}

	@Test
	void dormantSlotsStayEmptyInvisibleAndRejectEveryMutation() {
		IntegratedBagContainer container = new IntegratedBagContainer(IntegratedInventoryRole.SATCHEL);
		BagStorageSlot slot = new BagStorageSlot(container, 0, 0, 0);

		assertEquals(18, container.getContainerSize());
		assertFalse(slot.isActive());
		assertFalse(slot.mayPlace(new ItemStack(Items.DIAMOND)));
		assertFalse(slot.mayPickup(null));
		slot.set(new ItemStack(Items.DIAMOND, 4));
		assertTrue(slot.getItem().isEmpty());
		assertTrue(slot.remove(4).isEmpty());
	}

	@Test
	void exactBindingActivatesOnlyTheConcreteCapacityPrefix() {
		AtomicReference<ItemStack> live = new AtomicReference<>(new ItemStack(ModItems.POUCH));
		EquippedBagHandle handle = capture(live, IntegratedInventoryRole.LEFT_POUCH);
		IntegratedBagContainer container = new IntegratedBagContainer(IntegratedInventoryRole.LEFT_POUCH);

		assertTrue(container.bindClient(handle, 3, () -> true));
		assertEquals(3, container.activeCapacity());
		assertTrue(container.isSlotActive(0));
		assertTrue(container.isSlotActive(2));
		assertFalse(container.isSlotActive(3));
		container.setItem(2, new ItemStack(Items.APPLE, 2));
		container.setItem(3, new ItemStack(Items.DIAMOND));
		assertEquals(2, container.getItem(2).getCount());
		assertTrue(container.getItem(3).isEmpty());

		IntegratedBagContainer mismatch = new IntegratedBagContainer(IntegratedInventoryRole.LEFT_POUCH);
		assertFalse(mismatch.bindClient(handle, 6, () -> true));
		assertEquals(0, mismatch.activeCapacity());
	}

	@Test
	void staleAuthoritativeHandleFailsClosedWithoutRetargetingReplacement() {
		ItemStack original = new ItemStack(ModItems.SATCHEL);
		AtomicReference<ItemStack> live = new AtomicReference<>(original);
		EquippedBagHandle handle = capture(live, IntegratedInventoryRole.SATCHEL);
		AtomicBoolean validSession = new AtomicBoolean(true);
		IntegratedBagContainer container = new IntegratedBagContainer(IntegratedInventoryRole.SATCHEL);
		assertTrue(container.bindServer(handle, validSession::get));
		container.setItem(0, new ItemStack(Items.EMERALD, 7));

		ItemStack replacement = new ItemStack(ModItems.SATCHEL);
		live.set(replacement);
		assertEquals(0, container.activeCapacity());
		assertTrue(container.getItem(0).isEmpty());
		container.setItem(0, new ItemStack(Items.DIAMOND));
		assertEquals(7, original.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertTrue(replacement.get(ModDataComponents.BAG_CONTENTS).isEmpty());

		live.set(original);
		validSession.set(false);
		assertFalse(container.isSlotActive(0));
	}

	@Test
	void sharedSlotPolicyRejectsEveryNestedBagCandidate() {
		AtomicReference<ItemStack> live = new AtomicReference<>(new ItemStack(ModItems.UPGRADED_POUCH));
		IntegratedBagContainer container = new IntegratedBagContainer(IntegratedInventoryRole.RIGHT_POUCH);
		assertTrue(container.bindServer(capture(live, IntegratedInventoryRole.RIGHT_POUCH), () -> true));
		BagStorageSlot slot = new BagStorageSlot(container, 0, 0, 0);

		assertFalse(slot.mayPlace(new ItemStack(ModItems.SATCHEL)));
		slot.set(new ItemStack(ModItems.POUCH));
		assertTrue(slot.getItem().isEmpty());
		assertTrue(BagQuickMove.moveToBags(new ItemStack(ModItems.UPGRADED_SATCHEL), 0, 1, (stack, start, end, backwards) -> true) == false);
	}

	@Test
	void sessionPayloadsCarryOnlyIntentAndServerDerivedCapacities() {
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
			Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
		);
		try {
			OpenIntegratedInventoryPayload.STREAM_CODEC.encode(buffer, OpenIntegratedInventoryPayload.INSTANCE);
			CloseIntegratedInventoryPayload.STREAM_CODEC.encode(buffer, CloseIntegratedInventoryPayload.INSTANCE);
			assertEquals(0, buffer.readableBytes());

			IntegratedInventoryAckPayload payload = new IntegratedInventoryAckPayload(new IntegratedInventoryData(18, 3, 6));
			IntegratedInventoryAckPayload.STREAM_CODEC.encode(buffer, payload);
			assertEquals(payload, IntegratedInventoryAckPayload.STREAM_CODEC.decode(buffer));
			assertEquals(0, buffer.readableBytes());
		} finally {
			buffer.release();
		}
	}

	private static EquippedBagHandle capture(AtomicReference<ItemStack> live, IntegratedInventoryRole role) {
		String slotId = role == IntegratedInventoryRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, role.equipmentIndex(), role.equipmentRole(), live::get).orElseThrow();
	}
}
