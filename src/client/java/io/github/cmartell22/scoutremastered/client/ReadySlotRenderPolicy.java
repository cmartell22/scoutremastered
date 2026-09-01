package io.github.cmartell22.scoutremastered.client;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import org.jspecify.annotations.Nullable;

/** Read-only visibility and transform category selection for ready-slot item stacks. */
final class ReadySlotRenderPolicy {
	private ReadySlotRenderPolicy() {
	}

	static @Nullable Category category(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (stack.is(ItemTags.SWORDS)
			|| stack.is(ItemTags.AXES)
			|| stack.is(ItemTags.PICKAXES)
			|| stack.is(ItemTags.SHOVELS)
			|| stack.is(ItemTags.HOES)) {
			return Category.HANDHELD;
		}
		if (stack.getItem() instanceof BowItem) {
			return Category.BOW;
		}
		if (stack.getItem() instanceof CrossbowItem) {
			return Category.CROSSBOW;
		}
		if (stack.getItem() instanceof ShieldItem) {
			return Category.SHIELD;
		}
		if (stack.getItem() instanceof TridentItem) {
			return Category.TRIDENT;
		}
		return null;
	}

	enum Category {
		HANDHELD(0.40F, 0.72F),
		BOW(0.42F, 0.72F),
		CROSSBOW(0.36F, 0.64F),
		SHIELD(0.44F, 0.74F),
		TRIDENT(0.52F, 0.88F);

		private final float hipScale;
		private final float backScale;

		Category(float hipScale, float backScale) {
			this.hipScale = hipScale;
			this.backScale = backScale;
		}

		float hipScale() {
			return this.hipScale;
		}

		float backScale() {
			return this.backScale;
		}
	}
}
