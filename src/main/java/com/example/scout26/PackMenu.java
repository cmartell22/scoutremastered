package com.example.scout26;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Dedicated synchronized menu over the live physical ItemStacks equipped through Trinkets. */
public final class PackMenu extends AbstractContainerMenu {
	private final PackMenuData data;
	private final PackMenuLayout layout;
	private final List<EquippedBagHandle> liveHandles;
	private final Player owner;
	private final boolean serverBacked;
	private final int bagSlotCount;
	private final int playerInventoryStart;
	private final int playerInventoryEnd;

	/** Client factory used by {@link net.fabricmc.fabric.api.menu.v1.ExtendedMenuType}. */
	public PackMenu(int containerId, Inventory inventory, PackMenuData data) {
		super(ModMenus.PACK, containerId);
		this.data = data;
		this.layout = new PackMenuLayout(data);
		this.liveHandles = List.of();
		this.owner = inventory.player;
		this.serverBacked = false;

		this.addClientBagSlots();
		this.bagSlotCount = this.slots.size();
		this.playerInventoryStart = this.bagSlotCount;
		this.addStandardInventorySlots(inventory, this.layout.playerInventoryX(), this.layout.playerInventoryY());
		this.playerInventoryEnd = this.slots.size();
	}

	private PackMenu(int containerId, Inventory inventory, TrinketsIntegration.EquippedBags bags) {
		super(ModMenus.PACK, containerId);
		this.data = PackMenuData.from(bags);
		if (!this.data.hasAnyBag()) {
			throw new IllegalArgumentException("PackMenu requires at least one live equipped bag");
		}
		this.layout = new PackMenuLayout(this.data);
		this.liveHandles = bags.inStableOrder();
		this.owner = inventory.player;
		this.serverBacked = true;

		bags.satchel().ifPresent(handle -> this.addSatchelSlots(new BagContainer(handle)));
		bags.leftPouch().ifPresent(handle -> this.addLeftPouchSlots(new BagContainer(handle)));
		bags.rightPouch().ifPresent(handle -> this.addRightPouchSlots(new BagContainer(handle)));
		this.bagSlotCount = this.slots.size();
		this.playerInventoryStart = this.bagSlotCount;
		this.addStandardInventorySlots(inventory, this.layout.playerInventoryX(), this.layout.playerInventoryY());
		this.playerInventoryEnd = this.slots.size();
	}

	static PackMenu createServer(int containerId, Inventory inventory, TrinketsIntegration.EquippedBags bags) {
		return new PackMenu(containerId, inventory, bags);
	}

	@Override
	public boolean stillValid(Player player) {
		return !this.serverBacked
			|| player == this.owner
				&& player instanceof ServerPlayer serverPlayer
				&& player.isAlive()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& !serverPlayer.hasDisconnected()
				&& this.liveHandles.stream().allMatch(EquippedBagHandle::isValid);
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
		if (!this.backingValidFor(player)) {
			this.closeStaleMenu(player);
			return;
		}
		super.clicked(slotIndex, buttonNum, containerInput, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (!this.backingValidFor(player) || slotIndex < 0 || slotIndex >= this.slots.size()) {
			this.closeStaleMenu(player);
			return ItemStack.EMPTY;
		}

		Slot sourceSlot = this.slots.get(slotIndex);
		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack source = sourceSlot.getItem();
		ItemStack clicked = source.copy();
		boolean moved;
		if (slotIndex < this.bagSlotCount) {
			moved = this.moveItemStackTo(source, this.playerInventoryStart, this.playerInventoryEnd, false);
		} else {
			moved = BagStorageRules.canStore(source)
				&& this.moveItemStackTo(source, 0, this.bagSlotCount, false);
		}

		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) {
			sourceSlot.setByPlayer(ItemStack.EMPTY);
		} else {
			sourceSlot.setChanged();
		}
		return clicked;
	}

	public PackMenuData data() {
		return this.data;
	}

	public PackMenuLayout layout() {
		return this.layout;
	}

	public int bagSlotCount() {
		return this.bagSlotCount;
	}

	boolean hasValidServerBacking() {
		return !this.serverBacked || this.liveHandles.stream().allMatch(EquippedBagHandle::isValid);
	}

	private boolean backingValidFor(Player player) {
		return !this.serverBacked || this.stillValid(player);
	}

	private void closeStaleMenu(Player player) {
		if (this.serverBacked && player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu == this) {
			serverPlayer.closeContainer();
		}
	}

	private void addClientBagSlots() {
		if (this.data.satchelCapacity() > 0) {
			this.addSatchelSlots(new SimpleContainer(this.data.satchelCapacity()));
		}
		if (this.data.leftPouchCapacity() > 0) {
			this.addLeftPouchSlots(new SimpleContainer(this.data.leftPouchCapacity()));
		}
		if (this.data.rightPouchCapacity() > 0) {
			this.addRightPouchSlots(new SimpleContainer(this.data.rightPouchCapacity()));
		}
	}

	private void addSatchelSlots(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			this.addSlot(new BagSlot(container, slot, this.layout.satchelSlotX(slot), this.layout.satchelSlotY(slot)));
		}
	}

	private void addLeftPouchSlots(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			this.addSlot(new BagSlot(container, slot, this.layout.leftPouchSlotX(slot), this.layout.pouchSlotY(slot)));
		}
	}

	private void addRightPouchSlots(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			this.addSlot(new BagSlot(container, slot, this.layout.rightPouchSlotX(slot), this.layout.pouchSlotY(slot)));
		}
	}

	private static final class BagSlot extends Slot {
		private BagSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BagStorageRules.canStore(stack) && this.container.canPlaceItem(this.getContainerSlot(), stack);
		}

		@Override
		public void set(ItemStack stack) {
			if (stack.isEmpty() || this.mayPlace(stack)) {
				super.set(stack);
			}
		}

		@Override
		public boolean mayPickup(Player player) {
			return this.container.stillValid(player);
		}
	}
}
