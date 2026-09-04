package io.github.cmartell22.scoutremastered;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrinketsIntegrationTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void bagRolesMapOnlyToTheirDedicatedScoutSlots() {
		assertEquals(BagEquipmentRole.SATCHEL, ModItems.SATCHEL.equipmentRole());
		assertEquals(BagEquipmentRole.SATCHEL, ModItems.UPGRADED_SATCHEL.equipmentRole());
		assertEquals(BagEquipmentRole.POUCH, ModItems.POUCH.equipmentRole());
		assertEquals(BagEquipmentRole.POUCH, ModItems.UPGRADED_POUCH.equipmentRole());

		assertTrue(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT));
		assertFalse(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.SATCHEL, TrinketsIntegration.LEFT_POUCH_SLOT));
		assertFalse(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.SATCHEL, "chest/back"));

		assertTrue(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT));
		assertTrue(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT));
		assertFalse(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.POUCH, TrinketsIntegration.SATCHEL_SLOT));
		assertFalse(TrinketsIntegration.isAllowedSlot(BagEquipmentRole.POUCH, "legs/belt"));
	}

	@Test
	void handleRejectsWrongRoleAndFailsClosedAfterReplacement() {
		ItemStack original = new ItemStack(ModItems.SATCHEL);
		BagContainer contents = new BagContainer(original);
		contents.setItem(0, new ItemStack(Items.DIAMOND, 5));
		AtomicReference<ItemStack> liveSlot = new AtomicReference<>(original);

		EquippedBagHandle handle = EquippedBagHandle.capture(
			TrinketsIntegration.SATCHEL_SLOT,
			TrinketsIntegration.SATCHEL_INDEX,
			BagEquipmentRole.SATCHEL,
			liveSlot::get
		).orElseThrow();

		assertTrue(handle.isValid());
		assertSame(original, handle.resolve().orElseThrow());
		assertTrue(EquippedBagHandle.capture(
			TrinketsIntegration.SATCHEL_SLOT,
			TrinketsIntegration.SATCHEL_INDEX,
			BagEquipmentRole.POUCH,
			liveSlot::get
		).isEmpty());

		ItemStack replacement = new ItemStack(ModItems.SATCHEL);
		liveSlot.set(replacement);
		assertFalse(handle.isValid());
		assertEquals(5, original.get(ModDataComponents.BAG_CONTENTS).getStack(0).getCount());
		assertTrue(replacement.get(ModDataComponents.BAG_CONTENTS).isEmpty());

		liveSlot.set(ItemStack.EMPTY);
		assertFalse(handle.isValid());
	}

	@Test
	void equippedBagOrderingIsSatchelThenLeftThenRight() {
		EquippedBagHandle satchel = capture(
			new ItemStack(ModItems.SATCHEL),
			TrinketsIntegration.SATCHEL_SLOT,
			TrinketsIntegration.SATCHEL_INDEX,
			BagEquipmentRole.SATCHEL
		);
		EquippedBagHandle left = capture(
			new ItemStack(ModItems.POUCH),
			TrinketsIntegration.LEFT_POUCH_SLOT,
			TrinketsIntegration.LEFT_POUCH_INDEX,
			BagEquipmentRole.POUCH
		);
		EquippedBagHandle right = capture(
			new ItemStack(ModItems.UPGRADED_POUCH),
			TrinketsIntegration.RIGHT_POUCH_SLOT,
			TrinketsIntegration.RIGHT_POUCH_INDEX,
			BagEquipmentRole.POUCH
		);

		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			java.util.Optional.of(satchel),
			java.util.Optional.of(left),
			java.util.Optional.of(right)
		);
		assertEquals(List.of(satchel, left, right), bags.inStableOrder());
		assertEquals(TrinketsIntegration.LEFT_POUCH_SLOT, bags.leftPouch().orElseThrow().slotId());
		assertEquals(TrinketsIntegration.RIGHT_POUCH_SLOT, bags.rightPouch().orElseThrow().slotId());
		assertEquals(0, bags.leftPouch().orElseThrow().slotIndex());
		assertEquals(0, bags.rightPouch().orElseThrow().slotIndex());
	}

	@Test
	void packagedDataRequestsDedicatedScoutSlotsWithCorrectItemTags() throws IOException {
		assertTrue(readJsonResources("data/trinkets/entities/scoutremastered.json").stream().anyMatch(entities ->
			List.of("minecraft:player").equals(strings(entities, "entities"))
				&& Set.of(
					TrinketsIntegration.SATCHEL_SLOT,
					TrinketsIntegration.LEFT_POUCH_SLOT,
					TrinketsIntegration.RIGHT_POUCH_SLOT
				).equals(Set.copyOf(strings(entities, "slots")))
		));

		assertDedicatedSlot("data/trinkets/slots/chest/lower_back.json", "trinkets:container/slots/back");
		assertDedicatedSlot("data/trinkets/slots/legs/left_hip.json", "trinkets:container/slots/belt");
		assertDedicatedSlot("data/trinkets/slots/legs/right_hip.json", "trinkets:container/slots/belt");

		assertTrue(readJsonResources("data/trinkets/tags/item/chest/lower_back.json").stream().anyMatch(tag ->
			Set.of("scoutremastered:satchel", "scoutremastered:upgraded_satchel").equals(Set.copyOf(strings(tag, "values")))
		));

		Set<String> pouchItems = Set.of("scoutremastered:pouch", "scoutremastered:upgraded_pouch");
		assertTrue(readJsonResources("data/trinkets/tags/item/legs/left_hip.json").stream().anyMatch(tag ->
			pouchItems.equals(Set.copyOf(strings(tag, "values")))
		));
		assertTrue(readJsonResources("data/trinkets/tags/item/legs/right_hip.json").stream().anyMatch(tag ->
			pouchItems.equals(Set.copyOf(strings(tag, "values")))
		));
	}

	private static void assertDedicatedSlot(String path, String icon) throws IOException {
		assertTrue(readJsonResources(path).stream().anyMatch(slot ->
			slot.has("amount")
				&& slot.get("amount").getAsInt() == 1
				&& slot.has("icon")
				&& icon.equals(slot.get("icon").getAsString())
		));
	}

	private static EquippedBagHandle capture(
		ItemStack stack,
		String slotId,
		int slotIndex,
		BagEquipmentRole role
	) {
		return EquippedBagHandle.capture(slotId, slotIndex, role, () -> stack).orElseThrow();
	}

	private static List<JsonObject> readJsonResources(String path) throws IOException {
		List<JsonObject> resources = new ArrayList<>();
		var urls = TrinketsIntegrationTest.class.getClassLoader().getResources(path);
		while (urls.hasMoreElements()) {
			try (
				var input = urls.nextElement().openStream();
				var reader = new InputStreamReader(input, StandardCharsets.UTF_8)
			) {
				resources.add(JsonParser.parseReader(reader).getAsJsonObject());
			}
		}
		return List.copyOf(resources);
	}

	private static List<String> strings(JsonObject object, String field) {
		return object.getAsJsonArray(field).asList().stream().map(element -> element.getAsString()).toList();
	}
}
