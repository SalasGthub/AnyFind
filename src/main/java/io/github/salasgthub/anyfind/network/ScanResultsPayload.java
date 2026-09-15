package io.github.salasgthub.anyfind.network;

import io.github.salasgthub.anyfind.Anyfind;
import io.github.salasgthub.anyfind.scan.ScanResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Server → client: the result of a scan, already sorted by total count (highest first).
 */
public record ScanResultsPayload(int radius, int containerCount, int skippedLootContainers,
                                 int skippedStructureContainers, List<Entry> entries)
        implements CustomPacketPayload {

    public static final Type<ScanResultsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Anyfind.MOD_ID, "scan_results"));

    public record Location(BlockPos pos, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Location> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Location::pos,
                ByteBufCodecs.VAR_INT, Location::count,
                Location::new);
    }

    public record Entry(Item item, int total, List<Location> locations) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Entry::item,
                ByteBufCodecs.VAR_INT, Entry::total,
                Location.CODEC.apply(ByteBufCodecs.list()), Entry::locations,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanResultsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScanResultsPayload::radius,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::containerCount,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::skippedLootContainers,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::skippedStructureContainers,
            Entry.CODEC.apply(ByteBufCodecs.list()), ScanResultsPayload::entries,
            ScanResultsPayload::new);

    public static ScanResultsPayload from(ScanResult result, int radius) {
        List<Entry> entries = result.sortedByTotal().stream()
                .map(entry -> new Entry(entry.item(), entry.total(), entry.locations().entrySet().stream()
                        .map(location -> new Location(location.getKey(), location.getValue()))
                        .toList()))
                .toList();
        return new ScanResultsPayload(radius, result.containerCount(), result.skippedLootContainers(),
                result.skippedStructureContainers(), entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
