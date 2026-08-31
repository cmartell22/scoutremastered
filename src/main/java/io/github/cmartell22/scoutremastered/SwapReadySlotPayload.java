package io.github.cmartell22.scoutremastered;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Role-only client intent; every inventory owner and slot is selected by the server. */
public record SwapReadySlotPayload(ReadySlotRole role) implements CustomPacketPayload {
	public static final Type<SwapReadySlotPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "swap_ready_slot")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, SwapReadySlotPayload> STREAM_CODEC = StreamCodec.of(
		(buffer, payload) -> buffer.writeVarInt(payload.role.networkId()),
		buffer -> new SwapReadySlotPayload(ReadySlotRole.fromNetworkId(buffer.readVarInt()))
	);

	public SwapReadySlotPayload {
		Objects.requireNonNull(role, "role");
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
