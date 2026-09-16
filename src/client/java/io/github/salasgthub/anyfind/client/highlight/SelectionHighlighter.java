package io.github.salasgthub.anyfind.client.highlight;

import io.github.salasgthub.anyfind.client.SearchSelection;
import io.github.salasgthub.anyfind.client.config.AnyfindConfig;
import io.github.salasgthub.anyfind.network.ScanResultsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Draws the selected item's containers in the world: a box around each one, a trail of particles running
 * from the player to the closest one, and a column of particles over it.
 */
public final class SelectionHighlighter {

    /** Containers drawn at once; the rest would just be noise. */
    private static final int MAX_BOXES = 12;
    private static final double MAX_DISTANCE = 160.0;
    private static final int MARKER_INTERVAL_TICKS = 4;
    /** Distance between dots on the trail, in blocks. */
    private static final double TRAIL_SPACING = 1.25;
    /** Ticks it takes a dot to travel one gap, i.e. how fast the trail flows. */
    private static final int FLOW_PERIOD_TICKS = 10;
    private static final int MAX_TRAIL_PARTICLES = 48;
    /** Particles live about a second, so drawing the trail every tick would only burn CPU. */
    private static final int TRAIL_INTERVAL_TICKS = 2;
    /** How far the ground search looks above and below the straight line, in blocks. */
    private static final int GROUND_SEARCH_UP = 3;
    private static final int GROUND_SEARCH_DOWN = 6;

    private static final int BOX_STROKE_COLOR = 0xFFFFC74A;
    private static final int BOX_FILL_COLOR = 0x33FFC74A;
    private static final int CLOSEST_STROKE_COLOR = 0xFF6BE675;
    private static final int CLOSEST_FILL_COLOR = 0x3D6BE675;

    private static int tickCounter;

    private SelectionHighlighter() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SelectionHighlighter::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        tickCounter++;
        if (client.level == null || client.player == null || !SearchSelection.isActive()) {
            return;
        }

        BlockPos playerBlock = client.player.blockPosition();
        List<ScanResultsPayload.Location> targets = SearchSelection.locations().stream()
                .filter(location -> location.pos().distSqr(playerBlock) < MAX_DISTANCE * MAX_DISTANCE)
                .sorted(Comparator.comparingDouble(location -> location.pos().distSqr(playerBlock)))
                .limit(MAX_BOXES)
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        // The highlight is cleared by OpenedContainerWatcher once the container is actually opened, so being
        // next to it is not enough to turn it off.
        BlockPos closest = targets.getFirst().pos();
        AnyfindConfig config = AnyfindConfig.get();
        // The trail runs along the ground, so it starts at the player's feet.
        Vec3 from = client.player.position();
        Vec3 to = Vec3.atCenterOf(closest);

        if (config.showBox) {
            // Gizmos are collected per tick and drawn by the level renderer.
            try (Gizmos.TemporaryCollection ignored = client.collectPerTickGizmos()) {
                for (ScanResultsPayload.Location location : targets) {
                    boolean isClosest = location.pos().equals(closest);
                    Gizmos.cuboid(location.pos(), GizmoStyle.strokeAndFill(
                                    isClosest ? CLOSEST_STROKE_COLOR : BOX_STROKE_COLOR, 2.5f,
                                    isClosest ? CLOSEST_FILL_COLOR : BOX_FILL_COLOR))
                            .setAlwaysOnTop();
                }
            }
        }

        if (config.showPath && tickCounter % TRAIL_INTERVAL_TICKS == 0) {
            spawnTrail(client, from, to);
        }
        if (config.showMarker && tickCounter % MARKER_INTERVAL_TICKS == 0) {
            spawnMarker(client, closest);
        }
    }

    /** A column of particles over the chest, visible without looking for the outline. */
    private static void spawnMarker(Minecraft client, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (int step = 0; step < 5; step++) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    center.x(), center.y() + 0.8 + step * 0.3, center.z(),
                    0.0, 0.006, 0.0);
        }
        client.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                center.x(), center.y() + 1.2, center.z(), 0.0, 0.0, 0.0);
    }

    /**
     * Dots laid on the ground between the player and the chest. Each dot is dropped on the surface under the
     * straight horizontal line, and the starting offset advances every tick so the trail reads as flowing.
     * It does not walk around walls: it is a direction to follow, not a route.
     */
    private static void spawnTrail(Minecraft client, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double horizontalDistance = Math.sqrt(delta.x() * delta.x() + delta.z() * delta.z());
        if (horizontalDistance < 1.0) {
            return;
        }
        double stepX = delta.x() / horizontalDistance;
        double stepZ = delta.z() / horizontalDistance;
        double phase = (tickCounter % FLOW_PERIOD_TICKS) / (double) FLOW_PERIOD_TICKS * TRAIL_SPACING;

        int spawned = 0;
        for (double travelled = phase; travelled < horizontalDistance && spawned < MAX_TRAIL_PARTICLES;
                travelled += TRAIL_SPACING, spawned++) {
            double x = from.x() + stepX * travelled;
            double z = from.z() + stepZ * travelled;
            // Height the dot would have in a straight line, used as the starting point of the ground search.
            double expectedY = from.y() + (to.y() - from.y()) * (travelled / horizontalDistance);
            double groundY = groundHeightNear(client, x, expectedY, z);
            client.level.addParticle(ParticleTypes.END_ROD, x, groundY + 0.15, z, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Top of the first surface found around {@code startY}: looks a bit upwards first (stairs going up) and
     * then downwards (drops). Falls back to the straight-line height when there is nothing solid nearby.
     */
    private static double groundHeightNear(Minecraft client, double x, double startY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int origin = Mth.floor(startY);

        for (int offset = GROUND_SEARCH_UP; offset >= -GROUND_SEARCH_DOWN; offset--) {
            int y = origin + offset;
            if (client.level.isOutsideBuildHeight(y) || client.level.isOutsideBuildHeight(y - 1)) {
                continue;
            }
            cursor.set(blockX, y - 1, blockZ);
            boolean standsOnSolid = client.level.getBlockState(cursor).isSolid();
            cursor.set(blockX, y, blockZ);
            boolean isFree = !client.level.getBlockState(cursor).isSolid();
            if (standsOnSolid && isFree) {
                return y;
            }
        }
        return startY;
    }
}
