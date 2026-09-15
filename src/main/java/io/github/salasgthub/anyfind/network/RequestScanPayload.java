package io.github.salasgthub.anyfind.network;

import io.github.salasgthub.anyfind.Anyfind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: asks for a scan of the containers around the player.
 */
public record RequestScanPayload(int radius, boolean excludeStructures) implements CustomPacketPayload {

    public static final Type<RequestScanPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Anyfind.MOD_ID, "request_scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestScanPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestScanPayload::radius,
            ByteBufCodecs.BOOL, RequestScanPayload::excludeStructures,
            RequestScanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
