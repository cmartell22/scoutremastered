package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.BagEquipmentRole;
import io.github.cmartell22.scoutremastered.BagContents;
import io.github.cmartell22.scoutremastered.BagItem;
import io.github.cmartell22.scoutremastered.ModDataComponents;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
import io.github.cmartell22.scoutremastered.TrinketsIntegration;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * <p>Scout uses dedicated left-hip, right-hip, and lower-back Trinkets slots. Ready Slots replaces
 * the equipped bag image with a conservative, read-only render of bag-local slot 0. Body-worn bags
 * intentionally have no first-person arm rendering.</p>
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
		String slotId = slot.slotType().getId();
		return switch (bagItem.equipmentRole()) {
			case SATCHEL -> slot.index() == TrinketsIntegration.SATCHEL_INDEX
				&& TrinketsIntegration.SATCHEL_SLOT.equals(slotId);
			case POUCH -> slot.index() == 0
				&& (TrinketsIntegration.LEFT_POUCH_SLOT.equals(slotId)
					|| TrinketsIntegration.RIGHT_POUCH_SLOT.equals(slotId));
		};
	}

	private static ReadySlotPresentationConfig.Position position(BagItem bagItem, TrinketSlotAccess slot) {
		if (bagItem.equipmentRole() == BagEquipmentRole.SATCHEL) {
			return ReadySlotPresentationConfig.Position.BACK;
		}
		return TrinketsIntegration.LEFT_POUCH_SLOT.equals(slot.slotType().getId())
			? ReadySlotPresentationConfig.Position.LEFT_HIP
			: ReadySlotPresentationConfig.Position.RIGHT_HIP;
	}
}
