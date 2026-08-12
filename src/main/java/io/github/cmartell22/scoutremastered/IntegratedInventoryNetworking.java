package io.github.cmartell22.scoutremastered;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative ADR-012 session lifecycle. */
public final class IntegratedInventoryNetworking {
	private IntegratedInventoryNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(OpenIntegratedInventoryPayload.TYPE, OpenIntegratedInventoryPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CloseIntegratedInventoryPayload.TYPE, CloseIntegratedInventoryPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(IntegratedInventoryAckPayload.TYPE, IntegratedInventoryAckPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(IntegratedInventoryReadyPayload.TYPE, IntegratedInventoryReadyPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(
			OpenIntegratedInventoryPayload.TYPE,
			(payload, context) -> open(context.player())
		);
		ServerPlayNetworking.registerGlobalReceiver(
			CloseIntegratedInventoryPayload.TYPE,
			(payload, context) -> close(context.player())
		);
	}

	static boolean canOpen(ServerPlayer player) {
		return player.isAlive()
			&& !player.isRemoved()
			&& !player.isSpectator()
			&& !player.hasInfiniteMaterials()
			&& !player.hasDisconnected()
			&& player.containerMenu == player.inventoryMenu
			&& player.inventoryMenu instanceof IntegratedInventoryMenu;
	}

	static void open(ServerPlayer player) {
		if (!canOpen(player)) {
			close(player);
			return;
		}
		IntegratedInventoryMenu menu = (IntegratedInventoryMenu)player.inventoryMenu;
		menu.scoutremastered$activateServer(TrinketsIntegration.findEquippedBags(player));
		IntegratedInventoryData data = menu.scoutremastered$integratedInventoryData();
		ServerPlayNetworking.send(player, new IntegratedInventoryAckPayload(data));
		// Same connection ordering activates the client binding before vanilla slot contents arrive.
		player.inventoryMenu.sendAllDataToRemote();
		// The full sync replaces Trinkets' client-side ItemStack object. Rebind to that exact new
		// object only after all vanilla slot contents have been applied, preserving the mirrors.
		ServerPlayNetworking.send(player, new IntegratedInventoryReadyPayload(data));
	}

	static void close(ServerPlayer player) {
		if (player.inventoryMenu instanceof IntegratedInventoryMenu menu) {
			menu.scoutremastered$deactivateIntegratedInventory();
			player.inventoryMenu.broadcastChanges();
		}
	}
}
