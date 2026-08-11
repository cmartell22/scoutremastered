package com.example.scout26.client;

import com.example.scout26.BagEquipmentRole;
import com.example.scout26.BagItem;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Client-only third-person renderer for equipped bags.
 *
 * <p>The pinned Trinkets data renderer can select a belt slot but cannot branch on its index, so a
 * small code renderer is used to keep belt index 0 on the wearer's left and index 1 on the right.
 * It renders the normal item model and therefore reuses the same finalized texture as inventories.
 * Body-worn bags intentionally have no first-person arm rendering.</p>
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
			|| !(state instanceof HumanoidRenderState humanoidState)) {
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

		ItemStackRenderState itemRenderState = new ItemStackRenderState();
		Minecraft.getInstance().getItemModelResolver().appendItemLayers(
			itemRenderState,
			stack,
			ItemDisplayContext.FIXED,
			null,
			null,
			0
		);
		itemRenderState.submit(poseStack, submit, light, 0, 0);
		poseStack.popPose();
	}
}
