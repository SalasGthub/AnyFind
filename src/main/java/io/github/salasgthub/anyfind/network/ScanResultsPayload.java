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

import java.util.Comparator;
import java.util.List;

/**
 * Server → client: the result of a scan, already sorted by total count (highest first).
 */
/**
 * @param zoneName zone the scan used, or an empty string when it used the radius around the player
 */
public record ScanResultsPayload(int radius, String zoneName, int containerCount, int skippedLootContainers,
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
            ByteBufCodecs.stringUtf8(64), ScanResultsPayload::zoneName,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::containerCount,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::skippedLootContainers,
            ByteBufCodecs.VAR_INT, ScanResultsPayload::skippedStructureContainers,
            Entry.CODEC.apply(ByteBufCodecs.list()), ScanResultsPayload::entries,
            ScanResultsPayload::new);

    /**
     * Only the closest containers of each item travel: a custom payload cannot grow past 1 MiB, and the player
     * is guided to the closest one anyway. {@code total} still counts every container found.
     */
    public static final int MAX_LOCATIONS_PER_ITEM = 24;

    public static ScanResultsPayload from(ScanResult result, int radius, String zoneName, BlockPos origin) {
        List<Entry> entries = result.sortedByTotal().stream()
                .map(entry -> new Entry(entry.item(), entry.total(), closestLocations(entry, origin)))
                .toList();
        return new ScanResultsPayload(radius, zoneName, result.containerCount(), result.skippedLootContainers(),
                result.skippedStructureContainers(), entries);
    }

    private static List<Location> closestLocations(ScanResult.ItemEntry entry, BlockPos origin) {
        return entry.locations().entrySet().stream()
                .sorted(Comparator.comparingDouble(location -> location.getKey().distSqr(origin)))
                .limit(MAX_LOCATIONS_PER_ITEM)
                .map(location -> new Location(location.getKey(), location.getValue()))
                .toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
