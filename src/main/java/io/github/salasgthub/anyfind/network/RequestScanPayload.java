package io.github.salasgthub.anyfind.network;

import io.github.salasgthub.anyfind.Anyfind;
import io.github.salasgthub.anyfind.scan.ScanOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: asks for a scan of the containers around the player.
 */
public record RequestScanPayload(int radius, boolean excludeStructures, boolean includeOtherContainers,
                                 boolean includeNestedContainers) implements CustomPacketPayload {

    public static final Type<RequestScanPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Anyfind.MOD_ID, "request_scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestScanPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestScanPayload::radius,
            ByteBufCodecs.BOOL, RequestScanPayload::excludeStructures,
            ByteBufCodecs.BOOL, RequestScanPayload::includeOtherContainers,
            ByteBufCodecs.BOOL, RequestScanPayload::includeNestedContainers,
            RequestScanPayload::new);

    public ScanOptions options() {
        return new ScanOptions(includeOtherContainers, includeNestedContainers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
