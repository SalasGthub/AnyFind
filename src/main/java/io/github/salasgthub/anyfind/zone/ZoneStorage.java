package io.github.salasgthub.anyfind.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.salasgthub.anyfind.Anyfind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The zones of one dimension, saved with the world.
 */
public class ZoneStorage extends SavedData {

    private static final Codec<ZoneStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Zone.CODEC.listOf().fieldOf("zones").forGetter(storage -> List.copyOf(storage.zones.values()))
    ).apply(instance, ZoneStorage::new));

    private static final SavedDataType<ZoneStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Anyfind.MOD_ID, "zones"),
            ZoneStorage::new,
            CODEC,
            DataFixTypes.LEVEL);

    private final Map<String, Zone> zones = new HashMap<>();

    private ZoneStorage() {
    }

    private ZoneStorage(List<Zone> loaded) {
        loaded.forEach(zone -> zones.put(zone.key(), zone));
    }

    public static ZoneStorage of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Zone> all() {
        return new ArrayList<>(zones.values());
    }

    public Zone get(String name) {
        return zones.get(name.toLowerCase(Locale.ROOT));
    }

    /** Replaces any zone with the same name; returns the previous one, or null. */
    public Zone put(Zone zone) {
        Zone previous = zones.put(zone.key(), zone);
        setDirty();
        return previous;
    }

    public Zone remove(String name) {
        Zone removed = zones.remove(name.toLowerCase(Locale.ROOT));
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    /** The zone the position is in; the smallest one when several overlap. */
    public Zone zoneAt(BlockPos pos) {
        return zones.values().stream()
                .filter(zone -> zone.contains(pos))
                .min(Comparator.comparingLong(Zone::volume))
                .orElse(null);
    }
}
