package com.example.scout26;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Empty session-close intent; never contains bag state. */
public record CloseIntegratedInventoryPayload() implements CustomPacketPayload {
	public static final CloseIntegratedInventoryPayload INSTANCE = new CloseIntegratedInventoryPayload();
	public static final Type<CloseIntegratedInventoryPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(Scout26Mod.MOD_ID, "close_integrated_inventory")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, CloseIntegratedInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
