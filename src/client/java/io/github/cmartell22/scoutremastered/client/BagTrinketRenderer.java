package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.BagEquipmentRole;
import io.github.cmartell22.scoutremastered.BagContents;
import io.github.cmartell22.scoutremastered.BagItem;
import io.github.cmartell22.scoutremastered.ModDataComponents;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

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
		ReadySlotPresentationConfig.Category category = ReadySlotRenderPolicy.category(readyStack);
		if (category == null) {
			return;
		}

		poseStack.pushPose();
		ReadySlotTransforms.apply(
			poseStack,
			humanoidModel,
			humanoidState,
			position(bagItem, slot),
			category,
			ReadySlotRenderPolicy.itemId(readyStack)
		);

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

	private static ReadySlotPresentationConfig.Position position(BagItem bagItem, TrinketSlotAccess slot) {
		if (bagItem.equipmentRole() == BagEquipmentRole.SATCHEL) {
			return ReadySlotPresentationConfig.Position.BACK;
		}
		return slot.index() == TrinketsIntegration.LEFT_POUCH_INDEX
			? ReadySlotPresentationConfig.Position.LEFT_HIP
			: ReadySlotPresentationConfig.Position.RIGHT_HIP;
	}
}
