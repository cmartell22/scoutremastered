package io.github.cmartell22.scoutremastered;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Empty survival-inventory session intent; never contains bag state. */
public record OpenIntegratedInventoryPayload() implements CustomPacketPayload {
	public static final OpenIntegratedInventoryPayload INSTANCE = new OpenIntegratedInventoryPayload();
	public static final Type<OpenIntegratedInventoryPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "open_integrated_inventory")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenIntegratedInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
