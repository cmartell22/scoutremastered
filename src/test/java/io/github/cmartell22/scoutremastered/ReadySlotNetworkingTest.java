package io.github.cmartell22.scoutremastered;


import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadySlotNetworkingTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.initialize();
	}

	@Test
	void rolesHaveStableExplicitNetworkIdsWithoutOrdinalLookup() {
		assertEquals(0, ReadySlotRole.LEFT_POUCH.networkId());
		assertEquals(1, ReadySlotRole.RIGHT_POUCH.networkId());
		assertEquals(2, ReadySlotRole.SATCHEL.networkId());
		for (ReadySlotRole role : ReadySlotRole.values()) {
			assertSame(role, ReadySlotRole.fromNetworkId(role.networkId()));
		}
	}

	@Test
	void everyNegativeAndOutOfRangeRoleIdIsRejectedDeterministically() {
		for (int id = -10_000; id < 0; id++) {
			assertRejectedRoleId(id);
		}
		for (int id = 3; id <= 10_000; id++) {
			assertRejectedRoleId(id);
		}
		assertRejectedRoleId(Integer.MIN_VALUE);
		assertRejectedRoleId(Integer.MAX_VALUE);
	}

	@Test
	void payloadRoundTripsEveryRoleAsExactlyOneVarInt() {
		assertEquals("scoutremastered:swap_ready_slot", SwapReadySlotPayload.TYPE.id().toString());
		for (ReadySlotRole role : ReadySlotRole.values()) {
			RegistryFriendlyByteBuf buffer = buffer();
			try {
				SwapReadySlotPayload payload = new SwapReadySlotPayload(role);
				SwapReadySlotPayload.STREAM_CODEC.encode(buffer, payload);
				assertEquals(1, buffer.readableBytes());
				assertEquals(role.networkId(), buffer.getUnsignedByte(buffer.readerIndex()));
				assertEquals(payload, SwapReadySlotPayload.STREAM_CODEC.decode(buffer));
				assertEquals(0, buffer.readableBytes());
			} finally {
				buffer.release();
			}
		}
	}

	@Test
	void payloadCodecRejectsMalformedRoleIdsWithoutFallback() {
		for (int id : new int[] {Integer.MIN_VALUE, -1, 3, 127, 128, Integer.MAX_VALUE}) {
			RegistryFriendlyByteBuf buffer = buffer();
			try {
				buffer.writeVarInt(id);
				IllegalArgumentException failure = assertThrows(
					IllegalArgumentException.class,
					() -> SwapReadySlotPayload.STREAM_CODEC.decode(buffer)
				);
				assertEquals("Unknown ready-slot role network ID: " + id, failure.getMessage());
			} finally {
				buffer.release();
			}
		}
	}

	@Test
	void payloadRecordContainsOnlyOneReadySlotRole() {
		RecordComponent[] components = SwapReadySlotPayload.class.getRecordComponents();
		assertEquals(1, components.length);
		assertEquals("role", components[0].getName());
		assertSame(ReadySlotRole.class, components[0].getType());
		assertThrows(NullPointerException.class, () -> new SwapReadySlotPayload(null));
	}

	@Test
	void commonInitializationRegistersTheServerboundCodecBeforeItsReceiver() {
		ReadySlotNetworking.initialize();
		assertTrue(ReadySlotNetworking.isInitialized());
		assertTrue(ServerPlayNetworking.getGlobalReceivers().contains(SwapReadySlotPayload.TYPE.id()));
	}

	@Test
	void receiverBoundaryDelegatesExactlyOneSelectedRoleAndPreservesTheServiceResult() {
		AtomicInteger calls = new AtomicInteger();
		AtomicReference<ReadySlotRole> selected = new AtomicReference<>();
		ReadySlotSwapService.Result result = ReadySlotNetworking.dispatch(
			new SwapReadySlotPayload(ReadySlotRole.RIGHT_POUCH),
			role -> {
				calls.incrementAndGet();
				selected.set(role);
				return ReadySlotSwapService.Result.STALE_BAG;
			}
		);

		assertEquals(ReadySlotSwapService.Result.STALE_BAG, result);
		assertEquals(1, calls.get());
		assertSame(ReadySlotRole.RIGHT_POUCH, selected.get());
		assertEquals(
			ReadySlotSwapService.Result.INVALID_REQUEST,
			ReadySlotNetworking.dispatch(null, role -> ReadySlotSwapService.Result.SUCCESS)
		);
		assertEquals(
			ReadySlotSwapService.Result.INVALID_REQUEST,
			ReadySlotNetworking.dispatch(new SwapReadySlotPayload(ReadySlotRole.SATCHEL), null)
		);
	}

	@Test
	void repeatedReceiverServiceRequestsConserveStacksAndNeverMutateAnotherRole() {
		for (ReadySlotRole role : ReadySlotRole.values()) {
			Fixture fixture = fixture();
			ItemStack handBefore = fixture.inventory().getSelectedItem().copy();
			ItemStack satchelBefore = ready(fixture.satchel()).copy();
			ItemStack leftBefore = ready(fixture.left()).copy();
			ItemStack rightBefore = ready(fixture.right()).copy();

			for (int request = 0; request < 1_000; request++) {
				assertEquals(
					ReadySlotSwapService.Result.SUCCESS,
					dispatchToService(new SwapReadySlotPayload(role), fixture)
				);
				if (role != ReadySlotRole.SATCHEL) {
					assertStack(satchelBefore, ready(fixture.satchel()));
				}
				if (role != ReadySlotRole.LEFT_POUCH) {
					assertStack(leftBefore, ready(fixture.left()));
				}
				if (role != ReadySlotRole.RIGHT_POUCH) {
					assertStack(rightBefore, ready(fixture.right()));
				}
			}

			assertStack(handBefore, fixture.inventory().getSelectedItem());
			assertStack(satchelBefore, ready(fixture.satchel()));
			assertStack(leftBefore, ready(fixture.left()));
			assertStack(rightBefore, ready(fixture.right()));
			assertEquals(1_000, fixture.synchronizations().get());
		}
	}

	@Test
	void receiverServicePathRejectsNestedMissingStaleReplacedUnequippedAndWrongRoleBags() {
		Fixture nested = fixture();
		nested.inventory().setSelectedItem(new ItemStack(ModItems.POUCH));
		ItemStack nestedHand = nested.inventory().getSelectedItem();
		BagContents nestedReady = nested.satchel().get().get(ModDataComponents.BAG_CONTENTS);
		assertEquals(
			ReadySlotSwapService.Result.HAND_NOT_STORABLE,
			dispatchToService(new SwapReadySlotPayload(ReadySlotRole.SATCHEL), nested)
		);
		assertSame(nestedHand, nested.inventory().getSelectedItem());
		assertSame(nestedReady, nested.satchel().get().get(ModDataComponents.BAG_CONTENTS));

		Fixture missing = fixture();
		TrinketsIntegration.EquippedBags noBags = new TrinketsIntegration.EquippedBags(
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		assertEquals(
			ReadySlotSwapService.Result.MISSING_BAG,
			dispatchToService(new SwapReadySlotPayload(ReadySlotRole.LEFT_POUCH), missing, noBags)
		);

		assertStaleAfterSlotChange(ItemStack.EMPTY);
		assertStaleAfterSlotChange(new ItemStack(ModItems.SATCHEL));

		Fixture wrongRole = fixture();
		TrinketsIntegration.EquippedBags rightHandleInLeftRole = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(wrongRole.satchel(), BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(wrongRole.right(), BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX)),
			Optional.empty()
		);
		assertEquals(
			ReadySlotSwapService.Result.WRONG_ROLE,
			dispatchToService(new SwapReadySlotPayload(ReadySlotRole.LEFT_POUCH), wrongRole, rightHandleInLeftRole)
		);
	}

	@Test
	void clientMappingsKeepSwapsRoleOnlyAndAddOnlyTheClientSideEditorKey() throws IOException {
		String readyKeys = Files.readString(Path.of(
			"src/client/java/io/github/cmartell22/scoutremastered/client/ReadySlotKeyMappings.java"
		));
		String clientEntrypoint = Files.readString(Path.of(
			"src/client/java/io/github/cmartell22/scoutremastered/client/ScoutRemasteredClient.java"
		));

		assertBinding(readyKeys, "SWAP_LEFT", "key.scoutremastered.swap_left_ready", "ReadySlotRole.LEFT_POUCH");
		assertBinding(readyKeys, "SWAP_RIGHT", "key.scoutremastered.swap_right_ready", "ReadySlotRole.RIGHT_POUCH");
		assertBinding(readyKeys, "SWAP_SATCHEL", "key.scoutremastered.swap_back_ready", "ReadySlotRole.SATCHEL");
		assertTrue(readyKeys.contains("InputConstants.UNKNOWN.getValue()"));
		assertTrue(readyKeys.contains("new SwapReadySlotPayload(role)"));
		assertFalse(readyKeys.contains("ItemStack"));
		assertTrue(readyKeys.contains("key.scoutremastered.open_ready_slots_editor"));
		assertTrue(readyKeys.contains("GLFW.GLFW_KEY_O"));
		assertTrue(readyKeys.contains("client.screen == null"));
		assertTrue(readyKeys.contains("client.setScreen(new ReadySlotConfigScreen())"));
		assertTrue(clientEntrypoint.contains("GLFW.GLFW_KEY_B"));
		assertTrue(clientEntrypoint.contains("ClientPlayNetworking.send(OpenPackPayload.INSTANCE)"));
		assertTrue(clientEntrypoint.contains("client.screen == null"));
	}

	@Test
	void commonServerSourcesRemainFreeOfClientOnlyClasses() throws IOException {
		Path commonRoot = Path.of("src/main/java");
		try (var files = Files.walk(commonRoot)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				assertFalse(source.contains("import net.minecraft.client."), () -> "client Minecraft import in " + file);
				assertFalse(source.contains("import net.fabricmc.fabric.api.client."), () -> "client Fabric import in " + file);
				assertFalse(source.contains("import org.lwjgl."), () -> "LWJGL import in " + file);
				assertFalse(source.contains("import com.mojang.blaze3d."), () -> "Blaze3D import in " + file);
			}
		}

		String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
		assertTrue(metadata.contains("io.github.cmartell22.scoutremastered.ScoutRemasteredMod"));
		assertTrue(metadata.contains("io.github.cmartell22.scoutremastered.client.ScoutRemasteredClient"));
		assertFalse(Files.readString(Path.of(
			"src/main/java/io/github/cmartell22/scoutremastered/ScoutRemasteredMod.java"
		)).contains("ReadySlotKeyMappings"));
	}

	@Test
	void openMenuPolicyUsesContainerZeroSynchronizationWithoutAReceiverMenuGate() throws IOException {
		String networking = Files.readString(Path.of(
			"src/main/java/io/github/cmartell22/scoutremastered/ReadySlotNetworking.java"
		));
		String service = Files.readString(Path.of(
			"src/main/java/io/github/cmartell22/scoutremastered/ReadySlotSwapService.java"
		));
		assertFalse(networking.contains("containerMenu"));
		assertTrue(networking.contains("ReadySlotSwapService.swap(player, role)"));
		assertTrue(service.contains("TrinketsIntegration.findEquippedBags(player)"));
		assertTrue(service.contains("player.inventoryMenu.broadcastChanges()"));
		assertFalse(service.contains("player.containerMenu == player.inventoryMenu"));
	}

	private static void assertRejectedRoleId(int id) {
		IllegalArgumentException failure = assertThrows(
			IllegalArgumentException.class,
			() -> ReadySlotRole.fromNetworkId(id)
		);
		assertEquals("Unknown ready-slot role network ID: " + id, failure.getMessage());
	}

	private static RegistryFriendlyByteBuf buffer() {
		return new RegistryFriendlyByteBuf(
			Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
		);
	}

	private static void assertBinding(String source, String field, String translationKey, String role) {
		assertTrue(source.contains(field + " = register(\"" + translationKey + "\")"));
		assertTrue(source.contains("sendClicks(client, " + field + ", " + role + ")"));
	}

	private static ReadySlotSwapService.Result dispatchToService(SwapReadySlotPayload payload, Fixture fixture) {
		return dispatchToService(payload, fixture, fixture.bags());
	}

	private static ReadySlotSwapService.Result dispatchToService(
		SwapReadySlotPayload payload,
		Fixture fixture,
		TrinketsIntegration.EquippedBags bags
	) {
		return ReadySlotNetworking.dispatch(
			payload,
			role -> ReadySlotSwapService.swap(
				fixture.inventory(),
				bags,
				role,
				fixture.synchronizations()::incrementAndGet
			)
		);
	}

	private static void assertStaleAfterSlotChange(ItemStack replacement) {
		Fixture fixture = fixture();
		ItemStack handBefore = fixture.inventory().getSelectedItem();
		ItemStack originalBag = fixture.satchel().get();
		BagContents readyBefore = originalBag.get(ModDataComponents.BAG_CONTENTS);
		fixture.satchel().set(replacement);

		assertEquals(
			ReadySlotSwapService.Result.STALE_BAG,
			dispatchToService(new SwapReadySlotPayload(ReadySlotRole.SATCHEL), fixture)
		);
		assertSame(handBefore, fixture.inventory().getSelectedItem());
		assertSame(readyBefore, originalBag.get(ModDataComponents.BAG_CONTENTS));
	}

	private static Fixture fixture() {
		Inventory inventory = new Inventory(null, null);
		inventory.setSelectedSlot(4);
		inventory.setSelectedItem(new ItemStack(Items.DIAMOND, 17));
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(ModItems.SATCHEL, new ItemStack(Items.GOLD_INGOT, 2)));
		AtomicReference<ItemStack> left = new AtomicReference<>(bag(ModItems.UPGRADED_POUCH, new ItemStack(Items.EMERALD, 3)));
		AtomicReference<ItemStack> right = new AtomicReference<>(bag(ModItems.UPGRADED_POUCH, new ItemStack(Items.APPLE, 4)));
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
		return new Fixture(inventory, satchel, left, right, bags, new AtomicInteger());
	}

	private static ItemStack bag(BagItem item, ItemStack ready) {
		ItemStack bag = new ItemStack(item);
		new BagContainer(bag).setItem(0, ready);
		return bag;
	}

	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> slot,
		BagEquipmentRole role,
		String slotId,
		int index
	) {
		return EquippedBagHandle.capture(slotId, index, role, slot::get).orElseThrow();
	}

	private static ItemStack ready(AtomicReference<ItemStack> bag) {
		return bag.get().get(ModDataComponents.BAG_CONTENTS).getStack(0);
	}

	private static void assertStack(ItemStack expected, ItemStack actual) {
		assertTrue(ItemStack.matches(expected, actual), () -> "Expected " + expected + " but got " + actual);
	}

	private record Fixture(
		Inventory inventory,
		AtomicReference<ItemStack> satchel,
		AtomicReference<ItemStack> left,
		AtomicReference<ItemStack> right,
		TrinketsIntegration.EquippedBags bags,
		AtomicInteger synchronizations
	) {
	}
}
