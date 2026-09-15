package io.github.salasgthub.anyfind.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
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
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isSupported(blockEntity) || !area.isInside(blockEntity.getBlockPos())) {
                        continue;
                    }
                    if (excludeStructures && isInsideStructure(level, blockEntity.getBlockPos())) {
                        result.markSkippedStructure();
                        continue;
                    }
                    scanContainer(blockEntity, result);
                }
            }
        }
        return result;
    }

    private static boolean isSupported(BlockEntity blockEntity) {
        // ChestBlockEntity also covers trapped chests.
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
    }

    /**
     * True when the position sits inside a piece of a generated structure. A chest a player built next to a
     * village still counts as inside it, but that is the trade-off for catching looted dungeon chests, which
     * are indistinguishable from player chests once their loot table has been rolled.
     */
    private static boolean isInsideStructure(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, structure -> true).isValid();
    }

    private static void scanContainer(BlockEntity blockEntity, ScanResult result) {
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
                result.addItem(stack.getItem(), pos, stack.getCount());
            }
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
