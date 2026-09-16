package io.github.salasgthub.anyfind.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Reads the contents of containers around a position on the server.
 * <p>
 * Walks the block entity list of each loaded chunk in range instead of every block, and never
 * loads new chunks.
 */
public final class ContainerScanner {

    /** Upper bound per scan, so a huge zone cannot stall the server thread. */
    public static final int MAX_CONTAINERS = 2000;
    /** A shulker box inside a shulker box is as deep as vanilla goes. */
    private static final int MAX_NESTING = 2;

    private ContainerScanner() {
    }

    /** Scans the cube of side {@code 2 * radius + 1} centered on {@code center}. */
    public static ScanResult scan(ServerLevel level, BlockPos center, int radius, boolean excludeStructures) {
        return scan(level, cubeAround(center, radius), excludeStructures);
    }

    public static BoundingBox cubeAround(BlockPos center, int radius) {
        return BoundingBox.fromCorners(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius));
    }

    /**
     * Scans every supported container inside {@code area}. Must be called on the server thread.
     *
     * @param excludeStructures skip containers standing inside a generated structure (dungeons, villages,
     *                          mineshafts…), so only the player's own containers are indexed
     */
    public static ScanResult scan(ServerLevel level, BoundingBox area, boolean excludeStructures) {
        return scan(level, area, excludeStructures, ScanOptions.DEFAULT);
    }

    public static ScanResult scan(ServerLevel level, BoundingBox area, boolean excludeStructures,
                                  ScanOptions options) {
        ScanResult result = new ScanResult();

        int minChunkX = area.minX() >> 4;
        int maxChunkX = area.maxX() >> 4;
        int minChunkZ = area.minZ() >> 4;
        int maxChunkZ = area.maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                // Checking structures block by block is the costly part, and most chunks have none at all.
                boolean chunkMayHaveStructures = excludeStructures && hasStructureData(chunk);

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isSupported(blockEntity, options) || !area.isInside(blockEntity.getBlockPos())) {
                        continue;
                    }
                    if (result.containerCount() >= MAX_CONTAINERS) {
                        result.markTruncated();
                        return result;
                    }
                    if (chunkMayHaveStructures && isInsideStructure(level, blockEntity.getBlockPos())) {
                        result.markSkippedStructure();
                        continue;
                    }
                    scanContainer(blockEntity, result, options);
                }
            }
        }
        return result;
    }

    /** Cheap test: a chunk with no structure start or reference cannot hold a piece of one. */
    private static boolean hasStructureData(LevelChunk chunk) {
        return !chunk.getAllStarts().isEmpty() || !chunk.getAllReferences().isEmpty();
    }

    private static boolean isSupported(BlockEntity blockEntity, ScanOptions options) {
        // ChestBlockEntity also covers trapped chests.
        boolean isStorage = blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
        return isStorage || (options.includeOtherContainers() && blockEntity instanceof Container);
    }

    /**
     * True when the position sits inside a piece of a generated structure. A chest a player built next to a
     * village still counts as inside it, but that is the trade-off for catching looted dungeon chests, which
     * are indistinguishable from player chests once their loot table has been rolled.
     */
    private static boolean isInsideStructure(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, structure -> true).isValid();
    }

    private static void scanContainer(BlockEntity blockEntity, ScanResult result, ScanOptions options) {
        // Reading a container with a pending loot table would generate its loot, so leave those untouched.
        if (blockEntity instanceof RandomizableContainerBlockEntity randomizable && randomizable.getLootTable() != null) {
            result.markSkippedLoot();
            return;
        }

        BlockPos pos = canonicalPos(blockEntity);
        result.addContainer(pos);

        Container container = (Container) blockEntity;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                addStack(stack, pos, result, options, 0);
            }
        }
    }

    /**
     * Adds a stack and, when it is a container item such as a shulker box, what it holds inside. Nesting is
     * limited because a shulker box can only ever hold one more level of them.
     */
    private static void addStack(ItemStack stack, BlockPos pos, ScanResult result, ScanOptions options, int depth) {
        result.addItem(stack.getItem(), pos, stack.getCount());
        if (!options.includeNestedContainers() || depth >= MAX_NESTING) {
            return;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.nonEmptyItemCopyStream().forEach(nested -> addStack(nested, pos, result, options, depth + 1));
        }
    }

    /**
     * Each half of a double chest stores its own slots, so both halves get scanned, but their items are
     * reported under a single position so the double chest counts as one container.
     */
    private static BlockPos canonicalPos(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        if (!(blockEntity instanceof ChestBlockEntity)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return pos;
        }
        BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
        return pos.asLong() < other.asLong() ? pos : other;
    }
}
