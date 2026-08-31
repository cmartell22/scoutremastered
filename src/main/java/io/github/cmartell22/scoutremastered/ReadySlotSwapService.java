package io.github.cmartell22.scoutremastered;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative transaction service for selected-hotbar/ready-slot full-stack swaps. */
public final class ReadySlotSwapService {
	private static final int READY_SLOT = 0;

	private ReadySlotSwapService() {
	}

	/**
	 * Rediscovers the requested equipped bag and swaps its local slot 0 with the selected hotbar slot.
	 * This entry point must be invoked on the logical server thread.
	 */
	public static Result swap(ServerPlayer player, ReadySlotRole role) {
		if (player == null || role == null) {
			return Result.INVALID_REQUEST;
		}
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.hasDisconnected()) {
			return Result.INVALID_PLAYER;
		}

		return swap(
			player.getInventory(),
			TrinketsIntegration.findEquippedBags(player),
			role,
			() -> player.inventoryMenu.broadcastChanges()
		);
	}

	/** Transaction core kept package-visible so correctness tests exercise the real service directly. */
	static Result swap(
		Inventory inventory,
		TrinketsIntegration.EquippedBags bags,
		ReadySlotRole role,
		Runnable synchronize
	) {
		if (inventory == null || bags == null || role == null || synchronize == null) {
			return Result.INVALID_REQUEST;
		}

		Optional<EquippedBagHandle> selectedHandle = role.select(bags);
		if (selectedHandle.isEmpty()) {
			return Result.MISSING_BAG;
		}

		EquippedBagHandle handle = selectedHandle.orElseThrow();
		if (!role.matches(handle)) {
			return Result.WRONG_ROLE;
		}
		if (handle.resolve().isEmpty()) {
			return Result.STALE_BAG;
		}

		ItemStack handSnapshot = inventory.getSelectedItem().copy();
		if (!handSnapshot.isEmpty() && !BagStorageRules.canStore(handSnapshot)) {
			return Result.HAND_NOT_STORABLE;
		}

		BagContainer bag;
		try {
			bag = new BagContainer(handle);
		} catch (IllegalArgumentException staleHandle) {
			return Result.STALE_BAG;
		}
		if (bag.getContainerSize() <= READY_SLOT || !bag.isLive()) {
			return Result.READY_SLOT_UNAVAILABLE;
		}

		ItemStack readySnapshot = bag.getItem(READY_SLOT).copy();
		Optional<ItemStack> previous = bag.replaceWholeSlot(READY_SLOT, handSnapshot);
		if (previous.isEmpty()) {
			return Result.BAG_WRITE_FAILED;
		}

		try {
			inventory.setSelectedItem(readySnapshot.copy());
			inventory.setChanged();
		} catch (RuntimeException hotbarFailure) {
			return rollback(bag, inventory, readySnapshot, handSnapshot, role, hotbarFailure);
		}

		try {
			synchronize.run();
		} catch (RuntimeException synchronizationFailure) {
			ScoutRemasteredMod.LOGGER.error(
				"Ready-slot {} swap committed but inventory synchronization failed",
				role,
				synchronizationFailure
			);
			return Result.COMMITTED_SYNC_FAILED;
		}
		return Result.SUCCESS;
	}

	private static Result rollback(
		BagContainer bag,
		Inventory inventory,
		ItemStack readySnapshot,
		ItemStack handSnapshot,
		ReadySlotRole role,
		RuntimeException hotbarFailure
	) {
		boolean bagRestored = bag.replaceWholeSlot(READY_SLOT, readySnapshot).isPresent();
		boolean handRestored;
		try {
			inventory.setSelectedItem(handSnapshot.copy());
			inventory.setChanged();
			handRestored = true;
		} catch (RuntimeException rollbackFailure) {
			hotbarFailure.addSuppressed(rollbackFailure);
			handRestored = false;
		}

		ScoutRemasteredMod.LOGGER.error(
			"Ready-slot {} hotbar write failed; rollback bagRestored={}, handRestored={}",
			role,
			bagRestored,
			handRestored,
			hotbarFailure
		);
		return bagRestored && handRestored ? Result.HOTBAR_WRITE_FAILED_ROLLED_BACK : Result.ROLLBACK_FAILED;
	}

	public enum Result {
		SUCCESS(true),
		COMMITTED_SYNC_FAILED(true),
		INVALID_REQUEST(false),
		INVALID_PLAYER(false),
		MISSING_BAG(false),
		WRONG_ROLE(false),
		STALE_BAG(false),
		READY_SLOT_UNAVAILABLE(false),
		HAND_NOT_STORABLE(false),
		BAG_WRITE_FAILED(false),
		HOTBAR_WRITE_FAILED_ROLLED_BACK(false),
		ROLLBACK_FAILED(false);

		private final boolean committed;

		Result(boolean committed) {
			this.committed = committed;
		}

		public boolean committed() {
			return this.committed;
		}
	}
}
