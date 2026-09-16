package io.github.salasgthub.anyfind.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index built by a scan: for each item, how many there are and in which containers.
 */
public class ScanResult {

    private final Map<Item, ItemEntry> entries = new HashMap<>();
    private final Set<BlockPos> containers = new HashSet<>();
    private int skippedLootContainers;
    private int skippedStructureContainers;
    private boolean truncated;

    void addContainer(BlockPos pos) {
        containers.add(pos);
    }

    void addItem(Item item, BlockPos containerPos, int count) {
        entries.computeIfAbsent(item, ItemEntry::new).add(containerPos, count);
    }

    void markSkippedLoot() {
        skippedLootContainers++;
    }

    void markSkippedStructure() {
        skippedStructureContainers++;
    }

    void markTruncated() {
        truncated = true;
    }

    /** True when the scan hit {@link ContainerScanner#MAX_CONTAINERS} and stopped early. */
    public boolean truncated() {
        return truncated;
    }

    public int skippedStructureContainers() {
        return skippedStructureContainers;
    }

    public int containerCount() {
        return containers.size();
    }

    public int skippedLootContainers() {
        return skippedLootContainers;
    }

    public int distinctItemCount() {
        return entries.size();
    }

    public ItemEntry get(Item item) {
        return entries.get(item);
    }

    /** Entries sorted by total count, highest first. */
    public List<ItemEntry> sortedByTotal() {
        return entries.values().stream()
                .sorted(Comparator.comparingInt(ItemEntry::total).reversed())
                .toList();
    }

    public static class ItemEntry {
        private final Item item;
        private final Map<BlockPos, Integer> locations = new HashMap<>();
        private int total;

        ItemEntry(Item item) {
            this.item = item;
        }

        void add(BlockPos pos, int count) {
            locations.merge(pos, count, Integer::sum);
            total += count;
        }

        public Item item() {
            return item;
        }

        public int total() {
            return total;
        }

        /** Container position → amount of this item inside it. */
        public Map<BlockPos, Integer> locations() {
            return locations;
        }

        public BlockPos closestTo(BlockPos origin) {
            return locations.keySet().stream()
                    .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                    .orElse(null);
        }
    }
}
