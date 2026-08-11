package com.example.scout26.mixin;

import com.example.scout26.BagQuickMove;
import com.example.scout26.BagStorageSlot;
import com.example.scout26.EquippedBagHandle;
import com.example.scout26.IntegratedBagContainer;
import com.example.scout26.IntegratedInventoryData;
import com.example.scout26.IntegratedInventoryLayout;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.IntegratedInventoryRole;
import com.example.scout26.TrinketsIntegration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ADR-012 target: {@link InventoryMenu}.
 *
 * <p>Reason: install the identical 30-slot topology on both logical sides and add the missing
 * player-to-bag quick-move route. Failure mode: a changed constructor or shift-click boundary can
 * desynchronize container ID 0, so both exact-descriptor injections require one match. Version
 * risk: high because this is the common survival inventory protocol boundary.</p>
 */
// Trinkets Updated's InventoryMenu Mixin has priority 500. Running at 400 makes this RETURN
// callback execute first, reserving ADR-012 indices 46-75 before Trinkets appends its own slots.
@Mixin(value = InventoryMenu.class, priority = 400)
abstract class InventoryMenuMixin extends AbstractCraftingMenu implements IntegratedInventoryMenu {
	protected InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Unique
	private final Map<IntegratedInventoryRole, IntegratedBagContainer> scout26$integratedContainers =
		new EnumMap<>(IntegratedInventoryRole.class);

	@Unique
	private Player scout26$owner;

	@Inject(
		method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
		at = @At("RETURN"),
		require = 1
	)
	private void scout26$appendFixedDormantSlots(Inventory inventory, boolean active, Player owner, CallbackInfo callback) {
		this.scout26$owner = owner;
		for (IntegratedInventoryRole role : IntegratedInventoryRole.values()) {
			IntegratedBagContainer container = new IntegratedBagContainer(role);
			this.scout26$integratedContainers.put(role, container);
			for (int slot = 0; slot < role.maximumCapacity(); slot++) {
				this.addSlot(new BagStorageSlot(
					container,
					slot,
					IntegratedInventoryLayout.slotX(role, slot),
					IntegratedInventoryLayout.slotY(role, slot)
				));
			}
		}
		if (((InventoryMenu)(Object)this).slots.size() != IntegratedInventoryLayout.TOTAL_SLOT_COUNT) {
			throw new IllegalStateException("ADR-012 Scout26 slot prefix must end at index 75 before later integrations append slots");
		}
	}

	@Inject(
		method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
		at = @At("HEAD"),
		cancellable = true,
		require = 1
	)
	private void scout26$routePlayerStackToBags(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
		if (slotIndex < InventoryMenu.INV_SLOT_START
			|| slotIndex >= InventoryMenu.USE_ROW_SLOT_END
			|| !this.scout26$isIntegratedSessionValid(player)) {
			return;
		}

		Slot sourceSlot = ((InventoryMenu)(Object)this).slots.get(slotIndex);
		if (!sourceSlot.hasItem()) {
			return;
		}
		ItemStack source = sourceSlot.getItem();
		ItemStack clicked = source.copy();
		if (!BagQuickMove.moveToBags(
			source,
			IntegratedInventoryLayout.SATCHEL_START,
			IntegratedInventoryLayout.TOTAL_SLOT_COUNT,
			this::moveItemStackTo
		)) {
			return;
		}

		if (source.isEmpty()) {
			sourceSlot.setByPlayer(ItemStack.EMPTY, clicked);
		} else {
			sourceSlot.setChanged();
		}
		sourceSlot.onTake(player, source);
		callback.setReturnValue(clicked);
	}

	@Override
	public void scout26$activateServer(TrinketsIntegration.EquippedBags bags) {
		this.scout26$deactivateIntegratedInventory();
		this.scout26$bindServer(IntegratedInventoryRole.SATCHEL, bags.satchel());
		this.scout26$bindServer(IntegratedInventoryRole.LEFT_POUCH, bags.leftPouch());
		this.scout26$bindServer(IntegratedInventoryRole.RIGHT_POUCH, bags.rightPouch());
	}

	@Override
	public void scout26$activateClient(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data) {
		this.scout26$deactivateIntegratedInventory();
		this.scout26$bindClient(IntegratedInventoryRole.SATCHEL, bags.satchel(), data.satchelCapacity());
		this.scout26$bindClient(IntegratedInventoryRole.LEFT_POUCH, bags.leftPouch(), data.leftPouchCapacity());
		this.scout26$bindClient(IntegratedInventoryRole.RIGHT_POUCH, bags.rightPouch(), data.rightPouchCapacity());
	}

	@Override
	public void scout26$deactivateIntegratedInventory() {
		this.scout26$integratedContainers.values().forEach(IntegratedBagContainer::deactivate);
	}

	@Override
	public IntegratedInventoryData scout26$integratedInventoryData() {
		return new IntegratedInventoryData(
			this.scout26$capacity(IntegratedInventoryRole.SATCHEL),
			this.scout26$capacity(IntegratedInventoryRole.LEFT_POUCH),
			this.scout26$capacity(IntegratedInventoryRole.RIGHT_POUCH)
		);
	}

	@Override
	public boolean scout26$hasActiveIntegratedInventory() {
		return this.scout26$integratedInventoryData().hasAnyBag();
	}

	@Override
	public boolean scout26$isIntegratedSessionValid(Player player) {
		return player == this.scout26$owner
			&& this.scout26$isSurvivalInventorySession()
			&& this.scout26$integratedContainers.values().stream().anyMatch(IntegratedBagContainer::isBindingValid);
	}

	@Unique
	private void scout26$bindServer(IntegratedInventoryRole role, Optional<EquippedBagHandle> handle) {
		handle.ifPresent(value -> this.scout26$container(role).bindServer(value, this::scout26$isSurvivalInventorySession));
	}

	@Unique
	private void scout26$bindClient(IntegratedInventoryRole role, Optional<EquippedBagHandle> handle, int capacity) {
		if (capacity > 0) {
			handle.ifPresent(value -> this.scout26$container(role).bindClient(value, capacity, this::scout26$isSurvivalInventorySession));
		}
	}

	@Unique
	private IntegratedBagContainer scout26$container(IntegratedInventoryRole role) {
		IntegratedBagContainer container = this.scout26$integratedContainers.get(role);
		if (container == null) {
			throw new IllegalStateException("ADR-012 fixed containers were not initialized");
		}
		return container;
	}

	@Unique
	private int scout26$capacity(IntegratedInventoryRole role) {
		IntegratedBagContainer container = this.scout26$integratedContainers.get(role);
		return container == null ? 0 : container.activeCapacity();
	}

	@Unique
	private boolean scout26$isSurvivalInventorySession() {
		Player owner = this.scout26$owner;
		return owner != null
			&& owner.isAlive()
			&& !owner.isRemoved()
			&& !owner.isSpectator()
			&& !owner.hasInfiniteMaterials()
			&& owner.containerMenu == owner.inventoryMenu
			&& (!(owner instanceof ServerPlayer serverPlayer) || !serverPlayer.hasDisconnected());
	}
}
