package io.github.cmartell22.scoutremastered;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Empty client-to-server intent requesting that the server discover and open equipped bags. */
public record OpenPackPayload() implements CustomPacketPayload {
	public static final OpenPackPayload INSTANCE = new OpenPackPayload();
	public static final Type<OpenPackPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "open_pack")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenPackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
