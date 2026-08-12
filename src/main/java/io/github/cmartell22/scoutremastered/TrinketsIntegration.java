package io.github.cmartell22.scoutremastered;

import dev.yumi.commons.TriState;
import eu.pb4.trinkets.api.DefaultTrinketSlots;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.event.TrinketSlotCompatibilityCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Narrow server-side integration boundary for Trinkets Updated.
 */
public final class TrinketsIntegration {
	public static final int SATCHEL_INDEX = 0;
	public static final int LEFT_POUCH_INDEX = 0;
	public static final int RIGHT_POUCH_INDEX = 1;

	private TrinketsIntegration() {
	}

	public static void initialize() {
		TrinketSlotCompatibilityCallback.EVENT.register((stack, slot, entity, defaultResult) -> {
			if (!(stack.getItem() instanceof BagItem bagItem)) {
				return TriState.DEFAULT;
			}
			return TriState.from(
				entity instanceof Player && isAllowedSlot(bagItem.equipmentRole(), slot.slotType().getId())
			);
		});
		ScoutRemasteredMod.LOGGER.debug("ScoutRemastered Trinkets compatibility policy registered");
	}

	/**
	 * Discovers equipped bags from live Trinkets inventories in stable menu order.
	 */
	public static EquippedBags findEquippedBags(Player player) {
		Objects.requireNonNull(player, "player");
		return new EquippedBags(
			capture(player, DefaultTrinketSlots.CHEST_BACK, SATCHEL_INDEX, BagEquipmentRole.SATCHEL),
			capture(player, DefaultTrinketSlots.LEGS_BELT, LEFT_POUCH_INDEX, BagEquipmentRole.POUCH),
			capture(player, DefaultTrinketSlots.LEGS_BELT, RIGHT_POUCH_INDEX, BagEquipmentRole.POUCH)
		);
	}

	static boolean isAllowedSlot(BagEquipmentRole role, String slotId) {
		return switch (role) {
			case SATCHEL -> DefaultTrinketSlots.CHEST_BACK.equals(slotId);
			case POUCH -> DefaultTrinketSlots.LEGS_BELT.equals(slotId);
		};
	}

	static boolean isBagForRole(ItemStack stack, BagEquipmentRole role) {
		return !stack.isEmpty()
			&& stack.getItem() instanceof BagItem bagItem
			&& bagItem.equipmentRole() == role;
	}

	private static Optional<EquippedBagHandle> capture(
		Player player,
		String slotId,
		int slotIndex,
		BagEquipmentRole role
	) {
		return EquippedBagHandle.capture(
			slotId,
			slotIndex,
			role,
			() -> resolveStack(player, slotId, slotIndex)
		);
	}

	private static ItemStack resolveStack(Player player, String slotId, int slotIndex) {
		TrinketSlotAccess access = TrinketsApi.getAttachment(player).getSlotAccess(slotId, slotIndex);
		return access != null && access.isValid() ? access.get() : ItemStack.EMPTY;
	}

	public record EquippedBags(
		Optional<EquippedBagHandle> satchel,
		Optional<EquippedBagHandle> leftPouch,
		Optional<EquippedBagHandle> rightPouch
	) {
		public EquippedBags {
			Objects.requireNonNull(satchel, "satchel");
			Objects.requireNonNull(leftPouch, "leftPouch");
			Objects.requireNonNull(rightPouch, "rightPouch");
		}

		public List<EquippedBagHandle> inStableOrder() {
			List<EquippedBagHandle> handles = new ArrayList<>(3);
			this.satchel.ifPresent(handles::add);
			this.leftPouch.ifPresent(handles::add);
			this.rightPouch.ifPresent(handles::add);
			return List.copyOf(handles);
		}

		public boolean isEmpty() {
			return this.satchel.isEmpty() && this.leftPouch.isEmpty() && this.rightPouch.isEmpty();
		}
	}
}
