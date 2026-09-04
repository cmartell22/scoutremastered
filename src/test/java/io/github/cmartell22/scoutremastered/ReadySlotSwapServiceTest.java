package io.github.cmartell22.scoutremastered;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadySlotSwapServiceTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void swapsNonemptyHandAndNonemptyReadySlotAsCompleteStacks() {
		Fixture fixture = fixture(
			new ItemStack(Items.DIAMOND, 17),
			new ItemStack(Items.EMERALD, 9),
			ItemStack.EMPTY,
			ItemStack.EMPTY
		);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		assertStack(new ItemStack(Items.EMERALD, 9), fixture.inventory().getSelectedItem());
		assertStack(new ItemStack(Items.DIAMOND, 17), ready(fixture.satchel()));
		assertEquals(1, fixture.synchronizations().get());
		assertEquals(1, fixture.inventory().getTimesChanged());
	}

	@Test
	void bagWritePrecedesHotbarWriteAndSynchronizationFollowsTheCompleteCommit() {
		ItemStack hand = new ItemStack(Items.DIAMOND, 17);
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(
			ModItems.SATCHEL,
			new ItemStack(Items.EMERALD, 9)
		));
		AtomicBoolean observeWrites = new AtomicBoolean();
		AtomicBoolean hotbarWriteObserved = new AtomicBoolean();
		Inventory inventory = new Inventory(null, null) {
			@Override
			public ItemStack setSelectedItem(ItemStack itemStack) {
				if (observeWrites.get()) {
					assertStack(hand, ready(satchel));
					hotbarWriteObserved.set(true);
				}
				return super.setSelectedItem(itemStack);
			}
		};
		inventory.setSelectedSlot(2);
		inventory.setSelectedItem(hand);
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(handle), Optional.empty(), Optional.empty()
		);
		AtomicInteger synchronizations = new AtomicInteger();
		observeWrites.set(true);

		assertEquals(
			ReadySlotSwapService.Result.SUCCESS,
			ReadySlotSwapService.swap(inventory, bags, ReadySlotRole.SATCHEL, () -> {
				assertTrue(hotbarWriteObserved.get());
				assertStack(new ItemStack(Items.EMERALD, 9), inventory.getSelectedItem());
				synchronizations.incrementAndGet();
			})
		);
		assertEquals(1, synchronizations.get());
	}

	@Test
	void swapsEmptyHandAndNonemptyReadySlot() {
		Fixture fixture = fixture(ItemStack.EMPTY, new ItemStack(Items.EMERALD, 9), ItemStack.EMPTY, ItemStack.EMPTY);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		assertStack(new ItemStack(Items.EMERALD, 9), fixture.inventory().getSelectedItem());
		assertTrue(ready(fixture.satchel()).isEmpty());
	}

	@Test
	void swapsNonemptyHandAndEmptyReadySlot() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 17), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		assertTrue(fixture.inventory().getSelectedItem().isEmpty());
		assertStack(new ItemStack(Items.DIAMOND, 17), ready(fixture.satchel()));
	}

	@Test
	void swapsEmptyHandAndEmptyReadySlotWithoutCreatingAStack() {
		Fixture fixture = fixture(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		assertTrue(fixture.inventory().getSelectedItem().isEmpty());
		assertTrue(ready(fixture.satchel()).isEmpty());
		assertEquals(1, fixture.synchronizations().get());
	}

	@Test
	void preservesIdentityCountDurabilityEnchantmentsNameAndArbitraryComponentsExactly() {
		ItemStack richSword = componentRichSword();
		ItemStack expected = richSword.copy();
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 23), richSword, ItemStack.EMPTY, ItemStack.EMPTY);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		ItemStack swapped = fixture.inventory().getSelectedItem();

		assertStack(expected, swapped);
		assertSame(Items.IRON_SWORD, swapped.getItem());
		assertEquals(1, swapped.getCount());
		assertEquals(37, swapped.getDamageValue());
		assertEquals("RS2 Exact Blade", swapped.get(DataComponents.CUSTOM_NAME).getString());
		assertEquals(expected.get(DataComponents.ENCHANTMENTS), swapped.get(DataComponents.ENCHANTMENTS));
		assertEquals(expected.get(DataComponents.CUSTOM_DATA), swapped.get(DataComponents.CUSTOM_DATA));

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		assertStack(expected, ready(fixture.satchel()));
	}

	@Test
	void nestedScoutBagInHandIsRejectedWithZeroMutation() {
		Fixture fixture = fixture(
			new ItemStack(ModItems.POUCH),
			new ItemStack(Items.EMERALD, 9),
			ItemStack.EMPTY,
			ItemStack.EMPTY
		);
		ItemStack handBefore = fixture.inventory().getSelectedItem();
		BagContents bagBefore = fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS);

		assertEquals(ReadySlotSwapService.Result.HAND_NOT_STORABLE, fixture.swap(ReadySlotRole.SATCHEL));
		assertSame(handBefore, fixture.inventory().getSelectedItem());
		assertSame(bagBefore, fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS));
		assertEquals(0, fixture.synchronizations().get());
		assertEquals(0, fixture.inventory().getTimesChanged());
	}

	@Test
	void missingBagIsRejectedWithZeroMutation() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack handBefore = fixture.inventory().getSelectedItem();
		BagContents bagBefore = fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS);
		TrinketsIntegration.EquippedBags missing = new TrinketsIntegration.EquippedBags(
			Optional.empty(),
			fixture.bags().leftPouch(),
			fixture.bags().rightPouch()
		);

		assertEquals(
			ReadySlotSwapService.Result.MISSING_BAG,
			ReadySlotSwapService.swap(fixture.inventory(), missing, ReadySlotRole.SATCHEL, fixture.synchronizations()::incrementAndGet)
		);
		assertSame(handBefore, fixture.inventory().getSelectedItem());
		assertSame(bagBefore, fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS));
		assertEquals(0, fixture.synchronizations().get());
	}

	@Test
	void trinketsRebuildCopyInvalidatesTheCapturedHandleWithZeroMutation() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack originalBag = fixture.satchel().get();
		BagContents originalContents = originalBag.get(ModDataComponents.BAG_CONTENTS);
		fixture.satchel().set(originalBag.copy());

		assertEquals(ReadySlotSwapService.Result.STALE_BAG, fixture.swap(ReadySlotRole.SATCHEL));
		assertSame(originalContents, originalBag.get(ModDataComponents.BAG_CONTENTS));
		assertStack(new ItemStack(Items.DIAMOND), fixture.inventory().getSelectedItem());
		assertEquals(0, fixture.synchronizations().get());
	}

	@Test
	void sameRoleReplacementBagIsRejectedWithZeroMutation() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack original = fixture.satchel().get();
		BagContents originalBefore = original.get(ModDataComponents.BAG_CONTENTS);
		ItemStack replacement = bag(ModItems.SATCHEL, new ItemStack(Items.GOLD_INGOT, 5));
		BagContents replacementBefore = replacement.get(ModDataComponents.BAG_CONTENTS);
		fixture.satchel().set(replacement);

		assertEquals(ReadySlotSwapService.Result.STALE_BAG, fixture.swap(ReadySlotRole.SATCHEL));
		assertSame(originalBefore, original.get(ModDataComponents.BAG_CONTENTS));
		assertSame(replacementBefore, replacement.get(ModDataComponents.BAG_CONTENTS));
		assertStack(new ItemStack(Items.DIAMOND), fixture.inventory().getSelectedItem());
	}

	@Test
	void unequippedBagIsRejectedWithZeroMutation() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack original = fixture.satchel().get();
		BagContents before = original.get(ModDataComponents.BAG_CONTENTS);
		fixture.satchel().set(ItemStack.EMPTY);

		assertEquals(ReadySlotSwapService.Result.STALE_BAG, fixture.swap(ReadySlotRole.SATCHEL));
		assertSame(before, original.get(ModDataComponents.BAG_CONTENTS));
		assertStack(new ItemStack(Items.DIAMOND), fixture.inventory().getSelectedItem());
	}

	@Test
	void wrongRoleOrWrongPouchSlotIsRejectedWithZeroMutation() {
		Fixture fixture = fixture(
			new ItemStack(Items.DIAMOND),
			new ItemStack(Items.GOLD_INGOT, 2),
			new ItemStack(Items.EMERALD, 3),
			new ItemStack(Items.APPLE, 4)
		);
		TrinketsIntegration.EquippedBags satchelAsLeft = new TrinketsIntegration.EquippedBags(
			fixture.bags().satchel(),
			fixture.bags().satchel(),
			fixture.bags().rightPouch()
		);
		TrinketsIntegration.EquippedBags leftAsRight = new TrinketsIntegration.EquippedBags(
			fixture.bags().satchel(),
			fixture.bags().leftPouch(),
			fixture.bags().leftPouch()
		);
		ItemStack handBefore = fixture.inventory().getSelectedItem();
		BagContents satchelBefore = fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS);
		BagContents leftBefore = fixture.left().get().get(ModDataComponents.BAG_CONTENTS);

		assertEquals(
			ReadySlotSwapService.Result.WRONG_ROLE,
			ReadySlotSwapService.swap(fixture.inventory(), satchelAsLeft, ReadySlotRole.LEFT_POUCH, () -> {})
		);
		assertEquals(
			ReadySlotSwapService.Result.WRONG_ROLE,
			ReadySlotSwapService.swap(fixture.inventory(), leftAsRight, ReadySlotRole.RIGHT_POUCH, () -> {})
		);
		assertSame(handBefore, fixture.inventory().getSelectedItem());
		assertSame(satchelBefore, fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS));
		assertSame(leftBefore, fixture.left().get().get(ModDataComponents.BAG_CONTENTS));
	}

	@Test
	void everyRoleTargetsOnlyItsOwnLocalSlotZero() {
		verifyRoleIsolation(ReadySlotRole.SATCHEL);
		verifyRoleIsolation(ReadySlotRole.LEFT_POUCH);
		verifyRoleIsolation(ReadySlotRole.RIGHT_POUCH);
	}

	@Test
	void oneThousandRepeatedSwapsLoseDuplicateOrCrossMutateNothing() {
		ItemStack originalHand = componentRichSword();
		ItemStack originalReady = new ItemStack(Items.DIAMOND, 31);
		Fixture fixture = fixture(
			originalHand,
			originalReady,
			new ItemStack(Items.EMERALD, 7),
			new ItemStack(Items.APPLE, 5)
		);
		ItemStack leftBefore = ready(fixture.left()).copy();
		ItemStack rightBefore = ready(fixture.right()).copy();

		for (int iteration = 0; iteration < 1_000; iteration++) {
			assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));
		}

		assertStack(originalHand, fixture.inventory().getSelectedItem());
		assertStack(originalReady, ready(fixture.satchel()));
		assertStack(leftBefore, ready(fixture.left()));
		assertStack(rightBefore, ready(fixture.right()));
		assertEquals(1_000, fixture.synchronizations().get());
		assertEquals(1_000, fixture.inventory().getTimesChanged());
	}

	@Test
	void hotbarWriteFailureRestoresBothCompleteSnapshotsAndDoesNotSynchronize() {
		FailOnceInventory inventory = new FailOnceInventory();
		inventory.setSelectedSlot(3);
		ItemStack hand = componentRichSword();
		inventory.setSelectedItem(hand);
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(ModItems.SATCHEL, new ItemStack(Items.DIAMOND, 19)));
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(handle), Optional.empty(), Optional.empty()
		);
		AtomicInteger synchronizations = new AtomicInteger();
		ItemStack handBefore = hand.copy();
		ItemStack readyBefore = ready(satchel).copy();
		inventory.failNextWriteAfterMutation();

		assertEquals(
			ReadySlotSwapService.Result.HOTBAR_WRITE_FAILED_ROLLED_BACK,
			ReadySlotSwapService.swap(inventory, bags, ReadySlotRole.SATCHEL, synchronizations::incrementAndGet)
		);
		assertStack(handBefore, inventory.getSelectedItem());
		assertStack(readyBefore, ready(satchel));
		assertEquals(0, synchronizations.get());
	}

	@Test
	void bagContentsRemainsTheSoleBagStorageRepresentation() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY, ItemStack.EMPTY);
		BagContents before = fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS);

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(ReadySlotRole.SATCHEL));

		BagContents after = fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS);
		assertFalse(after == before);
		assertStack(new ItemStack(Items.DIAMOND, 6), after.getStack(0));
		assertStack(after.getStack(0), new BagContainer(fixture.satchel().get()).getItem(0));
	}

	private static void verifyRoleIsolation(ReadySlotRole role) {
		Fixture fixture = fixture(
			new ItemStack(Items.DIAMOND, 11),
			new ItemStack(Items.GOLD_INGOT, 2),
			new ItemStack(Items.EMERALD, 3),
			new ItemStack(Items.APPLE, 4)
		);
		new BagContainer(fixture.satchel().get()).setItem(1, new ItemStack(Items.APPLE, 21));
		new BagContainer(fixture.left().get()).setItem(1, new ItemStack(Items.APPLE, 22));
		new BagContainer(fixture.right().get()).setItem(1, new ItemStack(Items.APPLE, 23));
		ItemStack satchelBefore = ready(fixture.satchel()).copy();
		ItemStack leftBefore = ready(fixture.left()).copy();
		ItemStack rightBefore = ready(fixture.right()).copy();

		assertEquals(ReadySlotSwapService.Result.SUCCESS, fixture.swap(role));
		assertStack(new ItemStack(Items.DIAMOND, 11), switch (role) {
			case SATCHEL -> ready(fixture.satchel());
			case LEFT_POUCH -> ready(fixture.left());
			case RIGHT_POUCH -> ready(fixture.right());
		});
		if (role != ReadySlotRole.SATCHEL) {
			assertStack(satchelBefore, ready(fixture.satchel()));
		}
		if (role != ReadySlotRole.LEFT_POUCH) {
			assertStack(leftBefore, ready(fixture.left()));
		}
		if (role != ReadySlotRole.RIGHT_POUCH) {
			assertStack(rightBefore, ready(fixture.right()));
		}
		assertEquals(21, fixture.satchel().get().get(ModDataComponents.BAG_CONTENTS).getStack(1).getCount());
		assertEquals(22, fixture.left().get().get(ModDataComponents.BAG_CONTENTS).getStack(1).getCount());
		assertEquals(23, fixture.right().get().get(ModDataComponents.BAG_CONTENTS).getStack(1).getCount());
	}

	private static Fixture fixture(ItemStack hand, ItemStack satchelReady, ItemStack leftReady, ItemStack rightReady) {
		Inventory inventory = new Inventory(null, null);
		inventory.setSelectedSlot(4);
		inventory.setSelectedItem(hand);
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(ModItems.SATCHEL, satchelReady));
		AtomicReference<ItemStack> left = new AtomicReference<>(bag(ModItems.UPGRADED_POUCH, leftReady));
		AtomicReference<ItemStack> right = new AtomicReference<>(bag(ModItems.UPGRADED_POUCH, rightReady));
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
		return new Fixture(inventory, satchel, left, right, bags, new AtomicInteger());
	}

	private static ItemStack bag(BagItem bagItem, ItemStack ready) {
		ItemStack bag = new ItemStack(bagItem);
		if (!ready.isEmpty()) {
			new BagContainer(bag).setItem(0, ready);
		}
		return bag;
	}

	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		String slotId,
		int index
	) {
		return EquippedBagHandle.capture(slotId, index, role, liveSlot::get).orElseThrow();
	}

	private static ItemStack ready(AtomicReference<ItemStack> bag) {
		return bag.get().get(ModDataComponents.BAG_CONTENTS).getStack(0);
	}

	private static ItemStack componentRichSword() {
		ItemStack stack = new ItemStack(Items.IRON_SWORD);
		stack.setDamageValue(37);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("RS2 Exact Blade"));

		Enchantment enchantment = new Enchantment(
			Component.literal("RS2 Test Enchantment"),
			Enchantment.definition(
				HolderSet.direct(Items.IRON_SWORD.builtInRegistryHolder()),
				10,
				5,
				Enchantment.constantCost(1),
				Enchantment.constantCost(20),
				2,
				EquipmentSlotGroup.MAINHAND
			),
			HolderSet.empty(),
			DataComponentMap.EMPTY
		);
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(Holder.direct(enchantment), 4);
		stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

		CompoundTag marker = new CompoundTag();
		marker.putString("rs2_marker", "arbitrary-component-payload");
		marker.putInt("sequence", 2602);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
		return stack;
	}

	private static void assertStack(ItemStack expected, ItemStack actual) {
		assertTrue(
			ItemStack.matches(expected, actual),
			() -> "Expected complete stack " + expected + " but got " + actual
		);
	}

	private record Fixture(
		Inventory inventory,
		AtomicReference<ItemStack> satchel,
		AtomicReference<ItemStack> left,
		AtomicReference<ItemStack> right,
		TrinketsIntegration.EquippedBags bags,
		AtomicInteger synchronizations
	) {
		ReadySlotSwapService.Result swap(ReadySlotRole role) {
			return ReadySlotSwapService.swap(this.inventory, this.bags, role, this.synchronizations::incrementAndGet);
		}
	}

	private static final class FailOnceInventory extends Inventory {
		private boolean failNextWrite;

		private FailOnceInventory() {
			super(null, null);
		}

		void failNextWriteAfterMutation() {
			this.failNextWrite = true;
		}

		@Override
		public ItemStack setSelectedItem(ItemStack itemStack) {
			ItemStack previous = super.setSelectedItem(itemStack);
			if (this.failNextWrite) {
				this.failNextWrite = false;
				throw new IllegalStateException("forced RS2 hotbar write failure");
			}
			return previous;
		}
	}
}
