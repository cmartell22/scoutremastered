package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.BagEquipmentRole;
import io.github.cmartell22.scoutremastered.BagContents;
import io.github.cmartell22.scoutremastered.BagItem;
import io.github.cmartell22.scoutremastered.ModDataComponents;
import io.github.cmartell22.scoutremastered.TrinketsIntegration;
import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.DefaultTrinketSlots;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import org.joml.Quaternionf;

/**
 * Client-only third-person renderer for equipped bags.
 *
 * <p>The pinned Trinkets data renderer can select a belt slot but cannot branch on its index, so a
 * small code renderer is used to keep belt index 0 on the wearer's left and index 1 on the right.
 * Ready Slots replaces the old equipped bag image with a conservative, read-only render of bag-local
 * slot 0. Body-worn bags intentionally have no first-person arm rendering.</p>
 */
final class BagTrinketRenderer implements TrinketRenderer {
	static final BagTrinketRenderer INSTANCE = new BagTrinketRenderer();

	private BagTrinketRenderer() {
	}

	@Override
	public void submit(
		ItemStack stack,
		TrinketSlotAccess slot,
		EntityModel<? extends LivingEntityRenderState> contextModel,
		PoseStack poseStack,
		SubmitNodeCollector submit,
		int light,
		LivingEntityRenderState state,
		float limbAngle,
		float limbDistance
	) {
		if (!(stack.getItem() instanceof BagItem bagItem)
			|| !(contextModel instanceof HumanoidModel<?> humanoidModel)
			|| !(state instanceof HumanoidRenderState humanoidState)
			|| !isExpectedEquipmentSlot(bagItem, slot)) {
			return;
		}

		ItemStack readyStack = stack
			.getOrDefault(ModDataComponents.BAG_CONTENTS, BagContents.EMPTY)
			.normalized(bagItem.capacity())
			.getStack(0);
		if (!isReadyStackVisible(readyStack)) {
			return;
		}

		poseStack.pushPose();
		TrinketRenderer.translateToChest(poseStack, humanoidModel, humanoidState);
		if (bagItem.equipmentRole() == BagEquipmentRole.SATCHEL) {
			poseStack.translate(0.0F, 0.13F, 0.36F);
			poseStack.mulPose(new Quaternionf().rotateY(Mth.PI));
			poseStack.scale(0.72F, 0.72F, 0.72F);
		} else {
			float horizontalOffset = slot.index() == 0 ? 0.22F : -0.22F;
			poseStack.translate(horizontalOffset, 0.34F, 0.01F);
			poseStack.scale(0.38F, 0.38F, 0.38F);
		}
		poseStack.mulPose(new Quaternionf().rotateZ(Mth.PI));

		ItemStackRenderState itemRenderState = new ItemStackRenderState();
		Minecraft.getInstance().getItemModelResolver().appendItemLayers(
			itemRenderState,
			readyStack,
			ItemDisplayContext.FIXED,
			null,
			null,
			0
		);
		itemRenderState.submit(poseStack, submit, light, 0, 0);
		poseStack.popPose();
	}

	private static boolean isExpectedEquipmentSlot(BagItem bagItem, TrinketSlotAccess slot) {
		return switch (bagItem.equipmentRole()) {
			case SATCHEL -> slot.index() == TrinketsIntegration.SATCHEL_INDEX
				&& DefaultTrinketSlots.CHEST_BACK.equals(slot.slotType().getId());
			case POUCH -> (slot.index() == TrinketsIntegration.LEFT_POUCH_INDEX
				|| slot.index() == TrinketsIntegration.RIGHT_POUCH_INDEX)
				&& DefaultTrinketSlots.LEGS_BELT.equals(slot.slotType().getId());
		};
	}

	private static boolean isReadyStackVisible(ItemStack stack) {
		return !stack.isEmpty()
			&& (stack.is(ItemTags.SWORDS)
				|| stack.is(ItemTags.AXES)
				|| stack.is(ItemTags.PICKAXES)
				|| stack.is(ItemTags.SHOVELS)
				|| stack.is(ItemTags.HOES)
				|| stack.getItem() instanceof BowItem
				|| stack.getItem() instanceof CrossbowItem
				|| stack.getItem() instanceof TridentItem
				|| stack.getItem() instanceof ShieldItem);
	}
}
