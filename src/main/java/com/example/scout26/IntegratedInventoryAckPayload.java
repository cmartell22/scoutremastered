package com.example.scout26;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-derived capacities only; vanilla container packets synchronize every bag ItemStack. */
public record IntegratedInventoryAckPayload(IntegratedInventoryData data) implements CustomPacketPayload {
	public static final Type<IntegratedInventoryAckPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(Scout26Mod.MOD_ID, "integrated_inventory_ack")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, IntegratedInventoryAckPayload> STREAM_CODEC = StreamCodec.of(
		(buffer, payload) -> IntegratedInventoryData.STREAM_CODEC.encode(buffer, payload.data),
		buffer -> new IntegratedInventoryAckPayload(IntegratedInventoryData.STREAM_CODEC.decode(buffer))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
