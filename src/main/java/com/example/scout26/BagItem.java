package com.example.scout26;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A physical bag item whose storage capacity is defined by the item, never by serialized contents.
 */
public final class BagItem extends Item {
	private static final int TOOLTIP_ENTRY_LIMIT = 5;

	private final int capacity;
	private final BagEquipmentRole equipmentRole;

	public BagItem(Properties properties, int capacity, BagEquipmentRole equipmentRole) {
		super(properties);
		if (capacity < 1 || capacity > BagContents.MAX_SERIALIZED_SLOTS) {
			throw new IllegalArgumentException("Bag capacity must be between 1 and " + BagContents.MAX_SERIALIZED_SLOTS);
		}
		this.capacity = capacity;
		this.equipmentRole = Objects.requireNonNull(equipmentRole, "equipmentRole");
	}

	public int capacity() {
		return this.capacity;
	}

	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(
		ItemStack bagStack,
		TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> builder,
		TooltipFlag tooltipFlag
	) {
		BagContents contents = bagStack
			.getOrDefault(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY)
			.normalized(this.capacity);
		List<ItemStack> storedStacks = new ArrayList<>();
		for (ItemStack stack : contents.copyItems(this.capacity)) {
			if (!stack.isEmpty()) {
				storedStacks.add(stack);
			}
		}

		builder.accept(Component.translatable(
			"tooltip.scout26.slots_used",
			storedStacks.size(),
			this.capacity
		).withStyle(ChatFormatting.GRAY));

		int displayed = Math.min(storedStacks.size(), TOOLTIP_ENTRY_LIMIT);
		for (int index = 0; index < displayed; index++) {
			ItemStack storedStack = storedStacks.get(index);
			builder.accept(Component.translatable(
				"tooltip.scout26.entry",
				storedStack.getHoverName(),
				storedStack.getCount()
			).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (storedStacks.size() > displayed) {
			builder.accept(Component.translatable(
				"tooltip.scout26.more",
				storedStacks.size() - displayed
			).withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
