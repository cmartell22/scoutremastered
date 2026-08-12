package io.github.cmartell22.scoutremastered;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** One-shot server provider captured from an authoritative equipped-bag discovery. */
final class PackMenuProvider implements ExtendedMenuProvider<PackMenuData> {
	private static final Component TITLE = Component.translatable("container.scoutremastered.pack");
	private final ServerPlayer owner;
	private final TrinketsIntegration.EquippedBags bags;
	private final PackMenuData data;

	PackMenuProvider(ServerPlayer owner, TrinketsIntegration.EquippedBags bags) {
		this.owner = owner;
		this.bags = bags;
		this.data = PackMenuData.from(bags);
		if (!this.data.hasAnyBag()) {
			throw new IllegalArgumentException("Cannot provide a pack menu without an equipped bag");
		}
	}

	@Override
	public Component getDisplayName() {
		return TITLE;
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		if (player != this.owner || inventory != this.owner.getInventory() || !this.backingStillMatches()) {
			return null;
		}
		return PackMenu.createServer(containerId, inventory, this.bags);
	}

	@Override
	public PackMenuData getScreenOpeningData(ServerPlayer player) {
		if (player != this.owner || !this.backingStillMatches()) {
			throw new IllegalStateException("Equipped bags changed while opening the pack menu");
		}
		return this.data;
	}

	private boolean backingStillMatches() {
		return this.bags.inStableOrder().stream().allMatch(EquippedBagHandle::isValid)
			&& PackMenuData.from(this.bags).equals(this.data);
	}
}
