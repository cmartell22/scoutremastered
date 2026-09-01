package io.github.cmartell22.scoutremastered.client;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Category;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Position;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Transform;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.joml.Quaternionf;

/** Applies validated RS6 presentation transforms while retaining the accepted RS5 model anchors. */
final class ReadySlotTransforms {
	private ReadySlotTransforms() {
	}

	static void apply(
		PoseStack poseStack,
		HumanoidModel<?> model,
		HumanoidRenderState state,
		Position position,
		Category category,
		String itemId
	) {
		Transform transform = ReadySlotConfig.current().resolve(position, category, itemId);
		if (position == Position.BACK) {
			TrinketRenderer.translateToChest(poseStack, model, state);
		} else {
			// Crouching pitches the torso. Root/pelvis space keeps hip Z stable outside the leg plane.
			model.root().translateAndRotate(poseStack);
		}
		applyConfiguredTransform(poseStack, transform);
	}

	private static void applyConfiguredTransform(PoseStack poseStack, Transform transform) {
		poseStack.translate(transform.translateX(), transform.translateY(), transform.translateZ());
		float toRadians = (float) (Math.PI / 180.0);
		if (transform.rotateX() != 0.0F) {
			poseStack.mulPose(new Quaternionf().rotateX(transform.rotateX() * toRadians));
		}
		if (transform.rotateY() != 0.0F) {
			poseStack.mulPose(new Quaternionf().rotateY(transform.rotateY() * toRadians));
		}
		if (transform.rotateZ() != 0.0F) {
			poseStack.mulPose(new Quaternionf().rotateZ(transform.rotateZ() * toRadians));
		}
		poseStack.scale(transform.scale(), transform.scale(), transform.scale());
	}
}
