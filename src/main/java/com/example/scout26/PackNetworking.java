package com.example.scout26;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Registers the minimal open intent and performs all authoritative server-side discovery. */
public final class PackNetworking {
	private static final Component NO_BAGS = Component.translatable("message.scout26.no_equipped_bags");

	private PackNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(OpenPackPayload.TYPE, OpenPackPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenPackPayload.TYPE, (payload, context) -> openPack(context.player()));
	}

	static boolean canOpenPack(ServerPlayer player) {
		return player.isAlive()
			&& !player.isSpectator()
			&& player.containerMenu == player.inventoryMenu;
	}

	static void openPack(ServerPlayer player) {
		if (!canOpenPack(player)) {
			return;
		}

		TrinketsIntegration.EquippedBags bags = TrinketsIntegration.findEquippedBags(player);
		if (bags.isEmpty()) {
			player.sendSystemMessage(NO_BAGS);
			return;
		}

		player.openMenu(new PackMenuProvider(player, bags));
	}
}
