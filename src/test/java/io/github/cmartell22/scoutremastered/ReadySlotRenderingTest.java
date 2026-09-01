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

		assertTrue(transforms.contains("if (position == Position.BACK)"));
		assertTrue(transforms.contains("model.root().translateAndRotate(poseStack)"));
		assertTrue(transforms.contains("ReadySlotConfig.current().resolve(position, category, itemId)"));
		assertFalse(transforms.contains("HIP_X"));
		assertFalse(transforms.contains("HIP_Z"));
	}

	@Test
	void backRetainsTorsoMotionWithArmorAndCapeDepthClearance() throws IOException {
		String transforms = source("ReadySlotTransforms.java");

		assertTrue(transforms.contains("TrinketRenderer.translateToChest(poseStack, model, state)"));
		assertTrue(transforms.contains("transform.rotateY()"));
		assertTrue(transforms.contains("transform.rotateZ()"));
		assertTrue(transforms.contains("transform.scale()"));
		assertFalse(transforms.contains("BACK_Z"));
	}

	@Test
	void rs6ConfigIsClientLoadedAndFailsClosedWithoutMutatingTheExternalFile() throws IOException {
		String bootstrap = source("ScoutRemasteredClient.java");
		String loader = source("ReadySlotConfig.java");
		String policy = source("ReadySlotRenderPolicy.java");

		assertTrue(bootstrap.contains("ReadySlotConfig.load()"));
		assertTrue(loader.contains("FabricLoader.getInstance().getConfigDir()"));
		assertTrue(loader.contains("scoutremastered-ready-slots.json"));
		assertTrue(loader.contains("current = bundled.config()"));
		assertTrue(loader.contains("leaving the file untouched"));
		assertTrue(policy.indexOf("itemBlacklisted(itemId)") < policy.indexOf("whitelistedCategory(itemId)"));
		assertTrue(policy.indexOf("whitelistedCategory(itemId)") < policy.indexOf("categoryEnabled(builtIn)"));
	}

	@Test
	void rs7aEditorHasLivePreviewBoundedControlsAndExplicitSaveCancelSemantics() throws IOException {
		String editor = source("ReadySlotConfigScreen.java");
		String loader = source("ReadySlotConfig.java");
		String keys = source("ReadySlotKeyMappings.java");

		assertTrue(keys.contains("open_ready_slots_editor"));
		assertTrue(keys.contains("new ReadySlotConfigScreen()"));
		assertFalse(editor.contains("InventoryScreen.extractEntityInInventoryFollowsMouse("));
		assertTrue(editor.contains("extractEntityPreview("));
		assertTrue(editor.contains("this.previewYaw -= 15.0F"));
		assertTrue(editor.contains("this.previewPitch = 0.0F"));
		assertTrue(editor.contains("ReadySlotConfig.preview(updated)"));
		assertTrue(editor.contains("ReadySlotConfig.save(this.draft)"));
		assertTrue(editor.contains("ReadySlotConfig.restore(this.openingConfig)"));
		assertTrue(editor.contains("ReadySlotPresentationConfig.MIN_TRANSLATION"));
		assertTrue(editor.contains("ReadySlotPresentationConfig.MAX_ROTATION"));
		assertTrue(editor.contains("ReadySlotPresentationConfig.MAX_SCALE"));
		assertTrue(editor.contains("withBaseTransformPropagatingOverrides"));
		assertTrue(editor.contains("BuiltInRegistries.ITEM.containsKey(identifier)"));
		assertTrue(editor.contains("mirrorField(field)"));
		assertTrue(editor.contains("float next = value - 90.0F"));
		assertTrue(editor.contains("ContainerObjectSelectionList<PolicyEntry>"));
		assertTrue(editor.contains("resetPolicyList()"));
		assertTrue(editor.contains("clearPolicyList()"));
		assertTrue(editor.contains("ready_slots.visible"));
		assertTrue(editor.contains("SCALE(\"Scale\""));
		assertTrue(loader.contains("ReadySlotPresentationConfigFile.save(path, config)"));
		assertFalse(editor.contains("ClientPlayNetworking"));
		assertFalse(editor.contains("ReadySlotSwapService"));
	}

	@Test
	void rs7aGranularCategoriesUseExact2612TagsAndRetainLegacyCompatibility() throws IOException {
		String policy = source("ReadySlotRenderPolicy.java");
		String editor = source("ReadySlotConfigScreen.java");

		for (String mapping : new String[] {
			"ItemTags.SWORDS", "Category.SWORD",
			"ItemTags.AXES", "Category.AXE",
			"ItemTags.PICKAXES", "Category.PICKAXE",
			"ItemTags.SHOVELS", "Category.SHOVEL",
			"ItemTags.HOES", "Category.HOE",
			"ItemTags.SPEARS", "Category.SPEAR"
		}) {
			assertTrue(policy.contains(mapping));
		}
		assertFalse(editor.contains("Category.HANDHELD,"));
		assertTrue(editor.contains("withWhitelistedItem(this.itemId, this.category)"));
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
