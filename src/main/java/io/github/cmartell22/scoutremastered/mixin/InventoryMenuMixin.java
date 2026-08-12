package io.github.cmartell22.scoutremastered.mixin;

import io.github.cmartell22.scoutremastered.BagQuickMove;
import io.github.cmartell22.scoutremastered.BagStorageSlot;
import io.github.cmartell22.scoutremastered.EquippedBagHandle;
import io.github.cmartell22.scoutremastered.IntegratedBagContainer;
import io.github.cmartell22.scoutremastered.IntegratedInventoryData;
import io.github.cmartell22.scoutremastered.IntegratedInventoryLayout;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import io.github.cmartell22.scoutremastered.IntegratedInventoryRole;
import io.github.cmartell22.scoutremastered.TrinketsIntegration;
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
	private final Map<IntegratedInventoryRole, IntegratedBagContainer> scoutremastered$integratedContainers =
		new EnumMap<>(IntegratedInventoryRole.class);

	@Unique
	private Player scoutremastered$owner;

	@Unique
	private IntegratedInventoryData scoutremastered$clientLayoutData = IntegratedInventoryData.EMPTY;

	@Inject(
		method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
		at = @At("RETURN"),
		require = 1
	)
	private void scoutremastered$appendFixedDormantSlots(Inventory inventory, boolean active, Player owner, CallbackInfo callback) {
		this.scoutremastered$owner = owner;
		for (IntegratedInventoryRole role : IntegratedInventoryRole.values()) {
			IntegratedBagContainer container = new IntegratedBagContainer(role);
			this.scoutremastered$integratedContainers.put(role, container);
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
			throw new IllegalStateException("ADR-012 ScoutRemastered slot prefix must end at index 75 before later integrations append slots");
		}
	}

	@Inject(
		method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
		at = @At("HEAD"),
		cancellable = true,
		require = 1
	)
	private void scoutremastered$routePlayerStackToBags(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
		if (slotIndex < InventoryMenu.INV_SLOT_START
			|| slotIndex >= InventoryMenu.USE_ROW_SLOT_END
			|| !this.scoutremastered$isIntegratedSessionValid(player)) {
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
	public void scoutremastered$activateServer(TrinketsIntegration.EquippedBags bags) {
		this.scoutremastered$deactivateIntegratedInventory();
		this.scoutremastered$bindServer(IntegratedInventoryRole.SATCHEL, bags.satchel());
		this.scoutremastered$bindServer(IntegratedInventoryRole.LEFT_POUCH, bags.leftPouch());
		this.scoutremastered$bindServer(IntegratedInventoryRole.RIGHT_POUCH, bags.rightPouch());
	}

	@Override
	public void scoutremastered$activateClient(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data) {
		this.scoutremastered$deactivateIntegratedInventory();
		this.scoutremastered$clientLayoutData = data;
		this.scoutremastered$bindClient(IntegratedInventoryRole.SATCHEL, bags.satchel(), data.satchelCapacity());
		this.scoutremastered$bindClient(IntegratedInventoryRole.LEFT_POUCH, bags.leftPouch(), data.leftPouchCapacity());
		this.scoutremastered$bindClient(IntegratedInventoryRole.RIGHT_POUCH, bags.rightPouch(), data.rightPouchCapacity());
	}

	@Override
	public void scoutremastered$previewClientLayout(IntegratedInventoryData data) {
		// Presentation-only seed used before InventoryScreen.init. The dormant containers remain
		// inactive until the server acknowledgement authorizes and binds the mirrored slots.
		this.scoutremastered$clientLayoutData = data;
	}

	@Override
	public void scoutremastered$finalizeClientBindings(TrinketsIntegration.EquippedBags bags, IntegratedInventoryData data) {
		this.scoutremastered$clientLayoutData = data;
		this.scoutremastered$rebindClient(IntegratedInventoryRole.SATCHEL, bags.satchel(), data.satchelCapacity());
		this.scoutremastered$rebindClient(IntegratedInventoryRole.LEFT_POUCH, bags.leftPouch(), data.leftPouchCapacity());
		this.scoutremastered$rebindClient(IntegratedInventoryRole.RIGHT_POUCH, bags.rightPouch(), data.rightPouchCapacity());
	}

	@Override
	public void scoutremastered$deactivateIntegratedInventory() {
		this.scoutremastered$integratedContainers.values().forEach(IntegratedBagContainer::deactivate);
		this.scoutremastered$clientLayoutData = IntegratedInventoryData.EMPTY;
	}

	@Override
	public IntegratedInventoryData scoutremastered$integratedInventoryData() {
		return new IntegratedInventoryData(
			this.scoutremastered$capacity(IntegratedInventoryRole.SATCHEL),
			this.scoutremastered$capacity(IntegratedInventoryRole.LEFT_POUCH),
			this.scoutremastered$capacity(IntegratedInventoryRole.RIGHT_POUCH)
		);
	}

	@Override
	public IntegratedInventoryData scoutremastered$clientLayoutData() {
		return this.scoutremastered$clientLayoutData;
	}

	@Override
	public boolean scoutremastered$hasActiveIntegratedInventory() {
		return this.scoutremastered$integratedInventoryData().hasAnyBag();
	}

	@Override
	public boolean scoutremastered$isIntegratedSessionValid(Player player) {
		return player == this.scoutremastered$owner
			&& this.scoutremastered$isSurvivalInventorySession()
			&& this.scoutremastered$integratedContainers.values().stream().anyMatch(IntegratedBagContainer::isBindingValid);
	}

	@Unique
	private void scoutremastered$bindServer(IntegratedInventoryRole role, Optional<EquippedBagHandle> handle) {
		handle.ifPresent(value -> this.scoutremastered$container(role).bindServer(value, this::scoutremastered$isSurvivalInventorySession));
	}

	@Unique
	private void scoutremastered$bindClient(IntegratedInventoryRole role, Optional<EquippedBagHandle> handle, int capacity) {
		if (capacity > 0) {
			handle.ifPresent(value -> this.scoutremastered$container(role).bindClient(value, capacity, this::scoutremastered$isSurvivalInventorySession));
		}
	}

	@Unique
	private void scoutremastered$rebindClient(IntegratedInventoryRole role, Optional<EquippedBagHandle> handle, int capacity) {
		IntegratedBagContainer container = this.scoutremastered$container(role);
		if (capacity <= 0 || handle.isEmpty()
			|| !container.rebindClient(handle.get(), capacity, this::scoutremastered$isSurvivalInventorySession)) {
			container.deactivate();
		}
	}

	@Unique
	private IntegratedBagContainer scoutremastered$container(IntegratedInventoryRole role) {
		IntegratedBagContainer container = this.scoutremastered$integratedContainers.get(role);
		if (container == null) {
			throw new IllegalStateException("ADR-012 fixed containers were not initialized");
		}
		return container;
	}

	@Unique
	private int scoutremastered$capacity(IntegratedInventoryRole role) {
		IntegratedBagContainer container = this.scoutremastered$integratedContainers.get(role);
		return container == null ? 0 : container.activeCapacity();
	}

	@Unique
	private boolean scoutremastered$isSurvivalInventorySession() {
		Player owner = this.scoutremastered$owner;
		return owner != null
			&& owner.isAlive()
			&& !owner.isRemoved()
			&& !owner.isSpectator()
			&& !owner.hasInfiniteMaterials()
			&& owner.containerMenu == owner.inventoryMenu
			&& (!(owner instanceof ServerPlayer serverPlayer) || !serverPlayer.hasDisconnected());
	}
}
