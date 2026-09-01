package io.github.cmartell22.scoutremastered.client;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/** Fixed RS5 transforms for the three ready positions. */
final class ReadySlotTransforms {
	private static final float HIP_X = 0.27F;
	private static final float HIP_Y = 0.72F;
	private static final float HIP_Z = -0.20F;
	private static final float BACK_Y = 0.12F;
	private static final float BACK_Z = 0.44F;

	private ReadySlotTransforms() {
	}

	static void apply(
		PoseStack poseStack,
		HumanoidModel<?> model,
		HumanoidRenderState state,
		Position position,
		ReadySlotRenderPolicy.Category category
	) {
		if (position == Position.BACK) {
			applyBack(poseStack, model, state, category);
		} else {
			applyHip(poseStack, model, position, category);
		}
		poseStack.mulPose(new Quaternionf().rotateZ(Mth.PI));
	}

	private static void applyHip(
		PoseStack poseStack,
		HumanoidModel<?> model,
		Position position,
		ReadySlotRenderPolicy.Category category
	) {
		// The torso pitches by 0.5 radians while crouching. Anchoring hips to the root/pelvis
		// keeps their Z position stable instead of rotating them into the translated leg plane.
		model.root().translateAndRotate(poseStack);
		float x = position == Position.LEFT_HIP ? HIP_X : -HIP_X;
		poseStack.translate(x, HIP_Y, HIP_Z);
		float scale = category.hipScale();
		poseStack.scale(scale, scale, scale);
	}

	private static void applyBack(
		PoseStack poseStack,
		HumanoidModel<?> model,
		HumanoidRenderState state,
		ReadySlotRenderPolicy.Category category
	) {
		// Back items follow the animated torso. The resulting rest depth is 0.28 from the
		// body origin, beyond vanilla chest armor and cape's approximately 0.19 outer plane.
		TrinketRenderer.translateToChest(poseStack, model, state);
		poseStack.translate(0.0F, BACK_Y, BACK_Z);
		poseStack.mulPose(new Quaternionf().rotateY(Mth.PI));
		float scale = category.backScale();
		poseStack.scale(scale, scale, scale);
	}

	enum Position {
		LEFT_HIP,
		RIGHT_HIP,
		BACK
	}
}
