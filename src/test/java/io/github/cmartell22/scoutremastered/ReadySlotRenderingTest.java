package io.github.cmartell22.scoutremastered;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadySlotRenderingTest {
	private static final Path CLIENT_ROOT = Path.of(
		"src/client/java/io/github/cmartell22/scoutremastered/client"
	);

	@Test
	void renderPolicyRetainsEveryRs1WhitelistAndSuppressionBranch() throws IOException {
		String renderer = source("BagTrinketRenderer.java");
		String policy = source("ReadySlotRenderPolicy.java");

		assertTrue(renderer.contains(".normalized(bagItem.capacity())"));
		assertTrue(renderer.contains(".getStack(0)"));
		assertTrue(renderer.contains("if (category == null)"));
		assertTrue(renderer.contains("appendItemLayers("));
		assertTrue(renderer.contains("readyStack,"));
		assertFalse(renderer.contains("itemRenderState, stack,"));

		for (String tag : new String[] {"SWORDS", "AXES", "PICKAXES", "SHOVELS", "HOES"}) {
			assertTrue(policy.contains("ItemTags." + tag));
		}
		for (String type : new String[] {"BowItem", "CrossbowItem", "ShieldItem", "TridentItem"}) {
			assertTrue(policy.contains("instanceof " + type));
		}
		assertTrue(policy.contains("if (stack.isEmpty())"));
		assertTrue(policy.contains("return null;"));
	}

	@Test
	void hipsUseStableMirroredRootSpaceInsteadOfTheAnimatedTorso() throws IOException {
		String transforms = source("ReadySlotTransforms.java");

		int hipStart = transforms.indexOf("private static void applyHip(");
		int backStart = transforms.indexOf("private static void applyBack(");
		String hipMethod = transforms.substring(hipStart, backStart);

		assertTrue(hipMethod.contains("model.root().translateAndRotate(poseStack)"));
		assertFalse(hipMethod.contains("translateToChest"));
		assertTrue(hipMethod.contains("position == Position.LEFT_HIP ? HIP_X : -HIP_X"));
		assertTrue(transforms.contains("private static final float HIP_X = 0.27F"));
		assertTrue(transforms.contains("private static final float HIP_Z = -0.20F"));
	}

	@Test
	void backRetainsTorsoMotionWithArmorAndCapeDepthClearance() throws IOException {
		String transforms = source("ReadySlotTransforms.java");
		String policy = source("ReadySlotRenderPolicy.java");

		assertTrue(transforms.contains("TrinketRenderer.translateToChest(poseStack, model, state)"));
		assertTrue(transforms.contains("private static final float BACK_Z = 0.44F"));
		assertTrue(transforms.contains("poseStack.mulPose(new Quaternionf().rotateY(Mth.PI))"));
		assertTrue(policy.contains("HANDHELD(0.40F, 0.72F)"));
		assertTrue(policy.contains("TRIDENT(0.52F, 0.88F)"));
	}

	@Test
	void rs5AddsNoRendererMixinOrCommonClientDependency() throws IOException {
		String mixins = Files.readString(Path.of("src/main/resources/scoutremastered.mixins.json"));
		assertFalse(mixins.contains("ReadySlot"));
		assertFalse(mixins.contains("AvatarRenderer"));

		try (var files = Files.walk(Path.of("src/main/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				assertFalse(source.contains("ReadySlotTransforms"), () -> "client transform reference in " + file);
				assertFalse(source.contains("ReadySlotRenderPolicy"), () -> "client policy reference in " + file);
			}
		}
	}

	private static String source(String filename) throws IOException {
		return Files.readString(CLIENT_ROOT.resolve(filename));
	}
}
