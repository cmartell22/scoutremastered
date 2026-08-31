package io.github.cmartell22.scoutremastered;

import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Registers the role-only ready-slot intent and delegates all authority to the existing swap service. */
public final class ReadySlotNetworking {
	private static boolean initialized;

	private ReadySlotNetworking() {
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		PayloadTypeRegistry.serverboundPlay().register(SwapReadySlotPayload.TYPE, SwapReadySlotPayload.STREAM_CODEC);
		boolean receiverRegistered = ServerPlayNetworking.registerGlobalReceiver(
			SwapReadySlotPayload.TYPE,
			(payload, context) -> receive(payload, context.player())
		);
		if (!receiverRegistered) {
			throw new IllegalStateException("Ready-slot server receiver was already registered");
		}
		initialized = true;
	}

	static ReadySlotSwapService.Result receive(SwapReadySlotPayload payload, ServerPlayer player) {
		return dispatch(payload, role -> ReadySlotSwapService.swap(player, role));
	}

	/** Testable intent boundary shared by the real Fabric receiver and direct service-path tests. */
	static ReadySlotSwapService.Result dispatch(
		SwapReadySlotPayload payload,
		Function<ReadySlotRole, ReadySlotSwapService.Result> swapper
	) {
		if (payload == null || swapper == null) {
			return ReadySlotSwapService.Result.INVALID_REQUEST;
		}
		return swapper.apply(payload.role());
	}

	static boolean isInitialized() {
		return initialized;
	}
}
