package io.github.cmartell22.scoutremastered;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Rebinds client mirrors after vanilla has replaced the synchronized equipment ItemStacks. */
public record IntegratedInventoryReadyPayload(IntegratedInventoryData data) implements CustomPacketPayload {
	public static final Type<IntegratedInventoryReadyPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "integrated_inventory_ready")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, IntegratedInventoryReadyPayload> STREAM_CODEC = StreamCodec.of(
		(buffer, payload) -> IntegratedInventoryData.STREAM_CODEC.encode(buffer, payload.data),
		buffer -> new IntegratedInventoryReadyPayload(IntegratedInventoryData.STREAM_CODEC.decode(buffer))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
