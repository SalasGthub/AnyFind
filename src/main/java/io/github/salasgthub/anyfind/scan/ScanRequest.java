package io.github.salasgthub.anyfind.scan;

import io.github.salasgthub.anyfind.zone.Zone;
import io.github.salasgthub.anyfind.zone.ZoneStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Decides what to scan: the zone the player is standing in, or a cube around them when there is none.
 *
 * @param area              region to scan
 * @param zoneName          zone used, or an empty string when falling back to the radius
 * @param excludeStructures whether containers inside generated structures are skipped
 */
public record ScanRequest(BoundingBox area, String zoneName, boolean excludeStructures) {

    public static ScanRequest resolve(ServerLevel level, BlockPos origin, int radius, boolean excludeStructures) {
        Zone zone = ZoneStorage.of(level).zoneAt(origin);
        if (zone != null) {
            // Inside a zone the player said what to look at, so generated structures are not filtered out.
            return new ScanRequest(zone.area(), zone.name(), false);
        }
        return new ScanRequest(ContainerScanner.cubeAround(origin, radius), "", excludeStructures);
    }

    public ScanResult run(ServerLevel level) {
        return ContainerScanner.scan(level, area, excludeStructures);
    }
}
